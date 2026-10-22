# Evidencia de pruebas v1.0

## Evidencia vigente — ejecución real del 22 de octubre de 2026

La última ejecución real se realizó desde `C:\SeccionCritica\Prototipo` con el siguiente comando:

```powershell
.\mvnw.cmd clean verify
```

Resultado comprobado:

```text
PaginasTest: 7/7
MatriculaServiceH2Test: 12/12
MatriculaServicePostgresTest: 12/12
VerificacionAcademicaServiceTest: 2/2
Total: 33 pruebas
Fallos: 0
Errores: 0
Omitidas: 0
BUILD SUCCESS
```

PostgreSQL 17 se ejecutó mediante Testcontainers. La compilación limpia y el empaquetado terminaron satisfactoriamente. La duración total comprobada fue de 22.515 segundos.

Esta ejecución sustituye como evidencia vigente a la línea base anterior. Los XML de Surefire quedan en `Prototipo/target/surefire-reports`.

## Historial de auditoría anterior

En la ejecución anterior, las 12 pruebas de `MatriculaServicePostgresTest` fueron omitidas porque Testcontainers no pudo acceder al socket `\\.\pipe\docker_engine`; por tanto, PostgreSQL no estaba verificado en esa línea base. También se documentó que el comando `clean verify` no había completado porque Maven no pudo limpiar `Prototipo/target` bajo OneDrive. Ese estado histórico queda conservado aquí y ya no describe la ejecución vigente del 22 de octubre de 2026.

## Auditoría OWASP

Se inició `mvn org.owasp:dependency-check-maven:check`. Dependency-Check informó 413,320 registros NVD y progreso de 10,000 registros sin API key; la actualización se detuvo por su duración. No se obtuvo un informe concluyente ni se declara ausencia de vulnerabilidades. La auditoría OWASP permanece inconclusa.
