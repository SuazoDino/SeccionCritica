package pe.uni.seccioncritica.model;

public record Estudiante(long id, String nombre, String codigo, int maxCreditos) {
    public String inicial() { return nombre.isEmpty() ? "?" : nombre.substring(0, 1); }
}
