# Documento técnico parcial — Sección Crítica

**Curso:** Construcción de Software II – SW707 | Universidad Nacional de Ingeniería
**Entregable:** v1.0 (Examen Parcial)
**Integrantes:** Suazo Zanabria Dino, Chavez Oliva Carlos
**Docente:** Anibal Guillen
**Fecha:** 2026-10-23
**Repositorio:** <https://github.com/SuazoDino/SeccionCritica>

Este documento describe **solo lo implementado y verificado en v1.0**. Las propuestas para v2.0 se listan aparte en la sección 11. El informe consolidado con la matriz de rúbrica está en [`docs/informe-final-v1.0.md`](docs/informe-final-v1.0.md).

---

## 1. Descripción del problema y usuarios

### 1.1 Problema

Durante la apertura de la matrícula universitaria, muchos estudiantes intentan inscribirse al mismo tiempo en secciones con cupos limitados. Sin control de concurrencia se producen:

- **Sobrecupo**: más matriculados que vacantes en una sección.
- **Reglas académicas ignoradas**: prerrequisitos no aprobados o exceso de créditos.
- **Cruces de horario** no detectados entre prácticas.
- **Degradación o caída** cuando un servicio externo falla justo en la hora de mayor demanda.

### 1.2 Usuarios y actores en v1.0

| Actor | Rol en el prototipo |
|---|---|
| **Estudiante** | Elige su perfil (demo, sin autenticación), consulta la oferta habilitada, se matricula y se retira. |
| **Servicio externo de verificación académica** (simulado) | Confirma que el estudiante puede matricularse antes de abrir la transacción. Puede configurarse para fallar o tardar. |

### 1.3 Objetivo

Construir un núcleo de matrícula que garantice que nunca se excede el cupo de una sección, que respete prerrequisitos, créditos y cruces de práctica, y que se degrade de forma controlada si el servicio externo falla.

---

## 2. Alcance y requisitos

### 2.1 Alcance implementado en v1.0

- Flujo completo por interfaz web: seleccionar estudiante → ver oferta habilitada → matricularse → ver agenda y créditos → retirarse.
- Validaciones de matrícula: curso ya aprobado, matrícula duplicada en el mismo curso, cupo disponible, prerrequisito aprobado, máximo de créditos y cruces práctica–práctica.
- Retiro con liberación inmediata del cupo.
- Verificación académica externa (simulada) protegida con timeout, reintentos y circuit breaker.
- Ejecución con H2 en memoria (demo) o con PostgreSQL 17 (perfil `postgres`, Docker Compose).

### 2.2 Fuera de alcance en v1.0

API REST/OpenAPI, autenticación y CSRF, reglas intercambiables por institución, lista de espera, historial de retiros, migraciones versionadas e integración real con un servicio externo.

### 2.3 Requisitos no funcionales verificados

| Requisito | Criterio | Evidencia |
|---|---|---|
| Consistencia de cupos | Con N solicitudes simultáneas por la última vacante gana exactamente una. | `ultimoCupoTieneUnSoloGanador` en H2 y PostgreSQL |
| Degradación controlada | Si la verificación externa falla o tarda, se responde 503 y no se consume cupo. | `VerificacionAcademicaServiceTest`, `ServicioExternoCaidoTest` |
| Sin trazas al usuario | Cualquier error termina en `error.html` con un mensaje claro. | `ManejadorErrores`, `PaginasTest` |

No se midió rendimiento sostenido (latencia p95/p99 ni throughput).

---

## 3. Arquitectura y decisiones técnicas

### 3.1 Vista general

```
Navegador (Thymeleaf + matricula.js)
        │  GET /oferta, /matriculas · POST /matricular, /retirar
        ▼
controller/  MatriculaController · ManejadorErrores (@ControllerAdvice)
        │
        ▼
service/     MatriculaService (fachada: precondiciones, logging)
        ├──► VerificacionAcademicaService ──► VerificadorAcademico (stub demo)
        │       timeout · retry · circuit breaker (Resilience4j)
        └──► MatriculaCommandService (@Transactional, SELECT … FOR UPDATE)
        │
        ▼
repository/  JdbcClient: Estudiante · Curso · Seccion · Matricula
        │
        ▼
H2 (demo) / PostgreSQL 17 — schema.sql con CHECK, PK y FK
```

### 3.2 Decisiones clave

| Decisión | Alternativa | Justificación |
|---|---|---|
| Spring Boot 4.1 + Java 21 | Node.js | Tipado estático, transacciones declarativas, hilos virtuales. |
| JDBC (`JdbcClient`) con SQL explícito | JPA/Hibernate | El `SELECT … FOR UPDATE` y el orden de bloqueos quedan visibles y auditables. |
| Bloqueo pesimista por fila | Bloqueo optimista con versión | Bajo alta contención sobre la misma sección evita reintentos en cascada (ver §8.3). |
| Verificación externa **antes** de la transacción | Dentro de la transacción | Una falla o latencia externa no retiene bloqueos ni consume cupos. |
| Resilience4j (circuit breaker + retry) | Reintentos manuales | Librería madura; configuración explícita y probada. |

### 3.3 Modelo de datos

Tablas en [`schema.sql`](Prototipo/src/main/resources/schema.sql): `estudiante` (`max_creditos > 0`), `curso` (`creditos > 0`, prerrequisito opcional), `aprobado`, `seccion` (`CHECK (cupo_disponible BETWEEN 0 AND cupo_maximo)`), `matricula` (PK compuesta estudiante–sección) y `sesion` (teoría/práctica, día 1–7, de 07:00 a 22:00, `fin > inicio`).

---

## 4. Dependencias y reúso

### 4.1 Construcción con reúso

| Dependencia directa | Propósito | Alcance |
|---|---|---|
| `spring-boot-starter-webmvc`, `-thymeleaf` | Controladores y vistas | compile |
| `spring-boot-starter-jdbc` | `JdbcClient`, transacciones, HikariCP | compile |
| `spring-boot-starter-actuator` | `/actuator/health` | compile |
| `resilience4j-circuitbreaker` / `-retry` 2.2.0 | Resiliencia del borde externo | compile |
| `h2`, `postgresql` | Bases de datos | runtime |
| `spring-boot-starter-test`, `-webmvc-test`, Testcontainers | Pruebas | test |

Las versiones de Spring, logging, Jackson y testing las fija el parent `spring-boot-starter-parent:4.1.0`, lo que evita divergencias entre transitivas. Resilience4j y el plugin OWASP fijan su versión explícitamente.

### 4.2 Construcción para el reúso

- `VerificadorAcademico` es una interfaz: el stub de demostración puede sustituirse por un cliente real sin tocar `VerificacionAcademicaService`.
- `ReglasMatriculaTestBase` concentra las 12 pruebas de reglas y concurrencia; `MatriculaServiceH2Test` y `MatriculaServicePostgresTest` la reutilizan cambiando solo la base de datos.

### 4.3 Dependencias transitivas y riesgos

El árbol real (`mvnw dependency:tree`) incluye Spring Framework 7.0.8, Tomcat 11.0.22, HikariCP 7.0.2, Thymeleaf 3.1.5, Logback 1.5.34, Jackson 3.1.4, Micrometer 1.17.0, H2 2.4.240, PostgreSQL JDBC 42.7.11, y en pruebas JUnit 6.0.3, Mockito 5.23.0, Testcontainers 2.0.5 y Docker Java 3.7.1. El detalle está en [`Prototipo/docs/analisis-dependencias.md`](Prototipo/docs/analisis-dependencias.md).

| Riesgo | Mitigación |
|---|---|
| Vulnerabilidades en la superficie web (Tomcat, Jackson, Logback) | Versiones gestionadas por el parent de Spring Boot; actualizar el parent ante avisos de seguridad. |
| Driver JDBC y H2 | H2 solo se usa en demo y pruebas; PostgreSQL JDBC se actualiza con el parent. |
| Docker Java / Testcontainers | Solo en alcance `test`; no se empaqueta en el JAR. |
| CVEs desconocidas | OWASP Dependency-Check 12.1.0 configurado (umbral CVSS 7.0). La primera ejecución quedó **inconclusa** porque la descarga de la base NVD no terminó sin API key. No se afirma ausencia de vulnerabilidades. |

### 4.4 Licencias

El proyecto se publica bajo Apache 2.0 ([`LICENSE`](LICENSE)). Las dependencias directas usan Apache 2.0, EPL/MPL (H2), BSD-2 (PostgreSQL JDBC) o MIT (Testcontainers), todas compatibles; no hay dependencias GPL.

---

## 5. Problemas de runtime

### 5.1 Condición de carrera por la última vacante

**Problema.** Dos transacciones leen `cupo_disponible = 1` a la vez, ambas deciden que hay espacio y ambas insertan la matrícula: sobrecupo. Lo mismo ocurre con un estudiante que envía dos solicitudes simultáneas: cada una ve los créditos y la agenda anteriores y juntas superan el máximo o se cruzan.

**Análisis.** Una lectura previa a la escritura, sin bloqueo, no sirve: la ventana de carrera sigue abierta entre la lectura y el `UPDATE`.

**Solución.** [`MatriculaCommandService`](Prototipo/src/main/java/pe/uni/seccioncritica/service/MatriculaCommandService.java) abre una transacción y bloquea con `SELECT … FOR UPDATE`, **siempre en el orden estudiante → sección**. El bloqueo de la sección serializa el consumo de cupo, el del estudiante serializa sus créditos y horarios, y el orden fijo evita interbloqueos. El `CHECK` de la tabla `seccion` actúa como última barrera.

**Evidencia.** `ultimoCupoTieneUnSoloGanador`, `solicitudesDelMismoEstudianteRespetanElHorario` y `solicitudesDelMismoEstudianteRespetanLosCreditos`, en H2 y PostgreSQL.

### 5.2 Contención de bloqueos y esperas indefinidas

**Problema.** Con bloqueo pesimista, en la hora pico las transacciones forman una cola por la misma fila. Si una se demora, el resto espera; sin límites, los hilos de Tomcat y las conexiones de HikariCP se agotan y el sistema completo deja de responder. El mismo efecto ocurre si el servicio externo tarda y la espera ocurre dentro de la transacción.

**Solución.** Se acota cada espera en tiempo de ejecución:

| Límite | Valor | Dónde |
|---|---|---|
| Espera por un bloqueo de fila (PostgreSQL) | 5 s | `SET lock_timeout` en [`application-postgres.properties`](Prototipo/src/main/resources/application-postgres.properties) |
| Duración de la transacción de matrícula | 10 s | `@Transactional(timeout = 10)` |
| Obtener una conexión del pool | 5 s (pool de 20) | `spring.datasource.hikari.*` |
| Llamada al servicio externo | 2 s, fuera de la transacción | `resiliencia.verificacion.timeout` |

Al vencer un límite, `MatriculaController` captura la `TransientDataAccessException` y devuelve al panel el mensaje "Hay muchas solicitudes en este momento y no se modificó nada"; la transacción se revierte. Como la verificación externa ocurre antes de abrir la transacción, su latencia nunca retiene bloqueos.

**Límite del análisis.** No se hizo prueba de carga sostenida; los valores son conservadores y deben calibrarse con medición en v2.0.

---

## 6. Contratos y programación defensiva

### 6.1 Diseño por contrato

Los contratos están escritos en el código (Javadoc `Pre`/`Post`/invariantes) y se hacen cumplir con excepciones de dominio, no con `assert` (que puede desactivarse en producción). El detalle está en [`Prototipo/docs/contratos-matricula-y-concurrencia.md`](Prototipo/docs/contratos-matricula-y-concurrencia.md).

| Componente | Precondición | Postcondición | Invariante |
|---|---|---|---|
| `MatriculaService.matricular` / `MatriculaCommandService.matricular` | IDs positivos (si no, `RecursoNoEncontradoException` sin consultar al servicio externo); estudiante y sección existen; verificación externa aprobada. | Éxito: una única matrícula estudiante–sección y el cupo baja en 1. Rechazo: excepción de dominio y rollback; nada cambia. | `0 ≤ cupo_disponible ≤ cupo_maximo`; créditos ≤ `max_creditos`; sin cruces práctica–práctica. |
| `MatriculaService.retirar` / `MatriculaCommandService.retirar` | IDs positivos; existe una matrícula activa para el par. | Éxito: la matrícula desaparece y el cupo sube exactamente en 1. Retiro repetido: se rechaza sin liberar otro cupo. | El cupo liberado nunca supera `cupo_maximo` (CHECK + rollback). |
| `Seccion` (record) | — | — | El constructor rechaza `cupoMaximo ≤ 0` o `cupoDisponible` fuera de `[0, cupoMaximo]` con `IllegalStateException`. |
| `SeccionRepository.ajustarCupo` | La sección está bloqueada por la transacción actual. | Se modificó exactamente una fila; si no, se lanza excepción y la transacción se revierte. | — |
| `MatriculaService.oferta` | El estudiante existe. | Solo cursos no aprobados con prerrequisito cumplido; sin efectos laterales. | — |

**Pruebas de contrato:** `contratosRechazadosNoCambianMatriculasNiCupos`, `retiroHaceRollbackSiFallaLaLiberacionDelCupo`, `inexistentesNoAlteranLosCupos`, `ofertaFiltraPrerrequisitosYAprobados` y `SeccionTest`.

### 6.2 Programación defensiva

- **Entradas no confiables.** Los parámetros `estudiante` y `seccion` llegan del formulario. Un valor ausente o no numérico se convierte en 400 con un mensaje claro (`MethodArgumentTypeMismatchException` en `ManejadorErrores`); un ID no positivo o inexistente, en 404.
- **No confiar en el cliente.** El cupo que muestra la pantalla es informativo: el servidor vuelve a leer y bloquear la sección dentro de la transacción. El botón se desactiva al enviar (`matricula.js`), pero la unicidad la garantiza la PK de `matricula`.
- **Defensa en profundidad en la base.** `CHECK` de cupos, créditos y horarios, PK compuesta y FK. Aunque falle el código, la base rechaza el estado inválido.
- **SQL parametrizado.** Todas las consultas usan `JdbcClient` con parámetros; no se concatena entrada del usuario.
- **Sin filtración de información.** `server.error.include-message=never` e `include-stacktrace=never`.

---

## 7. Estrategia de errores, excepciones y logging

### 7.1 Jerarquía de excepciones

```
RuntimeException
└── SeccionCriticaException (abstracta)
    ├── MatriculaRechazadaException          → vuelve al panel con el motivo (regla de negocio)
    │   ├── CupoNoDisponibleException
    │   ├── PrerrequisitoNoCumplidoException
    │   ├── CreditosExcedidosException
    │   └── CruceDeHorarioException
    ├── RecursoNoEncontradoException         → 404, error.html
    └── ServicioExternoNoDisponibleException → 503, error.html
```

### 7.2 Política

| Tipo de falla | Tratamiento | Respuesta al usuario | Log |
|---|---|---|---|
| Regla de negocio | Rollback y redirección al panel | Mensaje específico (p. ej. "No quedan vacantes") | `WARN evento=matricula_rechazada … regla=… motivo=…` |
| Contención transitoria de la base | Rollback | "Intenta nuevamente en unos segundos" | `WARN Contención al …` |
| Recurso inexistente / parámetro inválido | `ManejadorErrores` | 404 / 400 en `error.html` | `WARN evento=recurso_no_encontrado` |
| Servicio externo caído, lento o circuito abierto | Rechazo antes de la transacción | 503 en `error.html` | `WARN evento=verificacion_academica_fallida causa=timeout\|servicio_no_disponible\|circuit_breaker_abierto` |
| Falla de base de datos | `ManejadorErrores` | 503 genérico | `ERROR` con stack trace |
| Error inesperado | `ManejadorErrores` | 500 genérico, sin trazas | `ERROR` con stack trace |

Los éxitos se registran en `INFO` (`evento=matricula_confirmada`, `evento=retiro_confirmado`). El formato `clave=valor` permite filtrar eventos sin reproducir el caso.

---

## 8. Concurrencia

### 8.1 Escenario

Cinco estudiantes solicitan **al mismo tiempo** la última vacante de la sección 4. Además, un mismo estudiante envía solicitudes simultáneas que, juntas, superarían sus créditos o cruzarían horarios.

### 8.2 Estrategias comparadas

**A — Pesimista (`SELECT … FOR UPDATE`), implementada:**

```java
@Transactional(timeout = 10)
void matricular(long estudianteId, long seccionId) {
    var estudiante = estudiantes.bloquear(estudianteId)...;   // FOR UPDATE, primero
    var seccion = secciones.bloquear(seccionId)...;           // FOR UPDATE, segundo
    ... validaciones ...
    if (seccion.agotada()) throw new CupoNoDisponibleException();
    matriculas.registrar(estudianteId, seccionId);
    secciones.ajustarCupo(seccionId, -1);
}
```

**B — Optimista (columna `version`):** se lee sin bloquear y el `UPDATE` se condiciona a que la versión no haya cambiado; si cambió, se reintenta.

### 8.3 Comparación y decisión

| Criterio | Pesimista | Optimista |
|---|---|---|
| Alta contención sobre una misma fila (apertura) | Cola ordenada, sin trabajo desperdiciado | Muchos conflictos y reintentos |
| Complejidad | Baja | Media (reintentos explícitos) |
| Interbloqueo | Posible, evitado con orden fijo estudiante → sección | No aplica |

Se eligió el bloqueo pesimista porque la contención es justamente el escenario del dominio. La espera se acota con los límites de §5.2.

### 8.4 Prueba reproducible

`ReglasMatriculaTestBase.ultimoCupoTieneUnSoloGanador` usa un `ExecutorService` de 5 hilos y dos `CountDownLatch`: uno espera a que todos los hilos estén listos y el otro los libera a la vez. Cada hilo llama a `service.matricular` en su propia transacción. Verifica que:

1. Exactamente 1 solicitud se confirma y 4 reciben `MatriculaRechazadaException`.
2. `cupo_disponible = 0` y existe 1 sola matrícula en la sección.
3. `cupo_maximo − cupo_disponible` es igual al número de matrículas.

Se ejecuta contra H2 y contra PostgreSQL 17 (Testcontainers):

```
cd Prototipo
./mvnw test -Dtest='MatriculaService*Test'
```

---

## 9. Tolerancia a fallas

### 9.1 Dependencia externa simulada

`VerificadorAcademicoDemo` simula el servicio de verificación académica. Se configura con `VERIFICACION_ACADEMICA_MODO=ok|fallo` y `VERIFICACION_ACADEMICA_LATENCIA=<duración>`.

### 9.2 Mecanismos

| Mecanismo | Configuración | Propósito | Prueba |
|---|---|---|---|
| **Timeout** | 2 s (configurable) | La solicitud no queda colgada si el servicio tarda. | `timeoutSeConvierteEnRechazoControlado` |
| **Reintentos** | 3 intentos, 50 ms entre ellos | Absorbe fallas transitorias sin involucrar al usuario. | `reintentaFallasTransitoriasYLuegoConfirma` |
| **Circuit breaker** | Ventana 4, mínimo 4 llamadas, umbral 50 %, abierto 5 s | Ante fallas repetidas deja de llamar al servicio y rechaza de inmediato. | `circuitBreakerAbiertoDejaDeLlamarAlServicioCaido` |
| **Fallback controlado** | `ServicioExternoNoDisponibleException` → 503 | No se confirma una matrícula sin verificar ni se muestra una traza. | `ServicioExternoCaidoTest` |
| **Rollback transaccional** | `@Transactional` | Si falla una escritura posterior, matrícula y cupo se revierten juntos. | `retiroHaceRollbackSiFallaLaLiberacionDelCupo` |

### 9.3 Resultado

Ante una falla del servicio externo el sistema no se cae, responde 503 con un mensaje claro, no abre la transacción (no consume cupos ni retiene bloqueos) y registra la causa: `timeout`, `servicio_no_disponible` o `circuit_breaker_abierto`.

---

## 10. Pruebas y resultados

### 10.1 Estrategia

| Suite | Tipo | Qué cubre | Casos |
|---|---|---|---:|
| `PaginasTest` | MVC (`@SpringBootTest` + MockMvc) | Flujo web completo, parámetros inválidos, 404/400 | 7 |
| `ServicioExternoCaidoTest` | MVC con verificador en modo `fallo` | 503 de punta a punta sin consumir cupo | 1 |
| `SeccionTest` | Unitaria | Invariante de cupos del record `Seccion` | 2 |
| `MatriculaServiceH2Test` | Integración (H2) | Reglas, contratos, rollback y carreras | 12 |
| `MatriculaServicePostgresTest` | Integración (PostgreSQL 17, Testcontainers) | Las mismas 12 sobre el motor real | 12 |
| `VerificacionAcademicaServiceTest` | Unitaria con stub | Reintento, timeout y circuit breaker | 3 |

### 10.2 Resultados

`./mvnw clean verify` (Java 21, Docker activo), 23 de octubre de 2026:

```
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Las capturas están en [`docs/evidencias/`](docs/evidencias/). No se generó porcentaje de cobertura ni prueba de carga.

---

## 11. Limitaciones y trabajo futuro

### 11.1 Limitaciones de v1.0

- Selección de estudiante sin autenticación ni CSRF (demo local).
- La verificación académica es un stub, no una integración real.
- Esquema inicializado con `schema.sql`, sin migraciones versionadas.
- Sin prueba de carga, percentiles ni cobertura.
- Auditoría OWASP pendiente de completar con API key de NVD.

### 11.2 Previsto para v2.0

- Reglas académicas intercambiables por institución mediante una interfaz común y perfiles configurables.
- Máquina de estados de matrícula (solicitada, confirmada, lista de espera, retirada).
- API REST documentada con OpenAPI, autenticación y CSRF.
- Migraciones versionadas (Flyway), métricas y prueba de carga del pico de apertura.
- Integración real con el servicio de verificación académica.
