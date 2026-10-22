# Guía de demostración y exposición v1.0

## 1. Preparar el entorno

Desde PowerShell:

```powershell
Set-Location C:\SeccionCritica\Prototipo
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

Resultado esperado: `BUILD SUCCESS`, 33 pruebas, 0 fallos, 0 errores y 0 omitidas. La ejecución vigente usa PostgreSQL 17 mediante Testcontainers; requiere Docker operativo.
Evidencia: captura de la consola con el comando y el resumen de pruebas.  

## 2. Ejecutar el prototipo

**Objetivo:** mostrar la aplicación real con H2 en memoria.

```powershell
Set-Location C:\SeccionCritica\Prototipo
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8080` y, en otra ventana de PowerShell, comprobar:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health | Select-Object -ExpandProperty Content
```

Resultado esperado: pantalla de selección de estudiantes y health check con estado `UP`.  
Evidencia: aplicación funcionando y respuesta del health check.  


## 3. Matrícula exitosa y rechazo por cupo

Los datos reales de [`data.sql`](../Prototipo/src/main/resources/data.sql) son Ana Torres (ID 1), Luis Ramos (ID 2), María Flores (ID 3). La sección 4 es `Construcción de software`, sección A, con una vacante.

**Matrícula exitosa**

1. Seleccionar Ana Torres.
2. En la oferta, ubicar `SW301 Construcción de software`, sección A.
3. Pulsar “Matricularme”.

Resultado esperado: mensaje “Matrícula confirmada. Tu vacante está reservada.” y la sección queda sin vacantes.  
Evidencia: mensaje y contador de cupo.

**Rechazo por falta de cupo**

1. Volver al inicio y seleccionar Luis Ramos.
2. Intentar la misma sección A de `Construcción de software`.

Resultado esperado: rechazo por falta de vacantes, sin matrícula confirmada para Luis.  
Evidencia: diálogo de rechazo y sección agotada.  

## 4. Retiro y liberación

1. Volver a Ana Torres.
2. Abrir “Mis matrículas”.
3. Pulsar “Retirarme” en `Construcción de software`, sección A, y confirmar.
4. Volver a Luis Ramos y actualizar la oferta.
5. Intentar nuevamente la misma sección.

Resultado esperado: el retiro muestra “La vacante vuelve a estar disponible” y Luis puede ocupar la vacante si no infringe otra regla.  
Evidencia: agenda de Ana antes/después y cupo liberado.  

## 5. Reglas académicas

Usar los datos reales y observar los resultados en la interfaz:

- Luis no tiene aprobado `Programación I`; `Estructuras de datos` exige ese prerrequisito y no aparece en su oferta.
- Ana y María tienen aprobado `Programación I`; el curso no aparece como oferta para ellas.
- Luis puede matricular `Programación I` sección A (ID 1) y luego intentar `Matemática discreta` sección A (ID 3): sus prácticas se cruzan el miércoles, por lo que la segunda operación se rechaza.
- Con Luis matriculado en `Programación I` (4 créditos), intentar `Construcción de software` (5 créditos): 9 supera su máximo de 8 y se rechaza, siempre que la sección esté libre.

Las aserciones exactas están en [`ReglasMatriculaTestBase`](../Prototipo/src/test/java/pe/uni/seccioncritica/service/ReglasMatriculaTestBase.java).  
Evidencia: oferta filtrada, mensaje de prerrequisito/créditos y diálogo de cruce horario.  

## 6. Explicar el control de concurrencia

Mostrar [`MatriculaCommandService`](../Prototipo/src/main/java/pe/uni/seccioncritica/service/MatriculaCommandService.java) y las consultas `FOR UPDATE` de los repositorios. Explicar:

1. cinco solicitudes compiten por la sección 4, que tiene una vacante;
2. cada transacción bloquea primero al estudiante y luego a la sección;
3. el primer commit consume la vacante;
4. las siguientes transacciones observan la sección agotada;
5. el estado final es un ganador, cuatro rechazos, una matrícula y cero vacantes.

Evidencia: prueba `ultimoCupoTieneUnSoloGanador` en los reportes de prueba.  

## 7. Ejecutar pruebas y mostrar PostgreSQL

```powershell
Set-Location C:\SeccionCritica\Prototipo
.\mvnw.cmd clean verify
Get-ChildItem .\target\surefire-reports\*.txt | Get-Content
```

Resultado esperado: 33 pruebas y `BUILD SUCCESS`. En la salida o en Docker Desktop debe observarse el contenedor `postgres:17` durante `MatriculaServicePostgresTest`.  
Evidencia: consola Maven y contenedor PostgreSQL.  

## 8. Manejo de errores y resiliencia

Para una demostración controlada de la falla externa, detener la aplicación (`Ctrl+C`) y ejecutar:

```powershell
$env:DEMO_VERIFICACION_ACADEMICA_MODO='fallo'
Set-Location C:\SeccionCritica\Prototipo
.\mvnw.cmd spring-boot:run
```

La propiedad del perfil usa el nombre `demo.verificacion-academica.modo`; si la variable de entorno no es reconocida por la configuración activa, explicar el mecanismo con la prueba automatizada y no presentar una pantalla no verificada como resultado. La evidencia automatizada comprobada es `VerificacionAcademicaServiceTest`: reintento de tres intentos y timeout convertido en rechazo controlado.

Para mostrar entradas inválidas, abrir `/oferta` sin `estudiante`, `/oferta?estudiante=abc` y `/no-existe`.  
Resultado esperado: página amigable, código HTTP controlado y sin stack trace en la respuesta.  

## 9. Cierre

Detener la aplicación con `Ctrl+C`.
