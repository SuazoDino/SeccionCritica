package pe.uni.seccioncritica.seguridad;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Estudiante autenticado. Los controladores toman de aquí el estudiante, nunca de un parámetro de la URL. */
public class UsuarioSesion extends User {
    private final long estudianteId;

    public UsuarioSesion(long estudianteId, String codigo, String claveHash) {
        super(codigo, claveHash, List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        this.estudianteId = estudianteId;
    }

    public long estudianteId() { return estudianteId; }
}
