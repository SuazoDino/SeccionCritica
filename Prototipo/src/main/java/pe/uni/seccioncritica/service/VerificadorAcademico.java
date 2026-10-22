package pe.uni.seccioncritica.service;

/** Frontera del servicio externo que confirma que un estudiante puede matricularse. */
public interface VerificadorAcademico {
    void verificar(long estudianteId);
}
