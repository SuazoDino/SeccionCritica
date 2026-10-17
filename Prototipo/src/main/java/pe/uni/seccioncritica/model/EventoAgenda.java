package pe.uni.seccioncritica.model;

/** Sesión de una sección matriculada, con los datos que muestra la agenda semanal. */
public record EventoAgenda(long id, TipoSesion tipo, int dia, int inicio, int fin,
                           long cursoId, String codigo, String curso, String seccion) implements BloqueHorario {
}
