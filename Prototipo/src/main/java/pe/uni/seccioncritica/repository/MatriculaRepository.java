package pe.uni.seccioncritica.repository;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.uni.seccioncritica.model.EventoAgenda;
import pe.uni.seccioncritica.model.Inscripcion;

@Repository
public class MatriculaRepository {
    private final JdbcClient db;

    public MatriculaRepository(JdbcClient db) { this.db = db; }

    public List<Inscripcion> inscripciones(long estudianteId) {
        return db.sql("""
                SELECT s.id AS seccion_id, s.nombre AS seccion, c.codigo, c.nombre AS curso, c.creditos
                FROM matricula m
                JOIN seccion s ON s.id = m.seccion_id
                JOIN curso c ON c.id = s.curso_id
                WHERE m.estudiante_id = ?
                ORDER BY c.codigo""")
                .param(estudianteId).query(Inscripcion.class).list();
    }

    public List<EventoAgenda> agenda(long estudianteId) {
        return db.sql("""
                SELECT h.id, h.tipo, h.dia, h.inicio, h.fin,
                       c.id AS curso_id, c.codigo, c.nombre AS curso, s.nombre AS seccion
                FROM matricula m
                JOIN seccion s ON s.id = m.seccion_id
                JOIN curso c ON c.id = s.curso_id
                JOIN sesion h ON h.seccion_id = s.id
                WHERE m.estudiante_id = ?
                ORDER BY h.dia, h.inicio""")
                .param(estudianteId).query(EventoAgenda.class).list();
    }

    public int creditos(long estudianteId) {
        return db.sql("""
                SELECT COALESCE(SUM(c.creditos), 0)
                FROM matricula m
                JOIN seccion s ON s.id = m.seccion_id
                JOIN curso c ON c.id = s.curso_id
                WHERE m.estudiante_id = ?""")
                .param(estudianteId).query(Integer.class).single();
    }

    public boolean matriculadoEnCurso(long estudianteId, long cursoId) {
        return db.sql("""
                SELECT COUNT(*) FROM matricula m JOIN seccion s ON s.id = m.seccion_id
                WHERE m.estudiante_id = ? AND s.curso_id = ?""")
                .params(estudianteId, cursoId).query(Integer.class).single() > 0;
    }

    public void registrar(long estudianteId, long seccionId) {
        db.sql("INSERT INTO matricula(estudiante_id, seccion_id) VALUES (?, ?)").params(estudianteId, seccionId).update();
    }

    /** @return {@code true} si existía una matrícula activa y se eliminó. */
    public boolean eliminar(long estudianteId, long seccionId) {
        return db.sql("DELETE FROM matricula WHERE estudiante_id = ? AND seccion_id = ?")
                .params(estudianteId, seccionId).update() > 0;
    }
}
