package pe.uni.seccioncritica.model;

public record Curso(long id, String codigo, String nombre, int creditos, Long prerrequisito) {
    public boolean tienePrerrequisito() { return prerrequisito != null; }
}
