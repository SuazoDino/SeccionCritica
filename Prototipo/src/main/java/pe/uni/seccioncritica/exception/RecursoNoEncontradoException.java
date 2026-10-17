package pe.uni.seccioncritica.exception;

public class RecursoNoEncontradoException extends SeccionCriticaException {
    public RecursoNoEncontradoException(String recurso, long id) {
        super("No se encontró " + recurso + " con identificador " + id + ".");
    }
}
