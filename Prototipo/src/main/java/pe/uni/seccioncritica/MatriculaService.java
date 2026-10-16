package pe.uni.seccioncritica;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatriculaService {
    private final JdbcTemplate db;
    public MatriculaService(JdbcTemplate db) { this.db = db; }

    public List<Map<String,Object>> estudiantes() { return db.queryForList("SELECT * FROM estudiante ORDER BY id"); }
    public Map<String,Object> estudiante(long id) { return one("SELECT * FROM estudiante WHERE id=?", id); }
    public List<Map<String,Object>> oferta() {
        return db.queryForList("SELECT s.*, c.codigo, c.nombre AS curso, c.creditos, p.nombre AS requisito FROM seccion s JOIN curso c ON c.id=s.curso_id LEFT JOIN curso p ON p.id=c.prerrequisito ORDER BY c.codigo, s.nombre");
    }
    public List<Map<String,Object>> matriculas(long id) {
        return db.queryForList("SELECT s.*, c.codigo, c.nombre AS curso, c.creditos FROM matricula m JOIN seccion s ON s.id=m.seccion_id JOIN curso c ON c.id=s.curso_id WHERE m.estudiante_id=? ORDER BY s.dia,s.inicio", id);
    }
    public int creditos(long id) {
        return db.queryForObject("SELECT COALESCE(SUM(c.creditos),0) FROM matricula m JOIN seccion s ON s.id=m.seccion_id JOIN curso c ON c.id=s.curso_id WHERE m.estudiante_id=?", Integer.class, id);
    }
    private Map<String,Object> one(String sql, long id) {
        var rows = db.queryForList(sql, id);
        if (rows.isEmpty()) throw new Rechazo("No se encontró el estudiante o la sección.");
        return rows.getFirst();
    }
    private static int num(Map<String,Object> row, String key) { return ((Number)row.get(key)).intValue(); }

    @Transactional
    public void matricular(long estudianteId, long seccionId) {
        // El orden estudiante -> sección protege también créditos y horarios entre secciones distintas.
        var estudiante = one("SELECT * FROM estudiante WHERE id=? FOR UPDATE", estudianteId);
        var seccion = one("SELECT * FROM seccion WHERE id=? FOR UPDATE", seccionId);
        var curso = one("SELECT * FROM curso WHERE id=?", num(seccion,"curso_id"));
        var actuales = matriculas(estudianteId);
        if (actuales.stream().anyMatch(m -> num(m,"curso_id")==num(seccion,"curso_id")))
            throw new Rechazo("Ya estás matriculado en una sección de este curso.");
        if (num(seccion,"cupo_disponible")==0) throw new Rechazo("La sección ya no tiene vacantes disponibles.");
        if (curso.get("prerrequisito") != null && db.queryForObject("SELECT COUNT(*) FROM aprobado WHERE estudiante_id=? AND curso_id=?", Integer.class, estudianteId, curso.get("prerrequisito"))==0)
            throw new Rechazo("Debes aprobar el prerrequisito antes de matricularte.");
        if (creditos(estudianteId)+num(curso,"creditos") > num(estudiante,"max_creditos"))
            throw new Rechazo("Esta matrícula supera tu límite de créditos.");
        if (actuales.stream().anyMatch(m -> num(m,"dia")==num(seccion,"dia") && num(m,"inicio")<num(seccion,"fin") && num(seccion,"inicio")<num(m,"fin")))
            throw new Rechazo("El horario se cruza con una de tus matrículas.");
        db.update("INSERT INTO matricula(estudiante_id,seccion_id) VALUES (?,?)", estudianteId,seccionId);
        db.update("UPDATE seccion SET cupo_disponible=cupo_disponible-1 WHERE id=?",seccionId);
    }
    @Transactional
    public void retirar(long estudianteId, long seccionId) {
        one("SELECT * FROM estudiante WHERE id=? FOR UPDATE", estudianteId);
        one("SELECT * FROM seccion WHERE id=? FOR UPDATE", seccionId);
        if (db.update("DELETE FROM matricula WHERE estudiante_id=? AND seccion_id=?",estudianteId,seccionId)==0)
            throw new Rechazo("La matrícula ya no está activa; no se liberó otro cupo.");
        db.update("UPDATE seccion SET cupo_disponible=cupo_disponible+1 WHERE id=?",seccionId);
    }
    public static class Rechazo extends RuntimeException { public Rechazo(String message) { super(message); } }
}
