package pe.uni.seccioncritica.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.uni.seccioncritica.model.Estudiante;

@Repository
public class EstudianteRepository {
    private final JdbcClient db;

    public EstudianteRepository(JdbcClient db) { this.db = db; }

    public List<Estudiante> listar() {
        return db.sql("SELECT * FROM estudiante ORDER BY id").query(Estudiante.class).list();
    }

    public Optional<Estudiante> buscar(long id) {
        return db.sql("SELECT * FROM estudiante WHERE id = ?").param(id).query(Estudiante.class).optional();
    }

    /** Bloquea la fila hasta el fin de la transacción; serializa las operaciones del mismo estudiante. */
    public Optional<Estudiante> bloquear(long id) {
        return db.sql("SELECT * FROM estudiante WHERE id = ? FOR UPDATE").param(id).query(Estudiante.class).optional();
    }

    public boolean aprobo(long estudianteId, long cursoId) {
        return db.sql("SELECT COUNT(*) FROM aprobado WHERE estudiante_id = ? AND curso_id = ?")
                .params(estudianteId, cursoId).query(Integer.class).single() > 0;
    }
}
