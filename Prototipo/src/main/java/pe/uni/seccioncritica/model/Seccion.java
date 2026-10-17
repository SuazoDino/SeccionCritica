package pe.uni.seccioncritica.model;

public record Seccion(long id, long cursoId, String nombre, int cupoMaximo, int cupoDisponible) {
    public boolean agotada() { return cupoDisponible <= 0; }
}
