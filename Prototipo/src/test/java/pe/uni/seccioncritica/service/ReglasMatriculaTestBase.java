package pe.uni.seccioncritica.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.uni.seccioncritica.exception.CreditosExcedidosException;
import pe.uni.seccioncritica.exception.CruceDeHorarioException;
import pe.uni.seccioncritica.exception.CupoNoDisponibleException;
import pe.uni.seccioncritica.exception.MatriculaRechazadaException;
import pe.uni.seccioncritica.exception.PrerrequisitoNoCumplidoException;
import pe.uni.seccioncritica.exception.RecursoNoEncontradoException;

/**
 * Reglas y carreras de matrícula. Se ejecutan con transacciones reales: las pruebas no envuelven
 * las carreras en una transacción compartida. Cada subclase elige la base de datos.
 */
abstract class ReglasMatriculaTestBase {
    @Autowired MatriculaService service;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reiniciar() {
        db.update("DELETE FROM matricula");
        db.update("UPDATE seccion SET cupo_disponible = cupo_maximo");
        insertarEstudiantesDeCarrera();
    }

    @Test
    void matriculaYRetiroSonConsistentes() {
        service.matricular(1, 4);
        assertEquals(5, service.creditos(1));
        assertEquals(0, cupos(4));
        service.retirar(1, 4);
        assertEquals(0, service.creditos(1));
        assertEquals(1, cupos(4));
        assertThrows(MatriculaRechazadaException.class, () -> service.retirar(1, 4));
        assertEquals(1, cupos(4));
    }

    @Test
    void rechazaReglasSinConsumirCupo() {
        assertThrows(PrerrequisitoNoCumplidoException.class, () -> service.matricular(2, 2));
        assertEquals(2, cupos(2));
        service.matricular(2, 1);
        assertThrows(CruceDeHorarioException.class, () -> service.matricular(2, 3));
        assertThrows(MatriculaRechazadaException.class, () -> service.matricular(2, 6));
        assertThrows(CreditosExcedidosException.class, () -> service.matricular(2, 4));
        assertEquals(4, service.creditos(2));
        assertEquals(1, cupos(4));
    }

    @Test
    void cupoAgotadoSeRechaza() {
        service.matricular(1, 4);
        assertThrows(CupoNoDisponibleException.class, () -> service.matricular(3, 4));
    }

    @Test
    void ultimoCupoTieneUnSoloGanador() throws Exception {
        var resultados = competirConResultados(new long[][]{{1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}});
        assertEquals(1, resultados.stream().filter(Boolean.TRUE::equals).count());
        assertEquals(4, resultados.stream().filter(Boolean.FALSE::equals).count());
        assertEquals(0, cupos(4));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM matricula WHERE seccion_id = 4", Integer.class));
        assertEquals(1, db.queryForObject("SELECT cupo_maximo - cupo_disponible FROM seccion WHERE id = 4", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM matricula WHERE seccion_id = 4", Integer.class));
    }

    @Test
    void contratosRechazadosNoCambianMatriculasNiCupos() {
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM matricula", Integer.class));
        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(-1, 4));
        assertThrows(PrerrequisitoNoCumplidoException.class, () -> service.matricular(2, 2));
        service.matricular(2, 1);
        var matriculas = db.queryForObject("SELECT COUNT(*) FROM matricula", Integer.class);
        var cupoSeccion4 = cupos(4);
        assertThrows(CruceDeHorarioException.class, () -> service.matricular(2, 3));
        assertThrows(MatriculaRechazadaException.class, () -> service.matricular(2, 6));
        assertThrows(CreditosExcedidosException.class, () -> service.matricular(2, 4));
        assertEquals(matriculas, db.queryForObject("SELECT COUNT(*) FROM matricula", Integer.class));
        assertEquals(cupoSeccion4, cupos(4));
    }

    @Test
    void retiroHaceRollbackSiFallaLaLiberacionDelCupo() {
        service.matricular(1, 4);
        // Inyección controlada: la matrícula existe, pero el cupo está en su máximo.
        // El UPDATE +1 viola el CHECK y debe revertir también el DELETE anterior.
        db.update("UPDATE seccion SET cupo_disponible = cupo_maximo WHERE id = 4");

        assertThrows(DataAccessException.class, () -> service.retirar(1, 4));
        assertEquals(1, db.queryForObject(
                "SELECT COUNT(*) FROM matricula WHERE estudiante_id = 1 AND seccion_id = 4", Integer.class));
        assertEquals(1, cupos(4));
    }

    @Test
    void solicitudesDelMismoEstudianteRespetanElHorario() throws Exception {
        assertEquals(1, competir(new long[][]{{2, 1}, {2, 3}}));
        assertEquals(4, service.creditos(2));
    }

    @Test
    void solicitudesDelMismoEstudianteRespetanLosCreditos() throws Exception {
        assertEquals(1, competir(new long[][]{{2, 4}, {2, 5}}));
        assertTrue(service.creditos(2) <= 8);
    }

    @Test
    void inexistentesNoAlteranLosCupos() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(999, 4));
        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(1, 999));
        assertThrows(RecursoNoEncontradoException.class, () -> service.estudiante(999));
        assertThrows(RecursoNoEncontradoException.class, () -> service.matricular(0, 4));
        assertThrows(RecursoNoEncontradoException.class, () -> service.retirar(1, -4));
        assertEquals(1, cupos(4));
    }

    @Test
    void ofertaFiltraPrerrequisitosYAprobados() {
        assertTrue(service.oferta(2).stream().noneMatch(c -> c.curso().codigo().equals("CC201")));
        assertTrue(service.oferta(1).stream().noneMatch(c -> c.curso().codigo().equals("CC101")));
        var estructuras = service.oferta(1).stream().filter(c -> c.curso().codigo().equals("CC201")).findFirst().orElseThrow();
        assertEquals(2, estructuras.secciones().size());
        estructuras.secciones().forEach(s -> assertEquals(2, s.sesiones().size()));
        assertThrows(MatriculaRechazadaException.class, () -> service.matricular(1, 1));
    }

    @Test
    void crucesConTeoriaSePermiten() {
        // Teoría de CC201 y práctica de CC301 coinciden el martes 10–12.
        service.matricular(3, 2);
        service.matricular(3, 5);
        assertEquals(8, service.creditos(3));
        assertEquals(4, service.agenda(3).size());
        assertEquals(2, service.inscripciones(3).size());
    }

    @Test
    void teoriaTeoriaSePermiteYHorariosContiguosTambien() {
        db.update("UPDATE sesion SET inicio = 720, fin = 840 WHERE id = 31");
        try {
            service.matricular(2, 1);
            service.matricular(2, 3);
            assertEquals(8, service.creditos(2));
        } finally {
            db.update("UPDATE sesion SET inicio = 660, fin = 780 WHERE id = 31");
        }
    }

    private int cupos(long seccion) {
        return db.queryForObject("SELECT cupo_disponible FROM seccion WHERE id = ?", Integer.class, seccion);
    }

    private int competir(long[][] solicitudes) throws Exception {
        return (int) competirConResultados(solicitudes).stream().filter(Boolean.TRUE::equals).count();
    }

    private java.util.List<Boolean> competirConResultados(long[][] solicitudes) throws Exception {
        try (var executor = Executors.newFixedThreadPool(solicitudes.length)) {
            var listos = new CountDownLatch(solicitudes.length);
            var salida = new CountDownLatch(1);
            var tareas = IntStream.range(0, solicitudes.length).mapToObj(i -> executor.submit(() -> {
                listos.countDown();
                if (!salida.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la carrera");
                try {
                    service.matricular(solicitudes[i][0], solicitudes[i][1]);
                    return 1;
                } catch (MatriculaRechazadaException e) {
                    return 0;
                }
            })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            salida.countDown();
            return tareas.stream().map(tarea -> {
                try {
                    return tarea.get(15, TimeUnit.SECONDS) == 1;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        }
    }

    private void insertarEstudiantesDeCarrera() {
        for (long id = 4; id <= 5; id++) {
            if (db.queryForObject("SELECT COUNT(*) FROM estudiante WHERE id = ?", Integer.class, id) == 0) {
                db.update("INSERT INTO estudiante(id, nombre, codigo, max_creditos) VALUES (?, ?, ?, ?)",
                        id, "Concurrente " + id, "TEST" + id, 20);
            }
        }
    }
}
