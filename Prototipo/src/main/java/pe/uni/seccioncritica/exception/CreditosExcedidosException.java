package pe.uni.seccioncritica.exception;

public class CreditosExcedidosException extends MatriculaRechazadaException {
    public CreditosExcedidosException() { super("Esta matrícula supera tu límite de créditos."); }
}
