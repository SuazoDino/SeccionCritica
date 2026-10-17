package pe.uni.seccioncritica.exception;

import pe.uni.seccioncritica.model.BloqueHorario;

public class CruceDeHorarioException extends MatriculaRechazadaException {
    public CruceDeHorarioException(String curso, String otroCurso, BloqueHorario a, BloqueHorario b) {
        super("La práctica de " + curso + " se cruza con la práctica de " + otroCurso + ". "
                + BloqueHorario.formatear(a.dia(), Math.max(a.inicio(), b.inicio()), Math.min(a.fin(), b.fin())));
    }
}
