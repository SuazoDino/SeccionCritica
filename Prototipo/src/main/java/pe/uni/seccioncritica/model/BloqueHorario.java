package pe.uni.seccioncritica.model;

/** Franja semanal de una clase. Los minutos se cuentan desde las 00:00 del día. */
public interface BloqueHorario {
    String[] DIAS = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    TipoSesion tipo();
    int dia();
    int inicio();
    int fin();

    /** Solo los cruces práctica–práctica bloquean la matrícula; los extremos contiguos no cuentan. */
    default boolean cruzaPracticaCon(BloqueHorario otro) {
        return tipo() == TipoSesion.PRACTICA && otro.tipo() == TipoSesion.PRACTICA
                && dia() == otro.dia() && inicio() < otro.fin() && otro.inicio() < fin();
    }

    default String horario() { return formatear(dia(), inicio(), fin()); }

    static String formatear(int dia, int inicio, int fin) {
        return DIAS[dia] + " · " + hora(inicio) + "–" + hora(fin);
    }

    private static String hora(int minutos) { return "%02d:%02d".formatted(minutos / 60, minutos % 60); }
}
