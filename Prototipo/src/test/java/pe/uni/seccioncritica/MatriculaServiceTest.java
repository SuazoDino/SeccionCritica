package pe.uni.seccioncritica;

import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MatriculaServiceTest {
    @Autowired MatriculaService service;
    @Autowired JdbcTemplate db;
    @BeforeEach void reset() {
        db.update("DELETE FROM matricula");
        db.update("UPDATE seccion SET cupo_disponible=cupo_maximo");
    }
    @Test void matriculaYRetiroSonConsistentes() {
        service.matricular(1,4);
        assertEquals(5,service.creditos(1));
        assertEquals(0,cupos(4));
        service.retirar(1,4);
        assertEquals(0,service.creditos(1));
        assertEquals(1,cupos(4));
        assertThrows(MatriculaService.Rechazo.class,()->service.retirar(1,4));
        assertEquals(1,cupos(4));
    }
    @Test void rechazaReglasSinConsumirCupo() {
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(2,2));
        assertEquals(2,cupos(2));
        service.matricular(2,1);
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(2,3));
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(2,6));
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(2,4));
        assertEquals(4,service.creditos(2));
        assertEquals(1,cupos(4));
    }
    @Test void ultimoCupoTieneUnSoloGanador() throws Exception {
        assertEquals(1,competir(new long[][]{{1,4},{2,4},{3,4}}));
        assertEquals(0,cupos(4));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM matricula WHERE seccion_id=4",Integer.class));
    }
    @Test void solicitudesDelMismoEstudianteRespetanElHorario() throws Exception {
        assertEquals(1,competir(new long[][]{{2,1},{2,3}}));
        assertEquals(4,service.creditos(2));
    }
    @Test void solicitudesDelMismoEstudianteRespetanLosCreditos() throws Exception {
        assertEquals(1,competir(new long[][]{{2,4},{2,5}}));
        assertTrue(service.creditos(2)<=8);
    }
    @Test void inexistentesNoAlteranLosCupos() {
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(999,4));
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(1,999));
        assertEquals(1,cupos(4));
    }
    private int cupos(long id) { return db.queryForObject("SELECT cupo_disponible FROM seccion WHERE id=?",Integer.class,id); }
    @Test void ofertaFiltraPrerrequisitosYAprobados() {
        assertTrue(service.oferta(2).stream().noneMatch(s->s.get("codigo").equals("CC201")));
        assertTrue(service.oferta(1).stream().noneMatch(s->s.get("codigo").equals("CC101")));
        assertEquals(2,service.oferta(1).stream().filter(s->s.get("codigo").equals("CC201")).count());
        assertThrows(MatriculaService.Rechazo.class,()->service.matricular(1,1));
    }
    @Test void crucesConTeoriaSePermiten() {
        // Teoría de CC201 y práctica de CC301 coinciden el martes 10–12.
        service.matricular(3,2);
        service.matricular(3,5);
        assertEquals(8,service.creditos(3));
        assertEquals(4,service.agenda(3).size());
    }
    @Test void teoriaTeoriaSePermiteYHorariosContiguosTambien() {
        db.update("UPDATE sesion SET inicio=720,fin=840 WHERE id=31");
        try {
            service.matricular(2,1);
            service.matricular(2,3);
            assertEquals(8,service.creditos(2));
        } finally { db.update("UPDATE sesion SET inicio=660,fin=780 WHERE id=31"); }
    }
    private int competir(long[][] solicitudes) throws Exception {
        try(var executor=Executors.newFixedThreadPool(solicitudes.length)) {
            var listos=new CountDownLatch(solicitudes.length);
            var salida=new CountDownLatch(1);
            var tareas=IntStream.range(0,solicitudes.length).mapToObj(i->executor.submit(()->{
                listos.countDown();
                if(!salida.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la carrera");
                try { service.matricular(solicitudes[i][0],solicitudes[i][1]); return 1; }
                catch(MatriculaService.Rechazo e) { return 0; }
            })).toList();
            assertTrue(listos.await(10,TimeUnit.SECONDS));
            salida.countDown();
            int exitos=0;
            for(var tarea:tareas) exitos+=tarea.get(15,TimeUnit.SECONDS);
            return exitos;
        }
    }
}
