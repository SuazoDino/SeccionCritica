# Sección Crítica

Motor de matrícula de código abierto con control de cupos concurrentes.

## Carátula

- **Universidad:** Universidad Nacional de Ingeniería (UNI) — FIIS
- **Curso:** Construcción de Software II – SW707
- **Docente:** *[Nombre del docente]*
- **Semestre:** *[2026-II]*
- **Integrantes:**

| Nombre completo | Código |
|---|---|
| **Suazo Zanabria Dino** | *20231324K* |
| **Chavez Oliva Carlos** | *20230131G* |

## Descripción

Durante la apertura de matrícula, miles de estudiantes compiten por cupos limitados. Sin control de concurrencia se generan sobrecupos, cruces de horario y caídas del sistema. **Sección Crítica** resuelve ese núcleo (cupos, concurrencia, reglas académicas, resiliencia) de forma configurable para distintas instituciones.

Documentación técnica completa: [`docs/documento-tecnico-parcial.md`](docs/documento-tecnico-parcial.md)

## Stack

Java + Spring Boot · PostgreSQL · React/Thymeleaf · API REST · Resilience4j · Docker · JUnit 5

## Instalación

```bash
git clone <url-del-repositorio>
cd seccion-critica
cp .env.example .env
docker-compose up -d db
mvn clean install
mvn spring-boot:run -pl src
```

API: `http://localhost:8080` · Docs OpenAPI: `http://localhost:8080/swagger-ui.html`

## Pruebas

```bash
mvn test
```

## Estructura

```
src/ · tests/ · docs/ · config/ · deployment/ · scripts/ · benchmarks/ · legacy/
```

## Licencia

Apache 2.0