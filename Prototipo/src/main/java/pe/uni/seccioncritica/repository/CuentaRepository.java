package pe.uni.seccioncritica.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.uni.seccioncritica.model.Cuenta;

@Repository
public class CuentaRepository {
    private final JdbcClient db;

    public CuentaRepository(JdbcClient db) { this.db = db; }

    /** El código no distingue mayúsculas: 20230001a y 20230001A son la misma cuenta. */
    public Optional<Cuenta> buscarPorCodigo(String codigo) {
        return db.sql("""
                SELECT c.estudiante_id, e.codigo, e.nombre, c.correo, c.clave_hash
                FROM cuenta c
                JOIN estudiante e ON e.id = c.estudiante_id
                WHERE UPPER(e.codigo) = UPPER(?)""")
                .param(codigo).query(Cuenta.class).optional();
    }

    public void cambiarClave(long estudianteId, String claveHash) {
        db.sql("UPDATE cuenta SET clave_hash = ? WHERE estudiante_id = ?").params(claveHash, estudianteId).update();
    }

    public void registrarRecuperacion(String tokenHash, long estudianteId, OffsetDateTime expira) {
        db.sql("INSERT INTO recuperacion (token_hash, estudiante_id, expira) VALUES (?, ?, ?)")
                .params(tokenHash, estudianteId, expira).update();
    }

    /** Invalida los enlaces pendientes: solo el último enlace enviado sirve. */
    public void anularRecuperaciones(long estudianteId) {
        db.sql("UPDATE recuperacion SET usado = TRUE WHERE estudiante_id = ? AND NOT usado").param(estudianteId).update();
    }

    public boolean recuperacionVigente(String tokenHash, OffsetDateTime ahora) {
        return db.sql("SELECT COUNT(*) FROM recuperacion WHERE token_hash = ? AND NOT usado AND expira > ?")
                .params(tokenHash, ahora).query(Integer.class).single() > 0;
    }

    /**
     * Marca el enlace como usado y devuelve su estudiante. El UPDATE condicional bloquea la fila:
     * si dos pestañas envían el mismo enlace a la vez, solo una lo consume.
     */
    public Optional<Long> consumirRecuperacion(String tokenHash, OffsetDateTime ahora) {
        int consumidos = db.sql("UPDATE recuperacion SET usado = TRUE WHERE token_hash = ? AND NOT usado AND expira > ?")
                .params(tokenHash, ahora).update();
        if (consumidos == 0) return Optional.empty();
        return db.sql("SELECT estudiante_id FROM recuperacion WHERE token_hash = ?")
                .param(tokenHash).query(Long.class).optional();
    }
}
