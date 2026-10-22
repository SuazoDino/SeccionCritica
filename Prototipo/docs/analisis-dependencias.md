# Análisis de dependencias y auditoría

## Alcance y procedimiento

El proyecto mantiene Java 21, Spring Boot 4.1.0, Spring JDBC, H2 y PostgreSQL. Las dependencias directas se declaran en `pom.xml`; Spring Boot administra sus versiones mediante el BOM del parent. Para generar y revisar el árbol:

```powershell
cd Prototipo
mvn dependency:tree -DoutputFile=target/dependency-tree.txt
```

El árbol distingue dependencias directas de transitivas. Entre las transitivas relevantes aparecen Spring Framework, HikariCP, Thymeleaf, Jackson, Logback, JUnit, Mockito, AssertJ, Testcontainers y sus clientes de base de datos. El árbol generado en `target/` es un artefacto de compilación y no se versiona.

## Dependencias directas

| Dependencia | Uso | Alcance |
|---|---|---|
| `spring-boot-starter-webmvc` | Controladores MVC y servidor web | producción |
| `spring-boot-starter-thymeleaf` | Vistas existentes | producción |
| `spring-boot-starter-jdbc` | JDBC y transacciones | producción |
| `spring-boot-starter-actuator` | health check existente | producción |
| `h2` | Base demo y pruebas | runtime |
| `postgresql` | Base de producción/integración | runtime |
| `spring-boot-starter-test` | JUnit, Mockito y utilidades | test |
| `spring-boot-starter-webmvc-test` | pruebas MVC | test |
| `spring-boot-testcontainers` | integración Spring–Testcontainers | test |
| `testcontainers-junit-jupiter` | ciclo de vida de contenedores | test |
| `testcontainers-postgresql` | PostgreSQL real para pruebas | test |

No se actualizaron versiones indiscriminadamente. El único agregado es el plugin de auditoría y no introduce una dependencia de runtime de la aplicación.

## OWASP Dependency-Check

Se configuró `org.owasp:dependency-check-maven:12.1.0` en `pom.xml` sin asociarlo a una fase del ciclo de vida. Por ello, `test` y `package` siguen siendo normales; la auditoría se solicita explícitamente:

```powershell
cd Prototipo
mvn org.owasp:dependency-check-maven:check
```

El umbral está configurado en CVSS 7.0. La auditoría incluye el alcance de pruebas. Los informes HTML y JSON se generan en `target/`.

## Resultado de auditoría

En esta ejecución la auditoría sí se inició. Dependency-Check informó 413,320 registros NVD y descargó 10,000 antes de detenerse por el tiempo de actualización sin API key. No se generó un resultado concluyente ni un inventario de CVE; por tanto, no se declara “sin vulnerabilidades”. Debe repetirse con una API key o con tiempo suficiente y conservar los informes HTML/JSON resultantes.

## Conflictos y licencias

Spring Boot centraliza versiones de Spring, logging, JSON y testing; `dependency:tree` debe revisarse para detectar una misma coordenada con varias versiones. No se observó en la inspección del `pom.xml` una versión explícita que fuerce un conflicto, pero el árbol efectivo es la evidencia necesaria.

Las licencias de las dependencias principales deben confirmarse con sus POM/NOTICE y el reporte legal del repositorio antes de publicar. Spring Boot, H2, PostgreSQL JDBC, JUnit, Mockito, Testcontainers y OWASP Dependency-Check son proyectos de licencias permisivas o de uso habitual, pero esta revisión no sustituye una verificación legal completa de todas las transitivas.

## Limitaciones

- La auditoría depende de red y de la disponibilidad de la base de vulnerabilidades de NVD.
- No se añadieron versiones manuales para “corregir” riesgos que no hayan sido confirmados por el reporte.
- Docker no estuvo disponible durante la línea base; las dependencias PostgreSQL de Testcontainers quedaron omitidas en pruebas.
