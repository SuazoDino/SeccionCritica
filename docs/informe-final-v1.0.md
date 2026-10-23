# Informe técnico final v1.0 — Sección Crítica

**Universidad:** Universidad Nacional de Ingeniería (UNI), FIIS  
**Curso:** Construcción de Software II — SW707  
**Proyecto:** Sección Crítica  
**Integrantes:** Suazo Zanabria Dino (20231324K) y Chavez Oliva Carlos (20232622E)  
**Versión:** v1.0 — examen parcial  
**Fecha de evidencia:** 22 de octubre de 2026  
**Repositorio:** https://github.com/SuazoDino/SeccionCritica

## 1. Resumen ejecutivo

Sección Crítica es un prototipo web local para gestionar matrícula con cupos limitados. Resuelve el riesgo de sobrecupos durante solicitudes concurrentes y aplica reglas académicas sobre prerrequisitos, cursos aprobados, créditos y cruces de prácticas.

La versión v1.0 entrega una aplicación Spring Boot con interfaz Thymeleaf, persistencia JDBC sobre H2 y PostgreSQL, transacciones con bloqueo pesimista, manejo centralizado de errores y una frontera de verificación académica simulada con timeout, reintentos y circuit breaker. El resultado comprobado fue una ejecución limpia de 37 pruebas, sin fallos, errores ni omitidas, con PostgreSQL 17 mediante Testcontainers.

El sistema permite seleccionar uno de los estudiantes de demostración, consultar la oferta, matricularse, retirarse y visualizar la agenda. La garantía de última vacante se implementa en servidor y base de datos; la pantalla solo muestra una vista informativa del estado.

## 2. Información general y alcance real

La aplicación se ejecuta desde [`Prototipo/`](../Prototipo/) con Java 21 y Maven Wrapper. El punto de entrada es [`Application`](../Prototipo/src/main/java/pe/uni/seccioncritica/Application.java). La interfaz expone las rutas MVC `/`, `/oferta`, `/matriculas`, `/matricular` y `/retirar` en [`MatriculaController`](../Prototipo/src/main/java/pe/uni/seccioncritica/controller/MatriculaController.java).

El alcance comprobado de v1.0 incluye:

- consulta de estudiantes, oferta, secciones, sesiones, cupos y agenda;
- matrícula y retiro con transacciones;
- validación de curso aprobado, matrícula duplicada, cupo, prerrequisito, créditos y cruce práctica–práctica;
- manejo amigable de entradas inválidas, recursos inexistentes, rechazo de reglas y errores de infraestructura;
- verificación académica simulada con mecanismos de resiliencia;
- pruebas de páginas, reglas y concurrencia en H2 y PostgreSQL 17.

## 3. Arquitectura y tecnologías

### 3.1 Arquitectura implementada

El sistema es un monolito web modular con cuatro límites principales:

```text
HTTP / Thymeleaf
        │
        ▼
controller: MatriculaController, ManejadorErrores
        │
        ▼
service: MatriculaService, MatriculaCommandService,
         VerificacionAcademicaService
        │
        ├── repository: JdbcClient y SQL explícito
        │       └── H2 o PostgreSQL
        └── model / exception
```

[`MatriculaService`](../Prototipo/src/main/java/pe/uni/seccioncritica/service/MatriculaService.java) ofrece consultas y coordina la verificación académica con los comandos. [`MatriculaCommandService`](../Prototipo/src/main/java/pe/uni/seccioncritica/service/MatriculaCommandService.java) concentra las escrituras transaccionales. [`EstudianteRepository`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/EstudianteRepository.java), [`SeccionRepository`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/SeccionRepository.java), [`CursoRepository`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/CursoRepository.java) y [`MatriculaRepository`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/MatriculaRepository.java) encapsulan consultas JDBC. Las reglas estructurales de datos están en [`schema.sql`](../Prototipo/src/main/resources/schema.sql) y los datos de demo en [`data.sql`](../Prototipo/src/main/resources/data.sql).

### 3.2 Tecnologías y motivo de uso

| Tecnología | Uso en v1.0 | Interacción comprobada |
|---|---|---|
| Java 21 | Lenguaje y virtual threads para la frontera externa | Ejecuta aplicación y pruebas |
| Spring Boot 4.1.0 | Inyección, MVC, transacciones, configuración y Actuator | Compone controladores, servicios, JDBC y health check |
| Maven Wrapper | Construcción reproducible local | `./mvnw` o `./mvnw.cmd` |
| Spring JDBC / `JdbcClient` | SQL explícito y acceso a datos | Ejecuta `SELECT FOR UPDATE`, consultas y actualizaciones |
| H2 | Demo predeterminada y pruebas | Base en memoria |
| PostgreSQL 17 | Perfil y prueba de integración | Contenedor real de Testcontainers |
| Docker / Testcontainers | Entorno PostgreSQL reproducible para pruebas | `MatriculaServicePostgresTest` |
| JUnit 5 y Spring Test | Pruebas MVC, servicio e integración | 37 pruebas comprobadas |
| Resilience4j 2.2.0 | Retry y circuit breaker | Protege la verificación académica simulada |
| Thymeleaf y CSS/JavaScript propio | Interfaz de demostración | Renderiza oferta, errores, agenda y formularios |

## 4. Construcción orientada a la verificación

### 4.1 Dependencias y reúso

Las dependencias directas están declaradas en [`Prototipo/pom.xml`](../Prototipo/pom.xml). Spring Boot administra las versiones de Spring, logging, Jackson y testing; H2 y PostgreSQL son runtime; Testcontainers es exclusivo de pruebas; Resilience4j es runtime para la frontera externa. El árbol real se generó con `dependency:tree` y se resume en [análisis-dependencias.md](../Prototipo/docs/analisis-dependencias.md).

El reúso comprobable es el de componentes de terceros y de una base común de reglas en [`ReglasMatriculaTestBase`](../Prototipo/src/test/java/pe/uni/seccioncritica/service/ReglasMatriculaTestBase.java), compartida por H2 y PostgreSQL. No se implementó la interfaz `ReglaMatricula` ni perfiles institucionales intercambiables descritos en el documento parcial.

### 4.2 Problemas de runtime

El problema operativo principal es la carrera por una vacante: dos transacciones pueden observar el mismo cupo si no se serializa la fila. El problema se mitiga con la transacción de [`MatriculaCommandService.matricular`](../Prototipo/src/main/java/pe/uni/seccioncritica/service/MatriculaCommandService.java) y los bloqueos de [`EstudianteRepository.bloquear`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/EstudianteRepository.java) y [`SeccionRepository.bloquear`](../Prototipo/src/main/java/pe/uni/seccioncritica/repository/SeccionRepository.java).

También existe contención esperable: PostgreSQL espera hasta 5 segundos por bloqueo según [`application-postgres.properties`](../Prototipo/src/main/resources/application-postgres.properties), mientras el comando transaccional tiene timeout de 10 segundos. El controlador traduce una contención transitoria a un mensaje controlado. No se midió capacidad sostenida ni percentiles de latencia.

### 4.3 Desarrollo por contratos

Los contratos observables y su evidencia se detallan en [contratos-matricula-y-concurrencia.md](../Prototipo/docs/contratos-matricula-y-concurrencia.md). Las rutinas principales son `matricular`, `retirar` y `oferta`.

### 4.4 Programación defensiva

El servidor no confía en el cupo mostrado en pantalla: vuelve a consultar y bloquear la sección. IDs inexistentes se convierten en `RecursoNoEncontradoException`; parámetros ausentes o no numéricos se manejan mediante [`ManejadorErrores`](../Prototipo/src/main/java/pe/uni/seccioncritica/controller/ManejadorErrores.java). El cliente desactiva el botón durante el envío, pero la unicidad y la consistencia final dependen del servidor y de la base.

### 4.5 Excepciones y logging

La jerarquía bajo `SeccionCriticaException` distingue cupo, prerrequisito, créditos, horario, recurso inexistente y servicio externo. [`MatriculaController`](../Prototipo/src/main/java/pe/uni/seccioncritica/controller/MatriculaController.java) conserva los rechazos de negocio en el panel; [`ManejadorErrores`](../Prototipo/src/main/java/pe/uni/seccioncritica/controller/ManejadorErrores.java) centraliza respuestas 404, 400, 503 y 500 sin exponer trazas al usuario. Los servicios registran confirmaciones en INFO, rechazos y fallas transitorias en WARN y fallas técnicas inesperadas en ERROR.

### 4.6 Concurrencia

El orden de adquisición es estudiante y luego sección. Esto serializa operaciones del mismo estudiante y el consumo de una sección, y evita que dos rutas del sistema adquieran esos bloqueos en orden inverso. La base además mantiene `cupo_disponible BETWEEN 0 AND cupo_maximo`, claves foráneas y clave primaria de matrícula.

### 4.7 Resiliencia

[`VerificacionAcademicaService`](../Prototipo/src/main/java/pe/uni/seccioncritica/service/VerificacionAcademicaService.java) usa timeout configurable, hasta tres intentos con espera de 50 ms y circuit breaker con ventana de 4 llamadas, mínimo de 4, umbral de fallo de 50% y espera de apertura de 5 s. El rechazo se transforma en `ServicioExternoNoDisponibleException` y el controlador responde 503. La transacción de matrícula comienza después de la verificación, por lo que una falla externa no consume cupos.

El rollback es transaccional: matrícula y ajuste del cupo se revierten juntos si una escritura posterior falla. La prueba específica de rollback está en `ReglasMatriculaTestBase.retiroHaceRollbackSiFallaLaLiberacionDelCupo`. Las pruebas automatizadas verifican reintento, timeout y apertura del circuit breaker (`circuitBreakerAbiertoDejaDeLlamarAlServicioCaido`: tras 4 fallos el circuito se abre y la siguiente solicitud se rechaza sin llamar al servicio). `ServicioExternoCaidoTest` comprueba de punta a punta que con el verificador caído se responde 503 sin consumir cupo.

## 5. Control de concurrencia

### 5.1 Última vacante y condición de carrera

La sección 4 de [`data.sql`](../Prototipo/src/main/resources/data.sql) tiene una vacante. Sin bloqueo, cinco transacciones podrían leer `cupo_disponible = 1` antes de actualizarlo. El flujo implementado bloquea primero la fila del estudiante y después la fila de la sección con `SELECT * FROM seccion WHERE id = ? FOR UPDATE`. La segunda transacción espera; al continuar, observa el cupo actualizado y rechaza la matrícula si la sección está agotada.

### 5.2 Escenario observado

`ultimoCupoTieneUnSoloGanador` coordina cinco competidores con los pares `{1,4}`, `{2,4}`, `{3,4}`, `{4,4}` y `{5,4}`. La prueba comprueba un ganador, cuatro rechazos, cero vacantes, una matrícula y una unidad consumida. El mismo conjunto de reglas se ejecuta con H2 y PostgreSQL 17 mediante Testcontainers en la evidencia registrada.

La prueba demuestra integridad de la operación coordinada; no es una prueba de carga sostenida ni una medición de capacidad.

## 6. Contratos y reglas de negocio

| Rutina | Precondiciones | Postcondiciones | Invariantes / fuente |
|---|---|---|---|
| `MatriculaCommandService.matricular` | Estudiante, sección y curso existen; la operación llega con IDs válidos | En éxito existe una matrícula y el cupo disminuye en uno; en rechazo no se confirma la operación | `cupo_disponible` permanece entre 0 y máximo; reglas y `CHECK` de [`schema.sql`](../Prototipo/src/main/resources/schema.sql) |
| `MatriculaCommandService.retirar` | Estudiante, sección y matrícula activa existen | En éxito se elimina la matrícula y se libera exactamente un cupo; retiro repetido no libera otro | No superar `cupo_maximo`; rollback mantiene ambos cambios atómicos |
| `MatriculaService.oferta` | El estudiante existe y los catálogos son consultables | Devuelve solo cursos habilitados y sus secciones/sesiones | Se ocultan cursos aprobados y prerrequisitos pendientes |

El contrato del código está compuesto por consultas, excepciones de dominio y transacciones. Las restricciones de base de datos son una segunda línea: claves primarias/foráneas, unicidad, `CHECK` de cupos, créditos, días y horarios. No deben confundirse con validaciones de servicio: por ejemplo, el prerrequisito y el cruce práctica–práctica se deciden en Java, mientras que el rango de cupo se impone en SQL.

## 7. Resultados de pruebas

La evidencia vigente corresponde al comando `mvnw clean verify` desde `Prototipo/`, ejecutado el 23 de octubre de 2026 con Docker activo.

| Suite | Resultado |
|---|---:|
| `PaginasTest` | 7 |
| `ServicioExternoCaidoTest` | 1 |
| `SeccionTest` | 2 |
| `MatriculaServiceH2Test` | 12 |
| `MatriculaServicePostgresTest` | 12 |
| `VerificacionAcademicaServiceTest` | 3 |
| Total | **37** |

Fallos: **0** · Errores: **0** · Omitidas: **0** · Resultado: **BUILD SUCCESS** · Duración: **10.5 s**. PostgreSQL 17 se ejecutó mediante Testcontainers. Los reportes Surefire existentes están en `Prototipo/target/surefire-reports/`; `target/` no forma parte del entregable.

No se generó un porcentaje de cobertura ni una prueba de carga. Por ello no se atribuyen métricas de cobertura o rendimiento.

## 8. Análisis de dependencias

El árbol se generó con:

```powershell
Set-Location C:\SeccionCritica\Prototipo
.\mvnw.cmd dependency:tree "-DoutputFile=target/dependency-tree.txt"
```

Finalizó con `BUILD SUCCESS`. Las directas son los starters MVC, Thymeleaf, JDBC y Actuator; Resilience4j circuit breaker/retry; H2 y PostgreSQL; y las dependencias de pruebas Spring Test, Spring Testcontainers y módulos Testcontainers JUnit/PostgreSQL. Entre las transitivas observadas están Tomcat, Spring Framework, HikariCP, Thymeleaf, Logback/SLF4J, Jackson 3, Micrometer, JUnit 6, Mockito, AssertJ, Awaitility, Docker Java y PostgreSQL/Testcontainers.

Riesgos a gestionar: la superficie transitiva de web, logging, Jackson, JDBC y Docker Java debe mantenerse actualizada; la auditoría debe repetirse cuando NVD pueda completar la actualización; y la revisión legal de licencias no se considera cerrada solo por leer el árbol. No se forzaron versiones adicionales para resolver riesgos no confirmados.

## 9. Auditoría OWASP

Se ejecutó OWASP Dependency-Check **12.1.0** bajo demanda. La actualización de NVD informó **413,340 registros**, descargó **10,000** (aproximadamente 2%), mostró reintentos de solicitud y no utilizó API key. La actualización quedó inconclusa; por tanto, no existe un inventario concluyente de CVE y no se afirma que haya cero vulnerabilidades.

Como verificación complementaria se activaron las alertas de Dependabot en GitHub: **0 alertas** sobre las dependencias declaradas en `pom.xml` (evidencia 10). Esto cubre las directas, no el árbol transitivo completo.

La auditoría OWASP queda pendiente. Debe repetirse con una API key o con tiempo suficiente para completar NVD y conservar los informes HTML/JSON resultantes en `target/` sin agregarlos al repositorio.

## 10. Limitaciones y trabajo futuro

Limitaciones verificables de v1.0:

- la selección de estudiante es una demostración local, no autenticación ni autorización;
- no hay protección CSRF, lista de espera, historial de retiros ni estados de matrícula;
- la verificación académica es un stub configurable, no una integración externa real;
- no hay migraciones versionadas; el esquema se inicializa con SQL;
- no se midieron carga sostenida, percentiles ni cobertura;
- la auditoría OWASP quedó pendiente por la actualización incompleta de NVD;

El trabajo futuro debe priorizar autenticación/CSRF, migraciones, integración real, métricas y pruebas de carga, lista de espera e historial, y completar la auditoría de dependencias. Estas propuestas no forman parte de v1.0.

## 11. Matriz de cumplimiento de la rúbrica

| Criterio | Peso | Implementación existente | Archivos o métodos | Prueba / evidencia | Estado | Observaciones |
|---|---:|---|---|---|---|---|
| Dependencias y reúso | 10% | Maven, starters, JDBC, Resilience4j, Testcontainers; base de pruebas común | [`pom.xml`](../Prototipo/pom.xml), `ReglasMatriculaTestBase` | árbol Maven con `BUILD SUCCESS` | Cumplido | Auditoría OWASP inconclusa; el análisis de vulnerabilidades queda pendiente |
| Problemas de runtime | 10% | Bloqueos, timeouts y control de contención | `MatriculaCommandService`, repositorios, propiedades PostgreSQL | `ultimoCupoTieneUnSoloGanador`; pruebas de timeout | Cumplido | No es prueba de carga |
| Desarrollo por contratos | 15% | Pre/postcondiciones e invariantes en código y en SQL | `matricular`, `retirar`, `oferta`, record `Seccion`, `ajustarCupo`, `schema.sql` | reglas, rollback, IDs no positivos y `SeccionTest` | Cumplido | Contratos en Javadoc y validaciones explícitas; no hay DSL formal de contratos |
| Programación defensiva | 10% | Validación en servidor, IDs inexistentes y errores de entrada | `ManejadorErrores`, `MatriculaController` | `PaginasTest`, 7 pruebas | Cumplido | Sin autenticación/CSRF por ser demo |
| Excepciones y logging | 15% | Jerarquía de dominio, advice y logging por evento | `exception/*`, `ManejadorErrores`, servicios | `PaginasTest` y pruebas de servicio | Cumplido | No se presentó una métrica de observabilidad |
| Concurrencia | 15% | `@Transactional`, `SELECT FOR UPDATE`, orden estudiante→sección | `MatriculaCommandService`, repositorios | cinco competidores en H2/PostgreSQL | Cumplido | Verificación focalizada, no carga sostenida |
| Resiliencia | 15% | Timeout, retry, circuit breaker y rollback | `VerificacionAcademicaService`, comando transaccional | 3 pruebas de verificación, `ServicioExternoCaidoTest` (503) y prueba de rollback | Cumplido | Los cuatro mecanismos tienen prueba automatizada |
| Pruebas y evidencias | 5% | Suite MVC, H2, PostgreSQL/Testcontainers y resiliencia | seis suites | 37, 0 fallos, 0 errores, 0 omitidas | Cumplido | Capturas en `docs/evidencias/` |
| Calidad y documentación | 5% | Informe, guías, contratos y análisis de dependencias | `docs/`, `Prototipo/docs/`, READMEs | revisión documental y enlaces relativos | Cumplido | Licencia Apache 2.0 en `LICENSE`; OWASP pendiente |

## 12. Conclusiones

La versión v1.0 entrega un núcleo de matrícula verificable para el escenario de última vacante. La combinación de transacciones, orden fijo de bloqueos, `SELECT FOR UPDATE` y restricciones SQL evita el sobrecupo en el escenario probado. La misma lógica de reglas se validó sobre H2 y PostgreSQL 17, y la interfaz maneja fallos funcionales y técnicos sin exponer trazas.

La evidencia es sólida para los comportamientos comprobados y acotada para carga, seguridad de acceso, cobertura y auditoría de dependencias. La documentación final distingue esos límites para que la defensa académica no atribuya al sistema capacidades que todavía no se verificaron.

