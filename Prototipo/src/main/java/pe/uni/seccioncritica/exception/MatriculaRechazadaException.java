package pe.uni.seccioncritica.exception;

/** Una regla académica impide la operación; no se modificó ningún dato. */
public class MatriculaRechazadaException extends SeccionCriticaException {
    public MatriculaRechazadaException(String mensaje) { super(mensaje); }
}
