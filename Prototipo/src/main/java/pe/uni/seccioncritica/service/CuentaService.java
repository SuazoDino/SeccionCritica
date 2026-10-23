package pe.uni.seccioncritica.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.uni.seccioncritica.exception.ClaveInvalidaException;
import pe.uni.seccioncritica.exception.EnlaceInvalidoException;
import pe.uni.seccioncritica.repository.CuentaRepository;

/** Recuperación de contraseña con enlaces de un solo uso. */
@Service
public class CuentaService {
    public static final Duration VIGENCIA_ENLACE = Duration.ofMinutes(30);
    private static final SecureRandom AZAR = new SecureRandom();

    private final CuentaRepository cuentas;
    private final PasswordEncoder codificador;
    private final Notificador notificador;
    private final String urlPublica;

    public CuentaService(CuentaRepository cuentas, PasswordEncoder codificador, Notificador notificador,
                         @Value("${seccioncritica.url-publica}") String urlPublica) {
        this.cuentas = cuentas;
        this.codificador = codificador;
        this.notificador = notificador;
        this.urlPublica = urlPublica;
    }

    /**
     * Envía un enlace nuevo y anula los anteriores. Devuelve el enlace solo si la cuenta existe;
     * quien llama debe responder lo mismo en ambos casos para no revelar qué códigos están registrados.
     */
    @Transactional
    public Optional<String> solicitarRecuperacion(String codigo) {
        return cuentas.buscarPorCodigo(codigo.strip()).map(cuenta -> {
            var token = nuevoToken();
            cuentas.anularRecuperaciones(cuenta.estudianteId());
            cuentas.registrarRecuperacion(hash(token), cuenta.estudianteId(), ahora().plus(VIGENCIA_ENLACE));
            var enlace = urlPublica + "/restablecer?token=" + token;
            notificador.enviarRecuperacion(cuenta, enlace);
            return enlace;
        });
    }

    public boolean enlaceVigente(String token) {
        return token != null && !token.isBlank() && cuentas.recuperacionVigente(hash(token), ahora());
    }

    /** Valida la contraseña antes de consumir el enlace: si no cumple, el estudiante puede corregirla. */
    @Transactional
    public void restablecer(String token, String clave, String confirmacion) {
        validar(clave, confirmacion);
        if (token == null || token.isBlank()) throw new EnlaceInvalidoException();
        long estudianteId = cuentas.consumirRecuperacion(hash(token), ahora()).orElseThrow(EnlaceInvalidoException::new);
        cuentas.cambiarClave(estudianteId, codificador.encode(clave));
        cuentas.anularRecuperaciones(estudianteId);
    }

    private static void validar(String clave, String confirmacion) {
        if (clave == null || clave.length() < 8)
            throw new ClaveInvalidaException("La contraseña debe tener al menos 8 caracteres.");
        // BCrypt ignora lo que pasa de 72 bytes; se rechaza para que la contraseña completa cuente.
        if (clave.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ClaveInvalidaException("La contraseña es demasiado larga (máximo 72 caracteres).");
        if (clave.chars().noneMatch(Character::isLetter) || clave.chars().noneMatch(Character::isDigit))
            throw new ClaveInvalidaException("La contraseña debe combinar letras y números.");
        if (!clave.equals(confirmacion))
            throw new ClaveInvalidaException("Las contraseñas no coinciden.");
    }

    private static String nuevoToken() {
        var bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static OffsetDateTime ahora() { return OffsetDateTime.now(ZoneOffset.UTC); }
}
