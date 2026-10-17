package pe.uni.seccioncritica.exception;

public class PrerrequisitoNoCumplidoException extends MatriculaRechazadaException {
    public PrerrequisitoNoCumplidoException() { super("Debes aprobar el prerrequisito antes de matricularte."); }
}
