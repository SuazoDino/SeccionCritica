package pe.uni.seccioncritica.exception;

/** La verificación académica no respondió dentro del contrato de disponibilidad. */
public class ServicioExternoNoDisponibleException extends SeccionCriticaException {
    public ServicioExternoNoDisponibleException() {
        super("No se pudo verificar tu situación académica. Intenta nuevamente en unos segundos.");
    }
}
