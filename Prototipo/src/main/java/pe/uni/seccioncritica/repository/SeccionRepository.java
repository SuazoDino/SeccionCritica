package pe.uni.seccioncritica.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.uni.seccioncritica.model.Seccion;
import pe.uni.seccioncritica.model.Sesion;

@Repository
public class SeccionRepository {
    private final JdbcClient db;

    public SeccionRepository(JdbcClient db) { this.db = db; }

    /** Bloquea la fila hasta el fin de la transacción; serializa el consumo de cupos de la sección. */
    public Optional<Seccion> bloquear(long id) {
        return db.sql("SELECT * FROM seccion WHERE id = ? FOR UPDATE").param(id).query(Seccion.class).optional();
    }

    public List<Seccion> deCursos(Collection<Long> cursoIds) {
        if (cursoIds.isEmpty()) return List.of();
        return db.sql("SELECT * FROM seccion WHERE curso_id IN (:ids) ORDER BY nombre")
                .param("ids", cursoIds).query(Seccion.class).list();
    }

    public List<Sesion> sesiones(long seccionId) {
        return db.sql("SELECT * FROM sesion WHERE seccion_id = ? ORDER BY dia, inicio")
                .param(seccionId).query(Sesion.class).list();
    }

    public List<Sesion> sesiones(Collection<Long> seccionIds) {
        if (seccionIds.isEmpty()) return List.of();
        return db.sql("SELECT * FROM sesion WHERE seccion_id IN (:ids) ORDER BY dia, inicio")
                .param("ids", seccionIds).query(Sesion.class).list();
    }

    /** Postcondición: se modificó exactamente una fila; si no, la transacción del comando se revierte. */
    public void ajustarCupo(long id, int delta) {
        int filas = db.sql("UPDATE seccion SET cupo_disponible = cupo_disponible + ? WHERE id = ?").params(delta, id).update();
        if (filas != 1)
            throw new IllegalStateException("Se esperaba ajustar el cupo de una sección y se afectaron " + filas);
    }
}
