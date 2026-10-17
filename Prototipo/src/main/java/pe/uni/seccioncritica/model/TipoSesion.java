package pe.uni.seccioncritica.model;

public enum TipoSesion {
    TEORIA("Teoría"), PRACTICA("Práctica");

    private final String etiqueta;

    TipoSesion(String etiqueta) { this.etiqueta = etiqueta; }

    public String etiqueta() { return etiqueta; }
}
