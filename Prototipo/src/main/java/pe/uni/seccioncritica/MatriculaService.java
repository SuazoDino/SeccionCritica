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
    public List<Map<String,Object>> oferta(long estudiante) {
        var rows = db.queryForList("SELECT s.*, c.codigo, c.nombre AS curso, c.creditos, p.nombre AS requisito FROM seccion s JOIN curso c ON c.id=s.curso_id LEFT JOIN curso p ON p.id=c.prerrequisito WHERE NOT EXISTS (SELECT 1 FROM aprobado a WHERE a.estudiante_id=? AND a.curso_id=c.id) AND (c.prerrequisito IS NULL OR EXISTS (SELECT 1 FROM aprobado a WHERE a.estudiante_id=? AND a.curso_id=c.prerrequisito)) ORDER BY c.codigo, s.nombre", estudiante, estudiante);
        rows.forEach(s -> s.put("sesiones", sesiones(num(s,"id"))));
        return rows;
    }
    public List<Map<String,Object>> cursos(long estudiante) {
        var agrupados = new java.util.LinkedHashMap<Object,Map<String,Object>>();
        for(var seccion : oferta(estudiante)) {
            var curso=agrupados.computeIfAbsent(seccion.get("curso_id"), id -> {
                var c=new org.springframework.util.LinkedCaseInsensitiveMap<Object>();
                c.putAll(seccion);
                c.put("secciones",new java.util.ArrayList<Map<String,Object>>()); return c;
            });
            @SuppressWarnings("unchecked") var secciones=(List<Map<String,Object>>)curso.get("secciones");
            secciones.add(seccion);
        }
        return List.copyOf(agrupados.values());
    }
    public List<Map<String,Object>> sesiones(long seccion) {
        return db.queryForList("SELECT * FROM sesion WHERE seccion_id=? ORDER BY dia,inicio",seccion);
    }
    public List<Map<String,Object>> agenda(long estudiante) {
        return db.queryForList("SELECT h.*,c.nombre AS curso,c.codigo,s.nombre AS seccion,c.id AS curso_id FROM matricula m JOIN seccion s ON s.id=m.seccion_id JOIN curso c ON c.id=s.curso_id JOIN sesion h ON h.seccion_id=s.id WHERE m.estudiante_id=? ORDER BY h.dia,h.inicio",estudiante);
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
        if(db.queryForObject("SELECT COUNT(*) FROM aprobado WHERE estudiante_id=? AND curso_id=?",Integer.class,estudianteId,seccion.get("curso_id"))>0)
            throw new Rechazo("Ya aprobaste este curso; no puedes volver a matricularte.");
        var actuales = matriculas(estudianteId);
        if (actuales.stream().anyMatch(m -> num(m,"curso_id")==num(seccion,"curso_id")))
            throw new Rechazo("Ya estás matriculado en una sección de este curso.");
        if (num(seccion,"cupo_disponible")==0) throw new Rechazo("La sección ya no tiene vacantes disponibles.");
        if (curso.get("prerrequisito") != null && db.queryForObject("SELECT COUNT(*) FROM aprobado WHERE estudiante_id=? AND curso_id=?", Integer.class, estudianteId, curso.get("prerrequisito"))==0)
            throw new Rechazo("Debes aprobar el prerrequisito antes de matricularte.");
        if (creditos(estudianteId)+num(curso,"creditos") > num(estudiante,"max_creditos"))
            throw new Rechazo("Esta matrícula supera tu límite de créditos.");
        for(var nueva : sesiones(seccionId)) for(var actual : agenda(estudianteId)) {
            if("PRACTICA".equals(nueva.get("tipo")) && "PRACTICA".equals(actual.get("tipo")) && num(nueva,"dia")==num(actual,"dia") && num(nueva,"inicio")<num(actual,"fin") && num(actual,"inicio")<num(nueva,"fin"))
                throw new Rechazo("La práctica de " + curso.get("nombre") + " se cruza con la práctica de " + actual.get("curso") + ". " + WebController.horario(num(nueva,"dia"),Math.max(num(nueva,"inicio"),num(actual,"inicio")),Math.min(num(nueva,"fin"),num(actual,"fin"))));
        }
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
