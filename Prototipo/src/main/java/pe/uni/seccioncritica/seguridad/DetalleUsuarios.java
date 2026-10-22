package pe.uni.seccioncritica.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.uni.seccioncritica.repository.CuentaRepository;

@Service
public class DetalleUsuarios implements UserDetailsService {
    private final CuentaRepository cuentas;

    public DetalleUsuarios(CuentaRepository cuentas) { this.cuentas = cuentas; }

    @Override
    public UserDetails loadUserByUsername(String codigo) {
        return cuentas.buscarPorCodigo(codigo.strip())
                .map(c -> new UsuarioSesion(c.estudianteId(), c.codigo(), c.claveHash()))
                .orElseThrow(() -> new UsernameNotFoundException("Código no registrado"));
    }
}
