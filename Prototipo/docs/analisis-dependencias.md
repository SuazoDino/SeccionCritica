# Análisis de dependencias v1.0

## Procedimiento

Desde `C:\SeccionCritica\Prototipo` se ejecutó:

```powershell
.\mvnw.cmd dependency:tree "-DoutputFile=target/dependency-tree.txt"
```

El comando terminó con `BUILD SUCCESS`. El archivo en `target/` es generado y no debe versionarse.

## Dependencias directas

| Coordenada / grupo | Uso | Alcance |
|---|---|---|
| `spring-boot-starter-webmvc` | MVC, servidor web y controladores | compile |
| `spring-boot-starter-thymeleaf` | Vistas HTML | compile |
| `spring-boot-starter-jdbc` | `JdbcClient`, transacciones y HikariCP | compile |
| `spring-boot-starter-actuator` | `/actuator/health` | compile |
| `resilience4j-circuitbreaker` 2.2.0 | Circuit breaker | compile |
| `resilience4j-retry` 2.2.0 | Reintentos | compile |
| `h2` | Demo y pruebas en memoria | runtime |
| `postgresql` | Perfil y persistencia PostgreSQL | runtime |
| `spring-boot-starter-test` | JUnit, Mockito, AssertJ y utilidades | test |
| `spring-boot-starter-webmvc-test` | Pruebas MVC | test |
| `spring-boot-testcontainers` | Integración Spring–Testcontainers | test |
| `testcontainers-junit-jupiter` | Ciclo de vida JUnit | test |
| `testcontainers-postgresql` | Contenedor PostgreSQL 17 | test |

## Transitivas observadas

El árbol real incluye Spring Framework 7.0.8, Tomcat 11.0.22, HikariCP 7.0.2, Thymeleaf 3.1.5, Logback 1.5.34, SLF4J 2.0.18, Jackson 3.1.4, Micrometer 1.17.0, JUnit 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, Awaitility 4.3.0, Testcontainers 2.0.5, Docker Java 3.7.1, H2 2.4.240 y PostgreSQL JDBC 42.7.11.

La gestión centralizada del parent Spring Boot reduce divergencias de versión. Los riesgos que deben vigilarse son la superficie transitiva de web/Jackson/logging, el driver de base de datos y Docker Java utilizado por las pruebas. Esta revisión no inventa vulnerabilidades: el reporte OWASP no alcanzó un estado concluyente.

## OWASP Dependency-Check

El `pom.xml` configura `org.owasp:dependency-check-maven:12.1.0` bajo demanda, con formatos HTML y JSON, incluyendo el alcance de pruebas y umbral CVSS 7.0. Se intentó ejecutar:

```powershell
.\mvnw.cmd org.owasp:dependency-check-maven:check
```

La actualización NVD informó 413,340 registros, descargó 10,000 (aproximadamente 2%), mostró reintentos y no utilizó API key. La auditoría quedó inconclusa. No se afirma ausencia de vulnerabilidades ni se presenta un inventario CVE.

## Dependabot (GitHub)

El 23 de octubre de 2026 se activaron las alertas de Dependabot en el repositorio. El análisis terminó con **0 alertas** sobre las 16 dependencias declaradas en `pom.xml`. GitHub no resuelve el árbol transitivo de Maven sin un flujo de envío de dependencias, por lo que este resultado cubre las directas; las transitivas siguen documentadas con `dependency:tree`. Evidencia: `docs/evidencias/10-dependabot-sin-alertas.png`.

## Límites

- El árbol de dependencias sí fue generado con éxito.
- La auditoría OWASP requiere una nueva ejecución con red estable, API key o tiempo suficiente.
- Las licencias transitivas requieren una revisión legal separada.
- No se agregan archivos de `target/` al repositorio.
