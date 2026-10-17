package pe.uni.seccioncritica.exception;

public class CupoNoDisponibleException extends MatriculaRechazadaException {
    public CupoNoDisponibleException() { super("La sección ya no tiene vacantes disponibles."); }
}
