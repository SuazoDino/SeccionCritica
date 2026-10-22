package pe.uni.seccioncritica.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stub configurable para demostrar éxito, falla y latencia sin depender de una red real. */
@Component
class VerificadorAcademicoDemo implements VerificadorAcademico {
    private final String modo;
    private final Duration latencia;

    VerificadorAcademicoDemo(
            @Value("${demo.verificacion-academica.modo:ok}") String modo,
            @Value("${demo.verificacion-academica.latencia:0ms}") Duration latencia) {
        this.modo = modo;
        this.latencia = latencia;
    }

    @Override
    public void verificar(long estudianteId) {
        if (!latencia.isZero()) {
            try {
                Thread.sleep(latencia.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Verificación interrumpida", e);
            }
        }
        if ("fallo".equalsIgnoreCase(modo)) {
            throw new IllegalStateException("Falla simulada del verificador académico");
        }
    }
}
