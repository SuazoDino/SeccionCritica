package pe.uni.seccioncritica.model;

/** Credenciales de un estudiante. El código de estudiante es el usuario de inicio de sesión. */
public record Cuenta(long estudianteId, String codigo, String nombre, String correo, String claveHash) {
    /** a***@uni.pe: confirma a qué correo se envió el enlace sin revelarlo completo. */
    public String correoOculto() {
        int arroba = correo.indexOf('@');
        return arroba <= 1 ? correo : correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
