# Sección Crítica · Prototipo local

Motor de matrícula para el período de demostración 2026-II. Java 21, Spring Boot 4.1.0, Thymeleaf y Spring JDBC. Interfaz responsive con CSS propio, sin compilación de frontend.

## Ejecutar ahora

Requisitos: JDK 21 y Maven 3.9+.

```powershell
cd seccion-critica
mvn spring-boot:run
```

Abre http://localhost:8080. El perfil predeterminado `demo` usa H2 en memoria con compatibilidad PostgreSQL. Los datos se reinician al detener la aplicación. Selecciona Ana, Luis o María para recorrer oferta y matrículas.

## PostgreSQL

Inicia Docker Desktop y define una contraseña local antes de levantar la base:

```powershell
$env:DB_PASSWORD='elige-una-clave-local'
docker compose up -d db
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

También se admiten `DB_URL`, `DB_USER` y `PORT`. El esquema se crea si no existe, y los datos iniciales se insertan por identificador sin reponer cupos consumidos. El volumen de Docker conserva la información. Para cambios futuros del esquema se deberá incorporar una herramienta de migraciones.

## Demostración

1. Ana: matricular Construcción de software (una sola vacante).
2. Cambiar a Luis e intentar el mismo curso: rechazo por cupo.
3. Ana: retirar el curso; Luis puede matricularlo después.
4. Luis: Estructuras de datos exige Programación I aprobada.
5. Luis: matricular Programación I A y luego Matemática discreta: cruce de horario.
6. Luis: con Programación I (4 créditos), Construcción de software (5) excede su máximo de 8, si el cupo está libre.
7. Programación I B tampoco puede matricularse si ya se tiene la sección A.

## Pruebas

```powershell
mvn test
```

Pruebas de integración del servicio contra H2: matrícula y retiro, retiro repetido, reglas, identificadores inexistentes y carreras con hilos coordinados para último cupo, horarios y créditos por estudiante. Se ejecutan transacciones reales; las pruebas no envuelven las carreras en una transacción compartida. Falta validar esas mismas garantías sobre PostgreSQL real antes de usarlo fuera de la demo.

## Decisiones

- JDBC permite ver explícitamente los bloqueos `SELECT FOR UPDATE`.
- Todas las escrituras bloquean primero estudiante y luego sección, dentro de una transacción. Esto serializa reglas por estudiante y consumo de cupos por sección.
- La base aplica claves foráneas, unicidad de matrícula y límites de cupos.
- El retiro elimina la matrícula activa; no se conserva historial de retiros en este prototipo.
- Se modela un horario por sección y un solo período. No hay lista de espera, integración externa ni autenticación.
- Los formularios usan POST. La selección de estudiante es solo para demostración: no hay autorización ni protección CSRF. Ejecutar localmente; antes de exponerlo se requiere Spring Security, identidad y autorización por estudiante.
- No se han realizado commits ni cambios remotos. `sources/` permanece intacto.

## Estructura

`src/main/java`: aplicación, servicio transaccional y controlador web.
`src/main/resources`: perfiles, esquema, datos, plantillas y estilos.
`src/test/java`: pruebas de reglas y concurrencia.

## Pendientes

Validación en PostgreSQL, migraciones versionadas, autenticación/CSRF, historial con estados, reglas intercambiables, servicio externo resiliente y métricas de carga. La licencia Apache 2.0 declarada en el repositorio original debe formalizarse por sus autores.
