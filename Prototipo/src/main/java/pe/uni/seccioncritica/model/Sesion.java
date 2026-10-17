package pe.uni.seccioncritica.model;

public record Sesion(long id, long seccionId, TipoSesion tipo, int dia, int inicio, int fin) implements BloqueHorario {
}
