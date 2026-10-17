package pe.uni.seccioncritica.exception;

/** Base de las excepciones de dominio. El mensaje siempre es apto para mostrarse al estudiante. */
public abstract class SeccionCriticaException extends RuntimeException {
    protected SeccionCriticaException(String mensaje) { super(mensaje); }
}
