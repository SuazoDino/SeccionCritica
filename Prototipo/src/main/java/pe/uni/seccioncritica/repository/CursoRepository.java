package pe.uni.seccioncritica.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.uni.seccioncritica.model.Curso;

@Repository
public class CursoRepository {
    private final JdbcClient db;

    public CursoRepository(JdbcClient db) { this.db = db; }

    public Optional<Curso> buscar(long id) {
        return db.sql("SELECT * FROM curso WHERE id = ?").param(id).query(Curso.class).optional();
    }

    /** Cursos que el estudiante aún no aprueba y cuyo prerrequisito, si lo hay, ya aprobó. */
    public List<Curso> habilitadosPara(long estudianteId) {
        return db.sql("""
                SELECT c.* FROM curso c
                WHERE NOT EXISTS (SELECT 1 FROM aprobado a WHERE a.estudiante_id = :e AND a.curso_id = c.id)
                  AND (c.prerrequisito IS NULL
                       OR EXISTS (SELECT 1 FROM aprobado a WHERE a.estudiante_id = :e AND a.curso_id = c.prerrequisito))
                ORDER BY c.codigo""")
                .param("e", estudianteId).query(Curso.class).list();
    }
}
