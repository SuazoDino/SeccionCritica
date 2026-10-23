# Sección Crítica · Prototipo local

Motor de matrícula para el período de demostración 2026-II. Java 21, Spring Boot 4.1.0, Thymeleaf y Spring JDBC. Interfaz responsive con CSS propio, sin compilación de frontend.

## Ejecutar ahora

Requisito: JDK 21. El wrapper descarga Maven 3.9 la primera vez.

```powershell
Set-Location C:\SeccionCritica\Prototipo
.\mvnw.cmd spring-boot:run
```

Abre http://localhost:8080. El perfil predeterminado `demo` usa H2 en memoria con compatibilidad PostgreSQL. Los datos se reinician al detener la aplicación. La aplicación abre directamente en el inicio de sesión; las cuentas de prueba aparecen en la misma pantalla:

| Código | Estudiante | Contraseña |
|---|---|---|
| `20230001A` | Ana Torres | `Matricula2026` |
| `20230002B` | Luis Ramos | `Matricula2026` |
| `20230003C` | María Flores | `Matricula2026` |

## PostgreSQL

Inicia Docker y define una contraseña local antes de levantar la base:

```powershell
Copy-Item .env.example .env
$env:DB_PASSWORD='elige-una-clave-local'
docker compose up -d db
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

También se admiten `DB_URL`, `DB_USER`, `PORT` y `URL_PUBLICA` (base de los enlaces de recuperación). El esquema se crea si no existe, y los datos iniciales se insertan por identificador sin reponer cupos consumidos. El volumen de Docker conserva la información. `schema.sql` elimina las columnas de horario que versiones anteriores guardaban en `seccion`; para cambios futuros conviene incorporar una herramienta de migraciones (Flyway).

## Antes de una demostración en vivo

1. `.\mvnw.cmd test` debe terminar sin fallos.
2. `.\mvnw.cmd -DskipTests package` y ejecutar `java -jar target/seccion-critica-1.0.0.jar`: arranca más rápido que `spring-boot:run` y no depende de Maven.
3. Comprobar http://localhost:8080/actuator/health → `{"status":"UP"}`.
4. Con H2 basta reiniciar la aplicación para volver a los datos iniciales. Con PostgreSQL: `docker compose down -v` y volver a levantar.
5. Sin internet la página funciona igual: solo cambia la tipografía por la del sistema.

## Demostración

1. Ana: matricular Construcción de software (una sola vacante).
2. Cerrar sesión, ingresar como Luis e intentar el mismo curso: rechazo por cupo.
3. Ana: retirar el curso; Luis puede matricularlo después.
4. Luis: Estructuras de datos exige Programación I aprobada (no aparece en su oferta).
5. Luis: matricular Programación I A y luego Matemática discreta: cruce entre prácticas del miércoles.
6. Luis: con Programación I (4 créditos), Construcción de software (5) excede su máximo de 8, si el cupo está libre.
7. Programación I B tampoco puede matricularse si ya se tiene la sección A.

## Pruebas

```powershell
.\mvnw.cmd test
```

Para la verificación de entrega v1.0 ejecutar `.\mvnw.cmd clean verify`. El informe final,
runtime, resiliencia, errores, evidencia y trazabilidad está en `../docs/informe-final-v1.0.md`.

La especificación de contratos, invariantes y evidencia de concurrencia está en
`docs/contratos-matricula-y-concurrencia.md`. El análisis del árbol Maven,
licencias y auditoría OWASP está en `docs/analisis-dependencias.md`.

Para generar el árbol y ejecutar la auditoría bajo demanda:

```powershell
.\mvnw.cmd dependency:tree "-DoutputFile=target/dependency-tree.txt"
.\mvnw.cmd org.owasp:dependency-check-maven:check
```

- `service/MatriculaServiceH2Test` y `service/MatriculaServicePostgresTest` ejecutan las mismas pruebas (`ReglasMatriculaTestBase`): matrícula y retiro, retiro repetido, cada regla académica, identificadores inexistentes y carreras con hilos coordinados para último cupo, horarios y créditos por estudiante. Usan transacciones reales; las carreras no comparten una transacción. La clase de PostgreSQL usa Testcontainers y requiere Docker para reproducirse.
- `controller/AccesoTest` cubre el inicio de sesión (correcto, incorrecto, código en minúsculas), el cierre de sesión, el rechazo sin token CSRF, la redirección al login sin sesión y la recuperación de contraseña completa: validaciones de la clave nueva, enlace de un solo uso, enlace vencido o inventado y anulación del enlace anterior al pedir uno nuevo.
- `controller/PaginasTest` recorre la demo por HTTP con una sesión iniciada y comprueba que los enlaces mal formados (sección no numérica o faltante, estudiante inexistente, método incorrecto, ruta desconocida) muestran una página amable con el código adecuado y nunca un error 500.
- `service/VerificacionAcademicaServiceTest` y `controller/ServicioExternoCaidoTest` cubren reintento, timeout, circuit breaker y el 503 de punta a punta; `model/SeccionTest` cubre el invariante de cupos.

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
- Cada sección tiene sesiones de teoría y práctica en la tabla `sesion`. Solo los cruces práctica–práctica bloquean la matrícula; los extremos contiguos no se consideran cruce. Se modela un solo período. No hay lista de espera ni integración externa.
- Spring Security con formulario propio (`/login`). El usuario es el código de estudiante (sin distinguir mayúsculas) y la contraseña se guarda como hash BCrypt en la tabla `cuenta`. Todos los formularios llevan token CSRF.
- El estudiante de cada operación sale de la sesión (`UsuarioSesion`), nunca de la URL: un estudiante no puede ver ni matricular a otro.
- "Mantener la sesión iniciada" usa una cookie remember-me de 7 días. Cerrar sesión es un POST.
- Recuperación de contraseña: el enlace vence en 30 minutos, es de un solo uso y pedir otro anula el anterior. Solo se guarda el SHA-256 del token y el UPDATE condicional que lo consume evita que dos pestañas lo usen a la vez. La respuesta es la misma exista o no el código, para no revelar qué cuentas están registradas.
- El prototipo no envía correos: `NotificadorRegistro` escribe el enlace en el registro de la aplicación y, en el perfil `demo`, la pantalla lo muestra. Para correo real basta otra implementación de `Notificador` con `spring-boot-starter-mail`.

## Estructura

| Paquete / carpeta | Contenido |
|---|---|
| `controller/` | `AccesoController` (login y recuperación), `MatriculaController` (páginas y formularios) y `ManejadorErrores` (`@ControllerAdvice`) |
| `seguridad/` | `SeguridadConfig` (Spring Security), `DetalleUsuarios` y `UsuarioSesion` |
| `service/` | `MatriculaService` (reglas académicas y transacciones) y `CuentaService` (recuperación de contraseña) |
| `repository/` | Una clase por agregado con el SQL (`EstudianteRepository`, `CuentaRepository`, `CursoRepository`, `SeccionRepository`, `MatriculaRepository`) |
| `model/` | Records de dominio y de vista (`Estudiante`, `Curso`, `Seccion`, `Sesion`, `CursoOferta`, `EventoAgenda`, …) |
| `exception/` | `SeccionCriticaException` y sus subclases |
| `resources/` | Perfiles, `schema.sql`, `data.sql`, plantillas (`templates/`) y estilos (`static/`) |

## Pendientes

Migraciones versionadas, envío real de correos y bloqueo por intentos fallidos de inicio de sesión, historial con estados, reglas intercambiables (`ReglaMatricula`) y métricas de carga. La verificación académica externa ya cuenta con timeout, reintentos y circuit breaker en v1.0. La licencia Apache 2.0 está en [`LICENSE`](../LICENSE).
