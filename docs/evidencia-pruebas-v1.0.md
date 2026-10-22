# Evidencia de pruebas v1.0

La evidencia vigente corresponde a la ejecución real del 22 de octubre de 2026 desde `C:\SeccionCritica\Prototipo`:

```powershell
.\mvnw.cmd clean verify
```

| Suite | Pruebas |
|---|---:|
| `PaginasTest` | 7 |
| `MatriculaServiceH2Test` | 12 |
| `MatriculaServicePostgresTest` | 12 |
| `VerificacionAcademicaServiceTest` | 2 |
| **Total** | **33** |

Fallos: **0** · Errores: **0** · Omitidas: **0** · Resultado: **BUILD SUCCESS** · Duración: **22.515 segundos**.

`MatriculaServicePostgresTest` ejecutó PostgreSQL 17 mediante Testcontainers. Las pruebas de reglas y concurrencia reutilizan [`ReglasMatriculaTestBase`](../Prototipo/src/test/java/pe/uni/seccioncritica/service/ReglasMatriculaTestBase.java), incluyendo el escenario de cinco competidores por la última vacante.

Los XML y TXT de Surefire están en `Prototipo/target/surefire-reports/`. Son artefactos generados y no deben agregarse al repositorio. La guía para obtener capturas está en [guia-evidencias-v1.0.md](./guia-evidencias-v1.0.md).

## Auditoría OWASP

Dependency-Check 12.1.0 se inició, pero la actualización de NVD quedó inconclusa. NVD informó 413,340 registros; se descargaron 10,000 (aproximadamente 2%), aparecieron reintentos y no se utilizó API key. No se obtuvo un resultado concluyente y no se afirma que existan cero vulnerabilidades.
