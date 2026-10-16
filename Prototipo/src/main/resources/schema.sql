CREATE TABLE IF NOT EXISTS estudiante (
 id BIGINT PRIMARY KEY, nombre VARCHAR(120) NOT NULL, codigo VARCHAR(20) UNIQUE NOT NULL,
 max_creditos INT NOT NULL CHECK(max_creditos > 0)
);
CREATE TABLE IF NOT EXISTS curso (
 id BIGINT PRIMARY KEY, codigo VARCHAR(20) UNIQUE NOT NULL, nombre VARCHAR(120) NOT NULL,
 creditos INT NOT NULL CHECK(creditos > 0), prerrequisito BIGINT REFERENCES curso(id)
);
CREATE TABLE IF NOT EXISTS aprobado (
 estudiante_id BIGINT REFERENCES estudiante(id), curso_id BIGINT REFERENCES curso(id),
 PRIMARY KEY(estudiante_id, curso_id)
);
CREATE TABLE IF NOT EXISTS seccion (
 id BIGINT PRIMARY KEY, curso_id BIGINT NOT NULL REFERENCES curso(id), nombre VARCHAR(30) NOT NULL,
 dia INT NOT NULL CHECK(dia BETWEEN 1 AND 7), inicio INT NOT NULL, fin INT NOT NULL,
 cupo_maximo INT NOT NULL CHECK(cupo_maximo > 0), cupo_disponible INT NOT NULL,
 CHECK(fin > inicio), CHECK(cupo_disponible BETWEEN 0 AND cupo_maximo)
);
CREATE TABLE IF NOT EXISTS matricula (
 estudiante_id BIGINT REFERENCES estudiante(id), seccion_id BIGINT REFERENCES seccion(id),
 PRIMARY KEY(estudiante_id, seccion_id)
);
CREATE TABLE IF NOT EXISTS sesion (
 id BIGINT PRIMARY KEY, seccion_id BIGINT NOT NULL REFERENCES seccion(id),
 tipo VARCHAR(10) NOT NULL CHECK(tipo IN ('TEORIA','PRACTICA')),
 dia INT NOT NULL CHECK(dia BETWEEN 1 AND 7), inicio INT NOT NULL CHECK(inicio >= 420),
 fin INT NOT NULL CHECK(fin <= 1320), CHECK(fin > inicio)
);
