# Sección Crítica

Motor de matrícula de código abierto con control de cupos concurrentes.

## Carátula

- **Universidad:** Universidad Nacional de Ingeniería (UNI) — FIIS
- **Curso:** Construcción de Software II – SW707
- **Docente:** Anibal Guillen
- **Semestre:** 2026-II
- **Integrantes:**

| Nombre completo | Código |
|---|---|
| **Suazo Zanabria Dino** | *20231324K* |
| **Chavez Oliva Carlos** | *20230131G* |

## Descripción

Durante la apertura de matrícula, miles de estudiantes compiten por cupos limitados. Sin control de concurrencia se generan sobrecupos, cruces de horario y caídas del sistema. **Sección Crítica** resuelve ese núcleo (cupos, concurrencia, reglas académicas, resiliencia) de forma configurable para distintas instituciones.

Documentación: [`docs/informe-final-v1.0.md`](docs/informe-final-v1.0.md) · [`docs/guia-demostracion-v1.0.md`](docs/guia-demostracion-v1.0.md) · [`docs/guia-evidencias-v1.0.md`](docs/guia-evidencias-v1.0.md) · [`Prototipo/README-prototipo.md`](Prototipo/README-prototipo.md)

## Stack del prototipo

Java 21 · Spring Boot 4.1 (Web MVC, JDBC, Actuator) · Thymeleaf · H2 (demo) / PostgreSQL 17 · Docker Compose · JUnit 5 + Testcontainers

Previstos para versiones siguientes: API REST/OpenAPI, autenticación, reglas intercambiables por institución y migraciones versionadas. Resilience4j ya se usa en v1.0 para la frontera de verificación académica.

## Ejecutar

Requisitos: JDK 21. Maven no es necesario: se usa el wrapper incluido (`mvnw`).

```powershell
Set-Location Prototipo
.\mvnw.cmd spring-boot:run
```

Abre http://localhost:8080. Usa H2 en memoria; no requiere base de datos instalada.

Con PostgreSQL (requiere Docker):

```powershell
Set-Location Prototipo
Copy-Item .env.example .env
$env:DB_PASSWORD='elige-una-clave-local'
docker compose up -d db
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Estado de la aplicación: http://localhost:8080/actuator/health

## Pruebas

```powershell
Set-Location Prototipo
.\mvnw.cmd clean verify
```

La evidencia v1.0 comprobada incluye PostgreSQL 17 mediante Testcontainers; Docker debe estar operativo para reproducir esa parte.

## Estructura

```
.
├── README.md
├── documentacion_tecnica_parcial.md
└── Prototipo/                      proyecto Spring Boot (Maven)
    ├── pom.xml · mvnw · compose.yaml · .env.example
    ├── Capturas/                   capturas de la demo
    └── src/
        ├── main/java/pe/uni/seccioncritica/
        │   ├── Application.java
        │   ├── controller/         páginas web y manejo global de errores
        │   ├── service/            reglas de matrícula y transacciones
        │   ├── repository/         consultas SQL (JdbcClient)
        │   ├── model/              records de dominio
        │   └── exception/          excepciones de dominio
        ├── main/resources/         perfiles, schema.sql, data.sql, templates/, static/
        └── test/java/...           pruebas de servicio (H2 y PostgreSQL) y de páginas
```

## Documentación técnica

- [`docs/informe-final-v1.0.md`](docs/informe-final-v1.0.md): informe académico consolidado y matriz de rúbrica.
- [`docs/guia-demostracion-v1.0.md`](docs/guia-demostracion-v1.0.md): pasos de exposición y comandos PowerShell.
- [`docs/guia-evidencias-v1.0.md`](docs/guia-evidencias-v1.0.md): capturas que aún deben obtenerse manualmente.
- [`docs/evidencia-pruebas-v1.0.md`](docs/evidencia-pruebas-v1.0.md): resultado comprobado de pruebas.
- [`Prototipo/docs/analisis-dependencias.md`](Prototipo/docs/analisis-dependencias.md): árbol Maven y auditoría OWASP.
- [`Prototipo/docs/contratos-matricula-y-concurrencia.md`](Prototipo/docs/contratos-matricula-y-concurrencia.md): contratos y concurrencia.

## Licencia

Apache 2.0 (pendiente de formalizar con el archivo `LICENSE`).
