# Documento técnico parcial — Sección Crítica

**Curso:** Construcción de Software II – SW707 | Universidad Nacional de Ingeniería
**Entregable:** v1.0 (Examen Parcial)
**Integrantes:** *Suazo Zanabria Dino, Chavez Oliva Carlos *
**Docente:** *Anibal Guillen*
**Fecha:** *2026-10-10*
**Repositorio:** *[enlace a GitHub]*

> Nota de uso: los apartados marcados con *[completar]* deben llenarse con capturas, métricas o resultados reales una vez ejecutado el código e integrados en el repositorio. El resto del documento describe el diseño y las decisiones técnicas que sustentan la implementación.

---

## 1. Descripción del problema y usuarios

### 1.1 Problema

Durante la apertura de la matrícula universitaria, miles de estudiantes intentan inscribirse al mismo tiempo en secciones de curso con cupos limitados. Sin un control adecuado de concurrencia en el sistema que gestiona esos cupos, se producen fallas estructurales:

- **Sobrecupo**: más estudiantes matriculados que vacantes disponibles en una sección.
- **Reglas académicas ignoradas**: matrículas que no respetan prerrequisitos aprobados o el límite máximo de créditos por ciclo.
- **Cruces de horario no detectados**: un estudiante queda matriculado en dos secciones con horarios superpuestos.
- **Degradación o caída del sistema** justo en el momento de mayor demanda (la "hora de apertura").

A esto se suma un problema de reúso: cada institución suele reconstruir desde cero su propio sistema de matrícula, con reglas distintas (créditos, prerrequisitos, fases de matrícula, paquetes de cursos), repitiendo los mismos errores de concurrencia y de diseño.

### 1.2 Usuarios y actores

| Actor | Rol en el sistema |
|---|---|
| **Estudiante** | Consulta la oferta de secciones, se matricula o se retira de una sección. |
| **Coordinador / secretaría académica** | Administra cursos, secciones, horarios y cupos; consulta el estado de la matrícula. |
| **Administrador institucional** | Configura las reglas académicas específicas de su institución (perfil de matrícula). |
| **Desarrollador / comunidad open source** | Integra, extiende o adapta el motor a un nuevo contexto institucional. |
| **Servicio externo de verificación académica** (simulado) | Sistema consumido por la API para validar deudas o situación académica del estudiante antes de confirmar la matrícula. |

### 1.3 Objetivo

Construir un motor de matrícula de código abierto y robusto, con reglas académicas configurables y extensibles, que garantice en todo momento que no se excedan los cupos, que se respeten prerrequisitos, créditos y horarios, y que mantenga un rendimiento estable durante los picos de demanda.

---

## 2. Alcance y requisitos principales

### 2.1 Alcance de la versión v1.0

- Registro de cursos, secciones, horarios y cupos.
- Registro de estudiantes con su historial de cursos aprobados.
- Matrícula de un estudiante en una sección, con validación de:
  - Prerrequisitos aprobados.
  - Créditos matriculados ≤ créditos máximos permitidos.
  - Ausencia de cruce de horarios con otras secciones ya matriculadas.
- Retiro de una matrícula, con liberación inmediata del cupo.
- Consulta de disponibilidad de cupos en tiempo real.
- Consulta del historial y la carga académica de cada estudiante.
- Exposición de una API REST documentada para que cualquier institución pueda integrar el motor.

### 2.2 Fuera de alcance en v1.0 (diferido a v2.0 o excluido)

- Internacionalización de la interfaz (prevista para v2.0).
- Máquina de estados completa de la matrícula (solicitada → confirmada → en lista de espera → retirada): en v1.0 se maneja un ciclo simplificado (confirmada / retirada); el modelo table-driven completo se implementa en v2.0.
- Contenerización completa del stack (Docker Compose se usa en desarrollo; el despliegue productivo es parte del alcance del final).
- Pagos, notificaciones push, reportes administrativos avanzados.

### 2.3 Requisitos no funcionales relevantes

| Requisito | Criterio de aceptación |
|---|---|
| Consistencia de cupos | Bajo ninguna condición de concurrencia el número de matriculados confirmados debe superar el cupo de la sección. |
| Disponibilidad ante fallas externas | El sistema debe degradarse controladamente (no caerse) si el servicio de verificación académica falla o responde con latencia alta. |
| Rendimiento bajo carga | El sistema debe sostener un pico simulado de solicitudes concurrentes sobre una misma sección sin pérdida de consistencia. *[completar con el umbral medido]* |
| Extensibilidad | Las reglas académicas (prerrequisitos, créditos, horarios) deben poder sustituirse sin modificar el núcleo del motor. |

---

## 3. Arquitectura y decisiones técnicas

### 3.1 Vista general

El sistema sigue una arquitectura en capas, con el núcleo de negocio desacoplado de las reglas académicas específicas de cada institución:

```
┌─────────────────────────────────────────────┐
│              API REST (Spring MVC)           │
│   Controladores: Matrícula, Secciones, ...   │
└───────────────────┬───────────────────────────┘
                     │
┌───────────────────▼───────────────────────────┐
│              Capa de Servicio                 │
│  MatriculaService · SeccionService · ...      │
│  (orquesta validaciones, locking, resiliencia)│
└───────────────────┬───────────────────────────┘
                     │
      ┌──────────────┼──────────────┐
      ▼              ▼              ▼
┌───────────┐  ┌────────────┐  ┌──────────────┐
│ Reglas     │  │ Persistencia│  │ Cliente      │
│ académicas │  │ (JPA /      │  │ resiliente   │
│ (interfaz  │  │ PostgreSQL) │  │ (servicio    │
│ + perfiles)│  │             │  │ externo)     │
└───────────┘  └────────────┘  └──────────────┘
```

### 3.2 Decisiones clave y justificación

| Decisión | Alternativa considerada | Justificación |
|---|---|---|
| Spring Boot + Java | Node.js/Express | Tipado estático, soporte maduro de concurrencia (JPA locking, `ExecutorService`) y del curso. |
| PostgreSQL con locking a nivel de fila | NoSQL con consistencia eventual | El problema exige consistencia fuerte sobre el cupo; `SELECT FOR UPDATE` y columnas de versión son nativos en PostgreSQL. |
| Reglas académicas como interfaz (`ReglaMatricula`) | Reglas embebidas en el servicio | Permite que cada institución sustituya o componga reglas sin tocar el núcleo (ver sección 4). |
| Resilience4j para el servicio externo | Reintentos manuales | Librería madura, declarativa, con circuit breaker y métricas integradas a Actuator. |
| API REST documentada con OpenAPI | GraphQL | Simplicidad de integración para instituciones externas; alineado al requisito de "mecanismo de integración apropiado". |

### 3.3 Modelo de datos (resumen)

Entidades principales: `Curso`, `Seccion` (con `cupoMaximo` y `cupoDisponible`), `Estudiante`, `HistorialAcademico`, `Matricula` (relación Estudiante–Sección con estado y marca de tiempo), `ReglaMatricula` (configuración por institución).

*[completar con el diagrama entidad-relación final una vez estabilizado el modelo]*

### 3.4 Estado de implementación en el prototipo

El diseño anterior describe la versión objetivo. El prototipo de `Prototipo/` implementa el núcleo con estas diferencias:

| Diseño | Prototipo actual |
|---|---|
| Persistencia con JPA | Spring JDBC (`JdbcClient`) para que los `SELECT ... FOR UPDATE` sean explícitos. Capas `controller` → `service` → `repository`, con records en `model`. |
| API REST con OpenAPI | Interfaz web Thymeleaf con formularios POST. La API REST queda pendiente. |
| Reglas como interfaz `ReglaMatricula` | Reglas (aprobado, duplicado, cupo, prerrequisito, créditos, cruce de prácticas) dentro de `MatriculaService.matricular()`. La extracción a una interfaz queda pendiente. |
| Jerarquía de excepciones (sección 7.1) | Implementada: `SeccionCriticaException`, `RecursoNoEncontradoException`, `MatriculaRechazadaException` y sus subclases de cupo, prerrequisito, créditos y cruce de horario. `ServicioExternoNoDisponibleException` queda pendiente con la sección 9. |
| Retiro con estado "retirada" | El retiro elimina la fila de `matricula`; no hay historial de estados. |
| `HistorialAcademico`, `ReglaMatricula` | Tabla `aprobado` (cursos aprobados por estudiante). |
| Resilience4j (sección 9) | No incluido todavía. Hay tiempos máximos de conexión, de bloqueo y de transacción, y manejo global de errores. |

Modelo de datos del prototipo: `estudiante`, `curso` (con `prerrequisito`), `aprobado`, `seccion` (`cupo_maximo`, `cupo_disponible`), `sesion` (teoría/práctica por día y minuto) y `matricula`.

---

## 4. Dependencias y reúso

### 4.1 Construcción PARA el reúso

El núcleo del motor se diseña como una librería agnóstica a la institución que lo usa. Las reglas académicas (prerrequisitos, créditos máximos, cruce de horarios) se modelan como una interfaz:

```java
public interface ReglaMatricula {
    ResultadoValidacion validar(Estudiante estudiante, Seccion seccion, ContextoMatricula contexto);
}
```

Cada institución compone su propio conjunto de reglas (un "perfil de matrícula") sin modificar el código del núcleo. Esto evita la sobre-ingeniería de construir un framework universal: el motor cubre un modelo base (cursos, secciones, cupos, reglas configurables) y demuestra su adaptabilidad con **dos perfiles de prueba distintos** (por ejemplo, matrícula por créditos libres frente a matrícula por paquete de cursos del ciclo).

### 4.2 Construcción CON reúso (dependencias de terceros)

| Dependencia | Propósito | Gestión de riesgo |
|---|---|---|
| `spring-boot-starter-web` | API REST | Versión fijada en `pom.xml`, actualizada mediante Dependabot/Renovate. |
| `spring-boot-starter-data-jpa` | Persistencia y locking | — |
| `postgresql` (driver JDBC) | Conexión a BD | — |
| `resilience4j-spring-boot3` | Resiliencia | — |
| `springdoc-openapi` | Documentación de API | — |
| `micrometer-registry-prometheus` | Métricas | — |

### 4.3 Análisis de dependencias transitivas y riesgos

*[completar]* ejecutar `mvn dependency:tree` y registrar aquí las dependencias transitivas relevantes, además de un análisis con una herramienta de verificación de vulnerabilidades conocidas (por ejemplo, OWASP Dependency-Check o la API de OSV.dev) sobre el árbol resultante. Documentar cualquier vulnerabilidad encontrada y la decisión tomada (actualizar versión, mitigar o aceptar el riesgo).

### 4.4 Licencias

El proyecto se publicará bajo licencia Apache 2.0. Antes de la publicación final se verificará la compatibilidad de esta licencia con la de cada dependencia directa y transitiva relevante, para evitar conflictos tipo "contaminación" por licencias copyleft (GPL).

---

## 5. Problemas de runtime

El curso exige identificar y analizar al menos dos problemas de tiempo de ejecución. Se documentan dos, ambos presentes en el diseño del motor:

### 5.1 Polimorfismo y enlace dinámico en la selección de reglas académicas

Las reglas académicas (`ValidadorPrerrequisitos`, `ValidadorCreditos`, `ValidadorHorario`, …) implementan la interfaz `ReglaMatricula`. El `MatriculaService` no conoce en tiempo de compilación qué implementación concreta se ejecutará: el runtime resuelve la llamada mediante enlace dinámico (V-Table), consultando la implementación real del objeto inyectado para el perfil institucional activo.

**Implicancia arquitectónica:** esta indirección habilita la extensibilidad (nuevas reglas sin modificar el código consumidor), pero tiene un costo de rendimiento no nulo (el compilador no puede hacer *inlining* de la llamada) y debe medirse si se vuelve relevante bajo el pico de carga de la apertura de matrícula.

**Mitigación:** el conjunto de reglas por perfil se resuelve una sola vez al iniciar la solicitud de matrícula (no en cada validación individual), y se mantiene un conjunto acotado de implementaciones conocidas en tiempo de compilación — se evita reflexión para este mecanismo, precisamente para no sumar el costo adicional de pérdida de *inlining* que sí tiene la reflexión.

### 5.2 Carga de reglas configurables por institución (enlace tardío controlado)

Para que una institución pueda activar un perfil de reglas distinto (por ejemplo, "créditos libres" vs. "paquete de ciclo") sin recompilar el sistema, el motor resuelve el perfil activo en tiempo de ejecución a partir de una configuración externa (ver sección de configuración, prevista para v2.0).

**Riesgo de seguridad identificado:** si el mecanismo de selección de reglas aceptara, sin validar, un nombre de clase arbitrario proveniente de configuración externa para cargarla por reflexión (el patrón que hizo posible Log4Shell), un atacante con control sobre esa configuración podría forzar la carga de una clase maliciosa. Por eso, en el diseño del motor:

- El conjunto de implementaciones de `ReglaMatricula` disponibles es una lista cerrada, registrada explícitamente en el contexto de Spring (no se cargan clases arbitrarias por nombre de cadena de texto desde configuración externa).
- La configuración externa solo selecciona **cuáles** reglas ya registradas componen el perfil activo, nunca **qué código** se ejecuta.

Esta decisión es una aplicación directa del principio visto en el curso: a mayor delegación de decisiones al runtime, mayor flexibilidad, pero también mayor superficie de ataque; el diseño limita deliberadamente esa superficie.

---

## 6. Contratos y programación defensiva

### 6.1 Diseño por contrato (Design by Contract)

| Componente | Precondición | Postcondición | Invariante |
|---|---|---|---|
| `MatriculaService.matricular()` | La sección existe y está activa; el estudiante existe. | Si la operación retorna éxito, existe una `Matricula` confirmada para ese estudiante y esa sección. | `matriculados ≤ cupoMaximo` de la sección, en todo momento. |
| `MatriculaService.retirar()` | Existe una matrícula confirmada para ese estudiante y esa sección. | El cupo de la sección se incrementa en 1; el estado de la matrícula pasa a "retirada". | El cupo liberado nunca excede el cupo máximo original. |
| `ValidadorCreditos.validar()` | El historial académico del estudiante está disponible. | Retorna un resultado explícito (válido/inválido con motivo), nunca `null`. | `creditosMatriculados ≤ creditosMaximoPermitido`. |
| `ValidadorHorario.validar()` | Las secciones ya matriculadas del estudiante tienen horario definido. | Retorna un resultado explícito. | Ninguna sección confirmada se superpone en horario con otra confirmada del mismo estudiante. |

Estos contratos se verifican con aserciones en desarrollo/pruebas y con validaciones explícitas (excepciones de dominio) en producción, siguiendo el principio de "falla rápido y explícito" en lugar de dejar que un estado inconsistente se propague silenciosamente.

### 6.2 Programación defensiva

- **Entradas no confiables**: todo dato que llega por la API (identificador de sección, de estudiante, parámetros de paginación) se valida en el borde del sistema (capa de controlador) antes de llegar a la capa de servicio.
- **Validación de datos de carga** (cuando el motor recibe catálogos de cursos o perfiles institucionales desde archivos externos): se valida estructura, tipos y rangos antes de persistir, rechazando archivos malformados con un mensaje explícito en vez de fallar de forma ambigua.
- **Nunca confiar en el estado del cliente**: el cupo disponible que ve el estudiante en pantalla es informativo; la validación real y autoritativa ocurre siempre en el servidor, en la transacción de matrícula.

---

## 7. Estrategia de errores y excepciones

### 7.1 Jerarquía de excepciones de dominio

```
RuntimeException
└── SeccionCriticaException (excepción base del dominio)
    ├── CupoNoDisponibleException        (409 Conflict)
    ├── PrerrequisitoNoCumplidoException  (422 Unprocessable Entity)
    ├── CreditosExcedidosException        (422 Unprocessable Entity)
    ├── CruceDeHorarioException           (422 Unprocessable Entity)
    ├── RecursoNoEncontradoException      (404 Not Found)
    └── ServicioExternoNoDisponibleException (503 Service Unavailable)
```

### 7.2 Política

- Las excepciones de dominio son **específicas y accionables**: cada una incluye el motivo exacto de rechazo, nunca un mensaje genérico de "error interno" cuando la causa es una regla de negocio.
- Un `@ControllerAdvice` centraliza el mapeo de excepciones de dominio a respuestas HTTP consistentes (código, mensaje, marca de tiempo, identificador de traza).
- Las excepciones técnicas no controladas (por ejemplo, fallas de conexión a base de datos) se capturan en el borde del sistema, se registran con el nivel `ERROR` y el stack trace completo, y se traducen a una respuesta genérica 500 sin exponer detalles internos al cliente.
- **Logging estructurado**: cada rechazo de matrícula por regla de negocio se registra en nivel `WARN` con el identificador de estudiante, sección y regla que falló, para poder auditar patrones de rechazo sin necesidad de reproducir el caso manualmente.

---

## 8. Concurrencia

### 8.1 Escenario concreto

**Condición de carrera:** dos (o más) estudiantes solicitan matricularse en la misma sección, que tiene exactamente un cupo disponible, al mismo tiempo. Sin control de concurrencia, ambas solicitudes podrían leer `cupoDisponible = 1`, decidir que hay espacio, y confirmar ambas matrículas — dejando la sección con un matriculado de más (sobrecupo).

### 8.2 Estrategias comparadas

**Opción A — Locking pesimista (`SELECT ... FOR UPDATE`)**
La transacción que intenta matricular bloquea la fila de la sección hasta confirmar o liberar. La segunda solicitud concurrente espera a que la primera transacción termine, y entonces lee el cupo ya actualizado.

```java
@Transactional
public Matricula matricular(Long estudianteId, Long seccionId) {
    Seccion seccion = seccionRepository.findByIdForUpdate(seccionId); // SELECT ... FOR UPDATE
    if (seccion.getCupoDisponible() <= 0) {
        throw new CupoNoDisponibleException(seccionId);
    }
    // ... validar reglas académicas ...
    seccion.decrementarCupo();
    return matriculaRepository.save(new Matricula(estudianteId, seccionId));
}
```

**Opción B — Locking optimista (columna de versión)**
Cada sección tiene una columna `version`. Al confirmar la matrícula, se actualiza condicionando a que la versión no haya cambiado desde la lectura. Si otra transacción ya modificó la fila, la actualización falla y se reintenta o se rechaza.

```java
@Version
private Long version;
```

**Barrera adicional:** independientemente de la estrategia de locking, una restricción `CHECK` en base de datos (`cupo_disponible >= 0`) actúa como última línea de defensa ante cualquier condición no contemplada en el código de aplicación.

### 8.3 Comparación y decisión

| Criterio | Pesimista (`FOR UPDATE`) | Optimista (versión) |
|---|---|---|
| Comportamiento bajo alta contención (pico de apertura) | Serializa el acceso; más predecible pero puede generar cola de espera | Menor bloqueo, pero más reintentos fallidos bajo alta contención |
| Complejidad de implementación | Baja | Media (requiere manejo explícito de reintentos) |
| Riesgo de interbloqueo | Mayor si se combina con otros locks | Ninguno |

*[completar]*: Decisión final del equipo tras medir ambas estrategias bajo carga simulada (ver sección 10), con la justificación basada en los resultados obtenidos y no solo en la teoría.

### 8.4 Prueba reproducible

Se implementa una prueba con `ExecutorService` y `CountDownLatch` que lanza *N* hilos concurrentes intentando matricularse en una sección con un único cupo disponible, y verifica que:

1. Exactamente un hilo obtiene la matrícula confirmada.
2. Los *N-1* hilos restantes reciben `CupoNoDisponibleException`.
3. El estado final en base de datos es consistente (`matriculados == cupoMaximo`).

Implementación: `ReglasMatriculaTestBase.ultimoCupoTieneUnSoloGanador()` en `Prototipo/src/test/java/pe/uni/seccioncritica/service/`. Tres hilos esperan en un `CountDownLatch` y se liberan a la vez sobre la sección de un único cupo. La misma clase se ejecuta contra H2 (`MatriculaServiceH2Test`) y contra PostgreSQL 17 con Testcontainers (`MatriculaServicePostgresTest`). Resultado: un solo éxito, cupo final 0 y una sola fila en `matricula`, en ambas bases. Los rechazos llegan como `CupoNoDisponibleException`.

Prueba manual adicional sobre PostgreSQL: 30 solicitudes HTTP simultáneas (3 estudiantes × 10) por el último cupo terminaron con una sola matrícula, cupo 0 y ningún error en el log.

---

## 9. Tolerancia a fallas

### 9.1 Dependencia externa simulada

El flujo de matrícula consulta un servicio externo (simulado) de verificación de deudas y situación académica antes de confirmar. Este servicio puede fallar, responder con latencia alta o no estar disponible.

### 9.2 Mecanismos aplicados (Resilience4j)

| Mecanismo | Configuración | Propósito |
|---|---|---|
| **Timeout** | *[completar, p. ej. 2s]* | Evitar que una solicitud de matrícula quede indefinidamente esperando al servicio externo. |
| **Reintentos con backoff** | *[completar, p. ej. 3 intentos, backoff exponencial]* | Absorber fallas transitorias de red sin involucrar al usuario. |
| **Circuit breaker** | *[completar umbral de fallas]* | Si el servicio externo falla reiteradamente, dejar de llamarlo temporalmente y degradar la respuesta en lugar de saturar el sistema con solicitudes que fallarán. |
| **Fallback** | Rechazo explícito y auditable de la matrícula, con mensaje claro ("no se pudo verificar la situación académica, intente nuevamente") | Evitar tanto confirmar matrículas sin verificación como colgar la solicitud del estudiante. |

### 9.3 Resultado esperado

Ante una falla del servicio externo, el sistema debe: (a) no caerse ni bloquear otras solicitudes de matrícula no relacionadas, (b) responder al estudiante con un mensaje claro y un código HTTP apropiado (503), y (c) registrar el evento para observabilidad.

*[completar con evidencia: log o métrica mostrando el circuit breaker abriéndose ante fallas simuladas]*

---

## 10. Pruebas y resultados

### 10.1 Estrategia de pruebas

| Tipo | Herramienta | Qué cubre |
|---|---|---|
| Unitarias | JUnit 5 + Mockito | Reglas académicas individuales, mapeo de excepciones. |
| De contrato | JUnit 5 | Precondiciones/postcondiciones de `MatriculaService`. |
| De concurrencia | JUnit 5 + `ExecutorService`/`CountDownLatch` | Condición de carrera del último cupo (sección 8.4). |
| De integración | `@SpringBootTest` + Testcontainers (PostgreSQL) | Flujo completo de matrícula contra una base de datos real. |
| De resiliencia | Resilience4j test-support / WireMock | Simulación de fallas del servicio externo. |
| De carga | JMeter / Gatling | Comportamiento bajo un pico simulado de solicitudes concurrentes. |

### 10.2 Resultados

*[completar con capturas de ejecución, cobertura de pruebas, y métricas de la prueba de carga: número de solicitudes concurrentes simuladas, tasa de éxito/rechazo, tiempo de respuesta p50/p95/p99]*

---

## 11. Limitaciones y trabajo futuro

### 11.1 Limitaciones conocidas de v1.0

- El ciclo de vida de la matrícula es simplificado (confirmada/retirada); no implementa aún lista de espera ni fases de matrícula.
- La configuración de perfiles institucionales no está externalizada todavía (se compone programáticamente); la externalización completa con *feature flags* es trabajo de v2.0.
- No se incluye todavía observabilidad con trazas distribuidas ni panel de métricas.
- El servicio de verificación académica es simulado (stub/mock), no una integración real.

### 11.2 Trabajo previsto para v2.0 (Examen Final)

- Máquina de estados *table-driven* completa de la matrícula (solicitada, confirmada, en lista de espera, retirada, rechazada).
- Configuración externalizada y *feature flags* (por ejemplo, fases de matrícula regular/rezagados, activar o no la lista de espera).
- Internacionalización de la interfaz y los mensajes.
- Logs estructurados y métricas expuestas (tasa de rechazo por cupo, tiempo de respuesta, cupos restantes).
- Contenerización completa con Docker Compose para despliegue de un solo comando.
- *Baseline*, profiling y optimización de rendimiento bajo carga simulando la hora de apertura real.
- Refactorización respaldada por pruebas sobre los *code smells* identificados en v1.0.
- Reingeniería del módulo de asignación de cupos de v1.0, tratado como componente legacy, con pruebas de caracterización previas.
- Ingeniería reversa de la arquitectura resultante.
- Publicación formal del repositorio con licencia abierta, README de contribución y al menos dos perfiles institucionales de ejemplo funcionando sobre el mismo núcleo.