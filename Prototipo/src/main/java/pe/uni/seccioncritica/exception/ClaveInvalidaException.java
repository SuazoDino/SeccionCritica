package pe.uni.seccioncritica.exception;

/** La nueva contraseña no cumple los requisitos; el enlace de recuperación sigue vigente. */
public class ClaveInvalidaException extends SeccionCriticaException {
    public ClaveInvalidaException(String mensaje) { super(mensaje); }
}
