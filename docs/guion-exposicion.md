# Guion de exposición v1.0 — Sección Crítica

Duración total: 15 minutos como máximo.

| Bloque | Quién | Tiempo |
|---|---|---|
| 1. Introducción | Dino | ~2 min |
| 2. Teoría y código | Carlos | ~7 min |
| 3. Demo y cierre | Dino | ~5.5 min |

Lo que va entre comillas se dice; lo que va entre **[corchetes]** se hace en pantalla.

## Preparación previa

- Reiniciar la aplicación (H2 en memoria: los datos vuelven al estado inicial).
- Pestaña 1: `http://localhost:8080` con **Ana** (`20230001A`).
- Pestaña 2: `http://127.0.0.1:8080` con **María** (`20230003C`), abierta en la oferta.
  Son direcciones distintas para el navegador, así que cada una conserva su propia sesión.
- Contraseña de todas las cuentas: `Matricula2026`.
- Una terminal en `Prototipo/` con este comando escrito y sin ejecutar:
  `VERIFICACION_ACADEMICA_MODO=fallo ./mvnw spring-boot:run`

---

## 1. Introducción — Dino (~2 min)

> "Buenos días, profesor. Somos Dino Suazo y Carlos Chavez, y presentamos **Sección Crítica**, un motor de matrícula de código abierto con control de cupos concurrentes. Esta es la versión **v1.0**, correspondiente al examen parcial."

> "El problema que abordamos es el siguiente: durante la apertura de matrícula, cientos o miles de estudiantes compiten **simultáneamente** por un número limitado de vacantes. Si no hay un control de concurrencia adecuado, aparecen tres fallas típicas. La primera es el **sobrecupo**: dos transacciones leen la misma vacante disponible y ambas la confirman, lo que es una **condición de carrera**. La segunda es la **violación de reglas académicas**, como prerrequisitos no aprobados, exceso de créditos o cruces de horario. La tercera es la **degradación del sistema** cuando un servicio externo del que dependemos falla justo en el pico de demanda."

> "Nuestra solución se enfoca en ese núcleo crítico: garantizar la **consistencia del cupo** bajo concurrencia, validar las reglas académicas dentro de una **transacción atómica** y aislar las fallas externas con mecanismos de **tolerancia a fallas**."

> "El prototipo está construido con **Java 21 y Spring Boot 4.1**, persistencia **JDBC** sobre **H2** para la demo y **PostgreSQL 17** como motor real, **Resilience4j** para la resiliencia y **JUnit 5 con Testcontainers** para las pruebas de integración contra una base de datos real."

> "A continuación, Carlos va a explicar cómo está construido por dentro: la gestión de dependencias, los problemas de runtime, los contratos, la concurrencia y la resiliencia. Al final yo les mostraré todo eso funcionando en la aplicación."

---

## 2. Teoría y código — Carlos (~7 min)

1. **Dependencias** (30 s): `pom.xml` (el parent de Spring Boot gestiona las versiones; alcances compile, runtime y test separados), árbol transitivo (evidencia 09) y Dependabot sin alertas (evidencia 10).
2. **Problemas de runtime** (1 min): la carrera por la última vacante y la contención de bloqueos con su tabla de timeouts (lock 5 s, transacción 10 s, pool 5 s, servicio externo 2 s). Documento técnico, §5.
3. **Concurrencia** (1.5 min): `MatriculaCommandService`, `SELECT … FOR UPDATE`, orden de bloqueo estudiante → sección, por qué pesimista y no optimista, y la prueba `ultimoCupoTieneUnSoloGanador` con 5 hilos.
4. **Design by Contract** (1.5 min): Javadoc Pre/Post de `matricular` y `retirar`, invariante del record `Seccion`, postcondición de `ajustarCupo` y `CHECK` en `schema.sql`.
5. **Programación defensiva, errores y logging** (1 min): jerarquía bajo `SeccionCriticaException`, `ManejadorErrores` y logs `evento=...`.
6. **Resiliencia** (1 min): `VerificacionAcademicaService` con timeout, retry, circuit breaker y rollback transaccional.
7. **Pruebas** (30 s): suites en H2 y PostgreSQL (Testcontainers), resiliencia y páginas (evidencia 07).

> Pase: "Ahora Dino lo muestra en la aplicación."

---

## 3. Demo — Dino (~5.5 min)

### 3.1 Flujo de negocio completo (Funcionalidad)

> "Gracias, Carlos. Ahora veamos el sistema en ejecución."

**[Pestaña de Ana, en Oferta de cursos]**

> "Ingresé como Ana Torres. La oferta ya viene **filtrada en el servidor**: Ana aprobó Programación I, así que ese curso no aparece, y los cursos cuyo prerrequisito no cumple tampoco se muestran."

**[Despliega Construcción de software]**

> "Esta sección tiene **una sola vacante**. Este es el escenario crítico: la última vacante."

**[Clic en Matricularme]**

> "La matrícula se confirmó. Las vacantes pasaron de **1 a 0** y los créditos de Ana subieron a **5 de 12**. Todo eso ocurrió en una única transacción: se insertó la matrícula y se decrementó el cupo de forma **atómica**."

**[Clic en Mis matrículas]**

> "Aquí está su agenda semanal con las sesiones de teoría y práctica del curso."

### 3.2 Concurrencia: la última vacante

**[Cambia a la pestaña de María, que todavía muestra 1 vacante]**

> "Ahora, la parte más importante. Esta es la sesión de María Flores. Abrió la oferta **antes** de que Ana se matriculara, así que su pantalla todavía muestra **1 vacante disponible**. Es exactamente lo que pasa en la apertura de matrícula: dos estudiantes ven el mismo cupo al mismo tiempo."

**[Clic en Matricularme en Construcción de software]**

> "El sistema la rechaza: *'La sección ya no tiene vacantes disponibles'*. La razón es que el servidor **no confía en el estado del cliente**. Como explicó Carlos, dentro de la transacción se ejecuta un **`SELECT … FOR UPDATE`**, que bloquea la fila de la sección y **revalida** el cupo con el dato real de la base. Así se serializa el acceso a la última vacante y es imposible el sobrecupo."

> "Esto mismo lo verificamos de forma automatizada: la prueba `ultimoCupoTieneUnSoloGanador` lanza **cinco hilos sincronizados con `CountDownLatch`** contra la misma vacante, y siempre hay exactamente un ganador, tanto en H2 como en PostgreSQL."

### 3.3 Contrato del retiro (Design by Contract)

**[Vuelve a Ana, entra a Mis matrículas y retira Construcción de software]**

> "Ahora Ana se retira. Fíjense en la **postcondición**: la matrícula desaparece y el cupo vuelve **exactamente a 1 de 1**, ni más ni menos. Y se mantiene el **invariante**: el cupo disponible siempre está entre **0 y el cupo máximo**. Ese invariante está validado en el código y además en la base de datos con una restricción **`CHECK`**, como última línea de defensa."

### 3.4 Reglas académicas (Programación defensiva)

**[Cierra sesión e ingresa como Luis, `20230002B`]**

> "Ahora ingreso como Luis Ramos, cuyo límite es de **8 créditos** y que no tiene cursos aprobados. Primero: **Estructuras de datos no aparece**, porque su prerrequisito es Programación I y Luis no la aprobó."

**[Matricula Programación I, sección A]**

> "Se matricula en Programación I, sección A: cuatro créditos, confirmada."

**[Intenta Matemática discreta, sección A]**

> "Ahora Matemática discreta. Rechazada por **cruce de horario**: las dos **prácticas** coinciden el miércoles. Aclaro que la regla bloquea solo cruces **práctica–práctica**, que son de asistencia obligatoria; los cruces con teoría se permiten por diseño."

**[Intenta Programación I, sección B]**

> "Si intenta otra sección del mismo curso, se rechaza: **no puede matricularse dos veces en el mismo curso**."

**[Intenta Construcción de software]**

> "Y si intenta Construcción de software, de cinco créditos, sumaría **nueve**, por encima de su límite de ocho: **créditos excedidos**."

> "Algo importante: **cada rechazo hace rollback** de la transacción. Ningún intento fallido consumió un cupo ni dejó datos inconsistentes."

### 3.5 Entradas no confiables y manejo de errores

**[En la barra de direcciones: `localhost:8080/oferta?estudiante=1`]**

> "Ahora intento manipular la URL para ver los datos de otro estudiante, pasando `estudiante=1`. El sistema lo **ignora** y sigue mostrando a Luis, porque la identidad del estudiante sale de la **sesión autenticada**, no de un parámetro que el usuario pueda alterar."

**[En la barra de direcciones: `localhost:8080/cualquiercosa`]**

> "Y si accedo a una ruta inexistente, el sistema responde con un **404 controlado** y un mensaje claro. Ninguna excepción llega al usuario como **stack trace**: todas pasan por un **manejador global de errores** que las traduce a una respuesta adecuada y las registra en el log."

### 3.6 Tolerancia a fallas: servicio externo caído

**[Detén la app; en la terminal preparada presiona Enter]**

> "Por último, simulo que el **servicio externo de verificación académica** está caído. Reinicio la aplicación en modo de falla."

**[Inicia sesión e intenta matricular cualquier curso]**

> "Al intentar matricular, el sistema responde con un **503, Servicio no disponible**, y un mensaje claro para el estudiante. Por detrás actuaron los mecanismos de **Resilience4j**: un **timeout**, **reintentos acotados** y, tras fallas repetidas, el **circuit breaker** se abre y deja de llamar al servicio caído."

**[Muestra la consola]**

> "Aquí en el log se ven los eventos `verificacion_academica_fallida` con su causa. Además, como la verificación ocurre **antes** de abrir la transacción, una falla externa **nunca consume cupos ni retiene bloqueos** en la base de datos."

### 3.7 Cierre (~30 s)

> "En resumen, la versión v1.0 garantiza la consistencia del cupo bajo concurrencia, valida las reglas académicas de forma transaccional y se degrada de forma controlada ante fallas externas, todo respaldado por pruebas automatizadas sobre H2 y PostgreSQL."

> "Como limitaciones, no realizamos pruebas de carga sostenida ni medición de latencias, y la verificación académica es un servicio simulado. Para la versión 2.0 planteamos una máquina de estados de la matrícula con lista de espera, reglas configurables por institución y pruebas de rendimiento bajo carga. Muchas gracias, quedamos atentos a sus preguntas."

---

## Glosario para preguntas

| Término | Respuesta |
|---|---|
| **Condición de carrera** | Dos operaciones concurrentes leen el mismo estado y el resultado depende del orden en que se ejecutan. |
| **`SELECT … FOR UPDATE`** | Bloqueo pesimista a nivel de fila: la segunda transacción espera hasta que la primera hace commit o rollback. |
| **¿Por qué pesimista y no optimista?** | En la apertura hay **alta contención** sobre la misma fila. Con bloqueo optimista habría muchos conflictos y reintentos; el pesimista los ordena en una cola. |
| **Invariante** | Condición que siempre se cumple: `0 ≤ cupo_disponible ≤ cupo_maximo`. |
| **Circuit breaker** | Tras 4 llamadas con al menos 50 % de fallos, se abre durante 5 s y rechaza de inmediato sin llamar al servicio. |
| **Rollback** | Si cualquier paso de la transacción falla, se revierten todos sus cambios juntos. |
| **¿Y si se cruzan teorías?** | Está permitido a propósito: solo se bloquean cruces práctica–práctica. Ejemplo con María: Estructuras de datos A (teoría martes 10–12) y Bases de datos A (práctica martes 10–12). |

## Si algo sale mal

- Detener y volver a arrancar la app: los datos vuelven al estado inicial.
- Si la interfaz se ve sin estilos: recarga forzada con `Ctrl + Shift + R`.
- Respaldo: capturas en [`evidencias/`](evidencias/).
