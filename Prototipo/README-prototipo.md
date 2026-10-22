# Sección Crítica · Prototipo local

Motor de matrícula para el período de demostración 2026-II. Java 21, Spring Boot 4.1.0, Thymeleaf y Spring JDBC. Interfaz responsive con CSS propio, sin compilación de frontend.

## Ejecutar ahora

Requisito: JDK 21. El wrapper descarga Maven 3.9 la primera vez.

```bash
cd Prototipo
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Abre http://localhost:8080. El perfil predeterminado `demo` usa H2 en memoria con compatibilidad PostgreSQL. Los datos se reinician al detener la aplicación. Selecciona Ana, Luis o María para recorrer oferta y matrículas.

## PostgreSQL

Inicia Docker y define una contraseña local antes de levantar la base:

```bash
cp .env.example .env                       # compose lee DB_PASSWORD de aquí
export DB_PASSWORD=elige-una-clave-local   # PowerShell: $env:DB_PASSWORD='...'
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

También se admiten `DB_URL`, `DB_USER` y `PORT`. El esquema se crea si no existe, y los datos iniciales se insertan por identificador sin reponer cupos consumidos. El volumen de Docker conserva la información. `schema.sql` elimina las columnas de horario que versiones anteriores guardaban en `seccion`; para cambios futuros conviene incorporar una herramienta de migraciones (Flyway).

## Antes de una demostración en vivo

1. `./mvnw test` debe terminar sin fallos.
2. `./mvnw -DskipTests package` y ejecutar `java -jar target/seccion-critica-0.1.0-SNAPSHOT.jar`: arranca más rápido que `spring-boot:run` y no depende de Maven.
3. Comprobar http://localhost:8080/actuator/health → `{"status":"UP"}`.
4. Con H2 basta reiniciar la aplicación para volver a los datos iniciales. Con PostgreSQL: `docker compose down -v` y volver a levantar.
5. Sin internet la página funciona igual: solo cambia la tipografía por la del sistema.

## Demostración

1. Ana: matricular Construcción de software (una sola vacante).
2. Cambiar a Luis e intentar el mismo curso: rechazo por cupo.
3. Ana: retirar el curso; Luis puede matricularlo después.
4. Luis: Estructuras de datos exige Programación I aprobada (no aparece en su oferta).
5. Luis: matricular Programación I A y luego Matemática discreta: cruce entre prácticas del miércoles.
6. Luis: con Programación I (4 créditos), Construcción de software (5) excede su máximo de 8, si el cupo está libre.
7. Programación I B tampoco puede matricularse si ya se tiene la sección A.

## Pruebas

```bash
./mvnw test
```

Para la verificación de entrega v1.0 ejecutar `./mvnw clean verify`. El informe de cambios,
runtime, resiliencia, errores, evidencia y trazabilidad está en `../docs/informe-v1.0.md`.

La especificación de contratos, invariantes y evidencia de concurrencia está en
`docs/contratos-matricula-y-concurrencia.md`. El análisis del árbol Maven,
licencias y auditoría OWASP está en `docs/analisis-dependencias.md`.

Para generar el árbol y ejecutar la auditoría bajo demanda:

```bash
./mvnw dependency:tree -DoutputFile=target/dependency-tree.txt
./mvnw org.owasp:dependency-check-maven:check
```

- `service/MatriculaServiceH2Test` y `service/MatriculaServicePostgresTest` ejecutan las mismas pruebas (`ReglasMatriculaTestBase`): matrícula y retiro, retiro repetido, cada regla académica, identificadores inexistentes y carreras con hilos coordinados para último cupo, horarios y créditos por estudiante. Usan transacciones reales; las carreras no comparten una transacción. La clase de PostgreSQL usa Testcontainers y se omite si Docker no está disponible.
- `controller/PaginasTest` recorre la demo por HTTP y comprueba que los enlaces mal formados (sin estudiante, identificador no numérico o inexistente, método incorrecto, ruta desconocida) muestran una página amable con el código adecuado y nunca un error 500.

## Robustez

- Todas las excepciones terminan en `error.html` con un mensaje claro (`ManejadorErrores`); nunca se muestran trazas.
- Los rechazos por regla y la contención momentánea de la base vuelven al panel con un diálogo, sin modificar datos.
- Tiempos máximos: 5 s para obtener conexión, 5 s de espera por bloqueo y 10 s por transacción de matrícula. Una base lenta produce "intenta nuevamente", no una página colgada.
- El botón se desactiva al enviar para evitar matrículas dobles por clics repetidos; el servidor igual rechaza duplicados.

## Decisiones

- JDBC (`JdbcClient`) permite ver explícitamente los bloqueos `SELECT ... FOR UPDATE`.
- Todas las escrituras bloquean primero estudiante y luego sección, dentro de una transacción. Esto serializa reglas por estudiante y consumo de cupos por sección, y el orden fijo evita interbloqueos.
- La base aplica claves foráneas, unicidad de matrícula y límites de cupos (`CHECK`).
- El retiro elimina la matrícula activa; no se conserva historial de retiros en este prototipo.
- Cada sección tiene sesiones de teoría y práctica en la tabla `sesion`. Solo los cruces práctica–práctica bloquean la matrícula; los extremos contiguos no se consideran cruce. Se modela un solo período. No hay lista de espera, integración externa ni autenticación.
- Los formularios usan POST. La selección de estudiante es solo para demostración: no hay autorización ni protección CSRF. Antes de exponerlo se requiere Spring Security, identidad y autorización por estudiante.

## Estructura

| Paquete / carpeta | Contenido |
|---|---|
| `controller/` | `MatriculaController` (páginas y formularios) y `ManejadorErrores` (`@ControllerAdvice`) |
| `service/` | `MatriculaService`: reglas académicas y transacciones |
| `repository/` | Una clase por agregado con el SQL (`EstudianteRepository`, `CursoRepository`, `SeccionRepository`, `MatriculaRepository`) |
| `model/` | Records de dominio y de vista (`Estudiante`, `Curso`, `Seccion`, `Sesion`, `CursoOferta`, `EventoAgenda`, …) |
| `exception/` | `SeccionCriticaException` y sus subclases |
| `resources/` | Perfiles, `schema.sql`, `data.sql`, plantillas (`templates/`) y estilos (`static/`) |

## Pendientes

Migraciones versionadas, autenticación/CSRF, historial con estados, reglas intercambiables (`ReglaMatricula`) y métricas de carga. La verificación académica externa ya cuenta con timeout, reintentos y circuit breaker en v1.0. La licencia Apache 2.0 debe formalizarse con un archivo `LICENSE`.
