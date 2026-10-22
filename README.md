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

Documentación técnica: [`documentacion_tecnica_parcial.md`](documentacion_tecnica_parcial.md) · Detalle del prototipo: [`Prototipo/README-prototipo.md`](Prototipo/README-prototipo.md)

## Stack del prototipo

Java 21 · Spring Boot 4.1 (Web MVC, Security, JDBC, Actuator) · Thymeleaf · H2 (demo) / PostgreSQL 17 · Docker Compose · JUnit 5 + Testcontainers

Previstos para versiones siguientes (aún no incluidos): API REST con OpenAPI, Resilience4j, reglas intercambiables por institución.

## Ejecutar

Requisitos: JDK 21. Maven no es necesario: se usa el wrapper incluido (`mvnw`).

```bash
cd Prototipo
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Abre http://localhost:8080 e inicia sesión con una cuenta de prueba (por ejemplo `20230001A` / `Matricula2026`; la pantalla de acceso lista las tres). Usa H2 en memoria; no requiere base de datos instalada.

Con PostgreSQL (requiere Docker):

```bash
cd Prototipo
cp .env.example .env            # define DB_PASSWORD
export DB_PASSWORD=elige-una-clave-local
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Estado de la aplicación: http://localhost:8080/actuator/health

## Pruebas

```bash
cd Prototipo
./mvnw test
```

Si Docker está disponible, las pruebas de reglas y concurrencia también se ejecutan contra PostgreSQL real; si no, esa clase se omite.

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

## Licencia

Apache 2.0 (pendiente de formalizar con el archivo `LICENSE`).
