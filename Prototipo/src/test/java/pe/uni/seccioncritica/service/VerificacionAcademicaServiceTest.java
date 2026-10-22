package pe.uni.seccioncritica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import pe.uni.seccioncritica.exception.ServicioExternoNoDisponibleException;

class VerificacionAcademicaServiceTest {
    @Test
    void reintentaFallasTransitoriasYLuegoConfirma() {
        var intentos = new AtomicInteger();
        try (var servicio = new ServicioDePrueba(() -> {
            if (intentos.incrementAndGet() < 3) throw new IllegalStateException("transitorio");
        }, Duration.ofMillis(500))) {
            servicio.verificar(1);
            assertEquals(3, intentos.get());
        }
    }

    @Test
    void timeoutSeConvierteEnRechazoControlado() {
        try (var servicio = new ServicioDePrueba(() -> dormir(200), Duration.ofMillis(50))) {
            assertThrows(ServicioExternoNoDisponibleException.class,
                    () -> servicio.verificar(1));
        }
    }

    private static void dormir(long milisegundos) {
        try { Thread.sleep(milisegundos); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static final class ServicioDePrueba implements AutoCloseable {
        private final VerificacionAcademicaService servicio;

        ServicioDePrueba(Runnable respuesta, Duration timeout) {
            servicio = new VerificacionAcademicaService(id -> respuesta.run(), timeout);
        }

        void verificar(long estudianteId) { servicio.verificar(estudianteId); }
        @Override public void close() { servicio.cerrar(); }
    }
}
