package pe.uni.seccioncritica.model;

/** Matrícula activa de un estudiante, tal como se lista en "Mis matrículas". */
public record Inscripcion(long seccionId, String seccion, String codigo, String curso, int creditos) {
}
