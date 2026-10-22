package pe.uni.seccioncritica.exception;

public class EnlaceInvalidoException extends SeccionCriticaException {
    public EnlaceInvalidoException() {
        super("El enlace de recuperación no es válido, ya se usó o venció. Solicita uno nuevo.");
    }
}
