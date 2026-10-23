package pe.uni.seccioncritica.model;

/** Invariante: {@code 0 <= cupoDisponible <= cupoMaximo} y {@code cupoMaximo > 0}, igual que el CHECK de schema.sql. */
public record Seccion(long id, long cursoId, String nombre, int cupoMaximo, int cupoDisponible) {
    public Seccion {
        if (cupoMaximo <= 0 || cupoDisponible < 0 || cupoDisponible > cupoMaximo)
            throw new IllegalStateException("Invariante de cupos violado en la sección " + id
                    + ": disponible=" + cupoDisponible + " máximo=" + cupoMaximo);
    }

    public boolean agotada() { return cupoDisponible <= 0; }
}
