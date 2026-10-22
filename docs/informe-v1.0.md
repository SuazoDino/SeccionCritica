# Informe de preparación v1.0

Este informe reúne la evidencia de la segunda etapa del prototipo de Sección Crítica. Los resultados se separan entre ejecuciones realizadas y aspectos pendientes; no se declara una auditoría o una prueba como aprobada sin salida verificable.

## Cambios implementados

- Se mantuvieron las transacciones de escritura y el orden de bloqueo estudiante → sección.
- Se completó la carrera reproducible de cinco estudiantes sobre una vacante y se verificó en H2 y PostgreSQL 17 mediante Testcontainers.
- Se agregó una prueba de rollback ante una falla de persistencia en el retiro: la eliminación de la matrícula y la liberación del cupo se revierten juntas.
- Se separaron los timeouts de la prueba de reintentos del verificador académico para evitar que una política de espera válida se confunda con un timeout demasiado corto.
- Se ampliaron las pruebas HTTP para IDs negativos, parámetros ausentes y retiro con sección inexistente.
- Se corrigió el cálculo de la ruta Maven local de `mvnw.cmd` cuando PowerShell no define correctamente `$HOME`.
- Se documentaron errores, logging, runtime, resiliencia, dependencias y trazabilidad.

## Contratos verificados

### `matricular(estudianteId, seccionId)`

Las precondiciones técnicas son IDs existentes y datos persistidos válidos. Las reglas de negocio son curso ya aprobado, matrícula duplicada, cupo, prerrequisito, créditos y cruce práctica–práctica. En éxito se inserta una matrícula y se consume un cupo; en rechazo no cambian matrículas ni cupos. La evidencia está en `matriculaYRetiroSonConsistentes`, `rechazaReglasSinConsumirCupo`, `contratosRechazadosNoCambianMatriculasNiCupos` y `ultimoCupoTieneUnSoloGanador`.

### `retirar(estudianteId, seccionId)`

Las precondiciones son estudiante, sección y matrícula activa existentes. En éxito se elimina la matrícula y se libera exactamente un cupo. Un retiro repetido se rechaza sin modificar el estado. `retiroHaceRollbackSiFallaLaLiberacionDelCupo` fuerza un `CHECK` de base de datos después del `DELETE` y comprueba que la matrícula reaparece tras el rollback.

### `oferta`, agenda y créditos

Son consultas sin efectos laterales. La oferta filtra cursos aprobados y prerrequisitos; agenda y créditos reflejan las matrículas confirmadas. La prueba `ofertaFiltraPrerrequisitosYAprobados` y las aserciones de créditos y agenda cubren el comportamiento observable.

## Problemas reales de runtime

### A. Carrera por la última vacante

Comparten estado la fila de `seccion`, su `cupo_disponible` y las filas de `matricula`. Sin bloqueo, dos transacciones pueden leer una vacante, insertar ambas matrículas y dejar un sobrecupo. `MatriculaCommandService` abre una transacción, bloquea primero al estudiante y después la sección con `SELECT FOR UPDATE`; la segunda transacción espera y vuelve a observar la sección sin vacantes. La prueba de cinco estudiantes verifica un ganador, cuatro rechazos, cero vacantes, una matrícula y cupos consumidos iguales a matrículas en ambas bases.

La garantía depende de que todas las escrituras pasen por este servicio y de que la base respete las restricciones. La consulta no es una prueba de carga, no cubre pérdida total de la base y la sección sigue siendo un punto de contención.

### B. Contención y espera por bloqueos

Varias transacciones que compiten por una sección se serializan en la fila bloqueada. El orden estudiante → sección evita que una operación bloquee la sección y otra el estudiante en orden inverso; así se reduce el riesgo de interbloqueo cuando se evalúan créditos y horarios. PostgreSQL usa `lock_timeout=5s`; la transacción tiene timeout de 10 s y el pool espera hasta 5 s por conexión. Al vencer la espera, la operación falla y Spring revierte la transacción; el controlador muestra un mensaje controlado para errores transitorios. El costo es menor paralelismo sobre una misma sección y más latencia bajo alta demanda.

## Resiliencia y errores

El primer mecanismo es el timeout: Hikari limita la obtención de conexión, PostgreSQL limita la espera por bloqueo, Spring limita la transacción y `VerificacionAcademicaService` limita la frontera externa. `VerificacionAcademicaServiceTest.timeoutSeConvierteEnRechazoControlado` demuestra que una demora externa produce `ServicioExternoNoDisponibleException`; la interfaz responde con 503 mediante `ManejadorErrores` sin stack trace.

El segundo mecanismo es el rollback transaccional. Matrícula y retiro se ejecutan en una sola transacción. Las reglas rechazadas se comprueban sin escritura y la prueba de inyección controlada demuestra que una falla posterior al `DELETE` restaura la matrícula y conserva el cupo.

La política de logging usa INFO para confirmaciones y configuración de resiliencia, WARN para rechazos, recursos inexistentes, timeouts y contención, y ERROR para fallos de infraestructura o errores inesperados. Los logs incluyen IDs de dominio y causa funcional; no incluyen credenciales ni trazas en la respuesta HTTP. Solo el manejador global registra la traza técnica de infraestructura.

## Evidencia de pruebas

La evidencia vigente corresponde a la ejecución real del 22 de octubre de 2026, realizada desde `C:\SeccionCritica\Prototipo` con `.\mvnw.cmd clean verify`. Terminó en `BUILD SUCCESS`, con compilación limpia, empaquetado satisfactorio y una duración de 22.515 segundos.

El desglose comprobado fue `PaginasTest` 7/7, `MatriculaServiceH2Test` 12/12, `MatriculaServicePostgresTest` 12/12 y `VerificacionAcademicaServiceTest` 2/2. El total fue de 33 pruebas, con 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17 se ejecutó mediante Testcontainers; por ello, la carrera de cinco competidores y la cobertura de contratos se verificaron en H2 y PostgreSQL.

La auditoría anterior queda conservada como historial: en ella se omitieron las 12 pruebas PostgreSQL por falta de acceso al socket de Docker y `clean verify` no completó por la limpieza de `target` bajo OneDrive. Ese resultado ya no es el vigente.

## OWASP y dependencias

El `pom.xml` declara Spring Boot 4.1.0, Java 21, JDBC, Thymeleaf, Actuator, H2, PostgreSQL, JUnit, Testcontainers y las librerías Resilience4j ya incorporadas. El plugin OWASP Dependency-Check 12.1.0 está configurado bajo demanda con reportes HTML y JSON y umbral CVSS 7.0.

La auditoría se inició con `mvn org.owasp:dependency-check-maven:check` y el plugin informó 413,320 registros NVD. Sin API key, la actualización alcanzó 10,000 registros y fue detenida por su duración. No se obtuvo un resultado de vulnerabilidades ni ausencia de ellas. Debe repetirse con una API key o con tiempo suficiente para generar `Prototipo/target/dependency-check-report.html` y `.json`.

## Demostración del examen

1. Ejecutar `cd Prototipo; .\\mvnw.cmd test` y abrir `http://localhost:8080`.
2. Matricular a Ana en la sección 4 y mostrar la vacante consumida.
3. Intentar la misma sección con Luis y mostrar el rechazo por falta de vacantes.
4. Retirar con Ana y mostrar que el cupo se libera.
5. Ejecutar la prueba de cinco competidores y mostrar que solo uno gana.
6. Probar un enlace sin parámetros, un ID no numérico, un ID negativo y un recurso inexistente.
7. Configurar `VERIFICACION_ACADEMICA_MODO=fallo` o una latencia mayor que `VERIFICACION_ACADEMICA_TIMEOUT` y mostrar la respuesta controlada.
8. Explicar el caso de rollback de retiro y enseñar sus aserciones de estado final.

## Matriz de trazabilidad

| Requisito | Implementación | Prueba | Evidencia | Estado |
|---|---|---|---|---|
| Runtime: última vacante (10%) | `SELECT FOR UPDATE`, transacción y orden de bloqueo | `ultimoCupoTieneUnSoloGanador` H2 y PostgreSQL | Salida Maven de H2 y PostgreSQL 17 con Testcontainers | Cumplido |
| Contratos (15%) | Commands, reglas e invariantes SQL | Reglas de `ReglasMatriculaTestBase` | 12 pruebas por motor | Cumplido |
| Programación defensiva (10%) | Advice MVC y validación por existencia | `PaginasTest` | IDs negativos, ausentes, mal formados e inexistentes | Cumplido |
| Errores y excepciones (15%) | Jerarquía, `ManejadorErrores`, SLF4J | `PaginasTest` y pruebas de servicio | HTTP controlado y logs observables | Cumplido |
| Resiliencia (15%) | Timeouts, rollback y frontera externa | `VerificacionAcademicaServiceTest`, rollback | Pruebas externas y rollback en H2 y PostgreSQL 17 con Testcontainers | Cumplido |
| Concurrencia (15%) | Barreras, transacciones independientes y locks | `ultimoCupoTieneUnSoloGanador` | H2 y PostgreSQL 17, 5 solicitudes por motor | Cumplido |
| Dependencias (10%) | Maven tree y OWASP bajo demanda | Comandos de auditoría | No hay informe NVD concluyente | Parcial |
| Pruebas/evidencias (5%) | Suite H2/PostgreSQL y pruebas focales | `clean verify` | 33 ejecutadas, 0 fallos, 0 errores y 0 omitidas; PostgreSQL 17 con Testcontainers | Cumplido |
| Documentación/entrega (5%) | Este informe, READMEs y docs técnicas | Revisión de archivos | Matriz y guion de exposición | Cumplido |

## Pendientes

- Ejecutar Dependency-Check y conservar `target/dependency-check-report.html` y `.json` si NVD permite actualizar.
- Si la auditoría no puede actualizar NVD, documentar el bloqueo exacto.
- No crear aún la etiqueta Git `v1.0`, commits, ramas ni releases.
