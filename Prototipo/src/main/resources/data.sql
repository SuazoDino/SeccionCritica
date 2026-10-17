-- Datos de demostración. Cada INSERT es idempotente: en PostgreSQL no repone cupos ya consumidos.

INSERT INTO estudiante (id, nombre, codigo, max_creditos) SELECT 1, 'Ana Torres', '20230001A', 12 WHERE NOT EXISTS (SELECT 1 FROM estudiante WHERE id = 1);
INSERT INTO estudiante (id, nombre, codigo, max_creditos) SELECT 2, 'Luis Ramos', '20230002B', 8 WHERE NOT EXISTS (SELECT 1 FROM estudiante WHERE id = 2);
INSERT INTO estudiante (id, nombre, codigo, max_creditos) SELECT 3, 'María Flores', '20230003C', 20 WHERE NOT EXISTS (SELECT 1 FROM estudiante WHERE id = 3);

INSERT INTO curso (id, codigo, nombre, creditos, prerrequisito) SELECT 1, 'CC101', 'Programación I', 4, CAST(NULL AS BIGINT) WHERE NOT EXISTS (SELECT 1 FROM curso WHERE id = 1);
INSERT INTO curso (id, codigo, nombre, creditos, prerrequisito) SELECT 2, 'CC201', 'Estructuras de datos', 4, 1 WHERE NOT EXISTS (SELECT 1 FROM curso WHERE id = 2);
INSERT INTO curso (id, codigo, nombre, creditos, prerrequisito) SELECT 3, 'MA201', 'Matemática discreta', 4, CAST(NULL AS BIGINT) WHERE NOT EXISTS (SELECT 1 FROM curso WHERE id = 3);
INSERT INTO curso (id, codigo, nombre, creditos, prerrequisito) SELECT 4, 'SW301', 'Construcción de software', 5, CAST(NULL AS BIGINT) WHERE NOT EXISTS (SELECT 1 FROM curso WHERE id = 4);
INSERT INTO curso (id, codigo, nombre, creditos, prerrequisito) SELECT 5, 'CC301', 'Bases de datos', 4, CAST(NULL AS BIGINT) WHERE NOT EXISTS (SELECT 1 FROM curso WHERE id = 5);

INSERT INTO aprobado (estudiante_id, curso_id) SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM aprobado WHERE estudiante_id = 1 AND curso_id = 1);
INSERT INTO aprobado (estudiante_id, curso_id) SELECT 3, 1 WHERE NOT EXISTS (SELECT 1 FROM aprobado WHERE estudiante_id = 3 AND curso_id = 1);

INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 1, 1, 'A', 3, 3 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 1);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 2, 2, 'A', 2, 2 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 2);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 3, 3, 'A', 4, 4 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 3);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 4, 4, 'A', 1, 1 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 4);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 5, 5, 'A', 3, 3 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 5);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 6, 1, 'B', 2, 2 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 6);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 7, 2, 'B', 3, 3 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 7);
INSERT INTO seccion (id, curso_id, nombre, cupo_maximo, cupo_disponible) SELECT 8, 5, 'B', 2, 2 WHERE NOT EXISTS (SELECT 1 FROM seccion WHERE id = 8);

-- Sesiones: id = seccion*10 para teoría y seccion*10 + 1 para práctica.
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 10, 1, 'TEORIA', 1, 480, 600 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 10);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 11, 1, 'PRACTICA', 3, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 11);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 20, 2, 'TEORIA', 2, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 20);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 21, 2, 'PRACTICA', 5, 840, 960 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 21);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 30, 3, 'TEORIA', 1, 540, 660 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 30);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 31, 3, 'PRACTICA', 3, 660, 780 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 31);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 40, 4, 'TEORIA', 3, 840, 960 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 40);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 41, 4, 'PRACTICA', 5, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 41);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 50, 5, 'TEORIA', 4, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 50);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 51, 5, 'PRACTICA', 2, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 51);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 60, 6, 'TEORIA', 5, 480, 600 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 60);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 61, 6, 'PRACTICA', 6, 480, 600 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 61);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 70, 7, 'TEORIA', 4, 840, 960 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 70);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 71, 7, 'PRACTICA', 6, 840, 960 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 71);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 80, 8, 'TEORIA', 6, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 80);
INSERT INTO sesion (id, seccion_id, tipo, dia, inicio, fin) SELECT 81, 8, 'PRACTICA', 7, 600, 720 WHERE NOT EXISTS (SELECT 1 FROM sesion WHERE id = 81);
