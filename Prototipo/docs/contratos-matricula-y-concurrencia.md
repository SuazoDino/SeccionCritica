# Contratos de matrícula y evidencia de concurrencia

## Alcance

Este documento describe el contrato observable de `MatriculaService` sin cambiar los contratos HTTP ni las reglas académicas existentes. La fuente autoritativa del estado es la base de datos; la pantalla solo muestra una vista informativa.

## Contratos de `matricular(estudianteId, seccionId)`

### Precondiciones de entrada

- Los identificadores son valores enteros que deben corresponder a un estudiante y una sección existentes. El servicio rechaza identificadores inexistentes sin modificar datos.
- La sección pertenece a un curso existente y sus datos persistidos cumplen las restricciones del esquema.

### Reglas de negocio

Dentro de una sola transacción, el servicio comprueba, en este orden lógico: curso ya aprobado, matrícula duplicada en el mismo curso, cupo disponible, prerrequisito aprobado, máximo de créditos y cruces práctica–práctica. Las teorías coincidentes y horarios contiguos siguen siendo permitidos por la regla vigente.

### Postcondiciones de éxito

- Existe exactamente una matrícula para el par estudiante–sección.
- El cupo disponible de la sección disminuye en uno.
- Los créditos y la agenda del estudiante incluyen el curso.

### Postcondiciones de rechazo

La excepción de dominio se propaga y la transacción se revierte: no se inserta matrícula, no se consumen cupos y no se alteran los créditos derivados ni la agenda.

## Contratos de `retirar(estudianteId, seccionId)`

### Precondiciones

- Existen el estudiante y la sección.
- Hay una matrícula activa para el par indicado.

### Postcondiciones de éxito

- La matrícula activa se elimina.
- El cupo disponible aumenta en uno y no supera el cupo máximo.
- Los créditos y la agenda derivados dejan de incluir el curso.

### Postcondiciones de rechazo

Si no hay matrícula activa, se rechaza la operación y el cupo permanece sin cambios.

## Invariantes persistentes

La base de datos mantiene durante y después de cada transacción:

- `0 <= cupo_disponible <= cupo_maximo`.
- Una sola matrícula por estudiante y sección, mediante la clave primaria compuesta.
- Las matrículas siempre apuntan a estudiantes y secciones existentes, mediante claves foráneas.
- El número de matrículas confirmadas de una sección no excede su cupo consumido (`cupo_maximo - cupo_disponible`).
- Las matrículas confirmadas de un estudiante no violan créditos ni cruces práctica–práctica, porque las escrituras del mismo estudiante se serializan.

Las precondiciones y reglas de negocio se expresan con validaciones y excepciones de dominio; no se usa `assert` de Java, que puede deshabilitarse en producción. Los invariantes persistentes se respaldan en SQL y en las pruebas de integración.

## Separación Command–Query

`matricular` y `retirar` son Commands: modifican matrícula y cupo y no devuelven una representación de consulta. `estudiante`, `estudiantes`, `oferta`, `inscripciones`, `agenda` y `creditos` son Queries: leen estado y no deben producir efectos laterales. Mantener esta separación hace visible qué métodos requieren transacción de escritura y qué resultados pueden usarse para verificar postcondiciones.

## Concurrencia

Las escrituras bloquean primero la fila del estudiante y luego la de la sección usando `SELECT ... FOR UPDATE`. El orden fijo evita interbloqueos entre operaciones que afectan a ambos agregados; el bloqueo de estudiante serializa créditos y horarios, y el de sección serializa el consumo de cupos. No se agregan consultas de comprobación fuera de ese flujo, porque una lectura adicional no sustituye al bloqueo y podría ampliar la ventana de carrera.

La prueba `ultimoCupoTieneUnSoloGanador` coordina cinco estudiantes con `CountDownLatch`, sobre una sección con una vacante, y comprueba un ganador, cuatro rechazos, cero cupos, una matrícula y la igualdad entre cupos consumidos y matrículas. Las pruebas de la misma clase conservan las carreras de horario y créditos. Cada llamada entra por separado al proxy transaccional de Spring; la carrera no comparte una transacción entre hilos.

## Verificación realizada

Ejecutado en H2 con Java 21.0.12 y Spring Boot 4.1.0 después de ampliar la carrera:

```text
Tests run: 33, Failures: 0, Errors: 0, Skipped: 12
BUILD SUCCESS
```

La carrera H2 se repitió cinco veces sin fallos durante esta auditoría. PostgreSQL se ejecuta mediante Testcontainers y se omite automáticamente cuando Docker no está disponible; en la ejecución documentada se omitieron 12 casos. Esa omisión no constituye evidencia de ejecución contra PostgreSQL.

## Tres contratos verificables

1. **Matrícula:** una confirmación crea una sola matrícula y consume exactamente un cupo; cualquier rechazo deja ambos estados intactos.
2. **Retiro:** un retiro confirmado elimina la matrícula y libera exactamente un cupo; un retiro repetido no cambia el estado.
3. **Consulta e invariantes:** las consultas de créditos, agenda y matrículas no tienen efectos laterales y la base conserva cupos válidos, unicidad y claves foráneas.

Estos contratos se cubren en `ReglasMatriculaTestBase` con estados antes/después y no dependen de la vista HTML.

## Limitaciones

- La prueba valida consistencia al finalizar cada carrera, no es una prueba de carga ni mide latencias.
- H2 usa compatibilidad PostgreSQL, pero no sustituye por completo al motor PostgreSQL.
- La clase PostgreSQL queda pendiente cuando Docker no está disponible.
