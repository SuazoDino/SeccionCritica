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

    @Test
    void circuitBreakerAbiertoDejaDeLlamarAlServicioCaido() {
        var llamadas = new AtomicInteger();
        try (var servicio = new ServicioDePrueba(() -> {
            llamadas.incrementAndGet();
            throw new IllegalStateException("caído");
        }, Duration.ofMillis(500))) {
            // 3 intentos de la primera solicitud + 1 de la segunda llenan la ventana (4 llamadas, 100% fallos).
            for (int i = 0; i < 2; i++)
                assertThrows(ServicioExternoNoDisponibleException.class, () -> servicio.verificar(1));
            assertEquals(4, llamadas.get());

            // Con el circuito abierto la solicitud se rechaza de forma controlada sin tocar el servicio.
            assertThrows(ServicioExternoNoDisponibleException.class, () -> servicio.verificar(2));
            assertEquals(4, llamadas.get());
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
