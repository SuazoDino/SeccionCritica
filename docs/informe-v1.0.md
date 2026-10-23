# Informe v1.0 — resumen de entrega

Este archivo conserva el nombre utilizado por la documentación anterior. El informe académico consolidado y trazable está en [informe-final-v1.0.md](./informe-final-v1.0.md).

## Estado comprobado

- `main` contiene el merge de la PR v1.0 (`129f75b`).
- `mvnw clean verify` terminó el 23 de octubre de 2026 con `BUILD SUCCESS`.
- Se ejecutaron 37 pruebas: 7 de páginas, 1 de servicio externo caído (503), 2 del invariante de `Seccion`, 12 H2, 12 PostgreSQL con Testcontainers y 3 de verificación académica (reintento, timeout y circuit breaker); 0 fallos, 0 errores y 0 omitidas.
- El árbol Maven se generó con `\.\mvnw.cmd dependency:tree "-DoutputFile=target/dependency-tree.txt"`.
- OWASP Dependency-Check 12.1.0 quedó inconcluso durante la actualización de NVD: 413,340 registros informados y 10,000 descargados sin API key.

La guía de exposición está en [Demostracion-v1.0.md](./Demostracion-v1.0.md) y la guía de capturas en [Evidencias-v1.0.md](./Evidencias-v1.0.md).
