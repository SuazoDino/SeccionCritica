package pe.uni.seccioncritica.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.uni.seccioncritica.exception.ServicioExternoNoDisponibleException;

/** Resiliencia explícita en el borde externo: timeout, reintentos acotados y circuit breaker. */
@Service
public class VerificacionAcademicaService {
    private static final Logger log = LoggerFactory.getLogger(VerificacionAcademicaService.class);

    private final VerificadorAcademico cliente;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration timeout;

    public VerificacionAcademicaService(
            VerificadorAcademico cliente,
            @Value("${resiliencia.verificacion.timeout:2s}") Duration timeout) {
        this.cliente = cliente;
        this.timeout = timeout;
        this.circuitBreaker = CircuitBreaker.of("verificacion-academica", CircuitBreakerConfig.custom()
                .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5)).build());
        this.retry = Retry.of("verificacion-academica", RetryConfig.custom()
                .maxAttempts(3).waitDuration(Duration.ofMillis(50)).build());
        log.info("evento=resiliencia_configurada servicio=verificacion_academica timeout={}ms reintentos={} circuitBreaker={}",
                timeout.toMillis(), retry.getRetryConfig().getMaxAttempts(), circuitBreaker.getName());
    }

    public void verificar(long estudianteId) {
        var llamada = Retry.decorateRunnable(retry,
                CircuitBreaker.decorateRunnable(circuitBreaker, () -> cliente.verificar(estudianteId)));
        try {
            CompletableFuture.runAsync(llamada, executor).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("evento=verificacion_academica_exitosa estudianteId={}", estudianteId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rechazar(estudianteId, "interrumpida", e);
        } catch (TimeoutException e) {
            rechazar(estudianteId, "timeout", e);
        } catch (ExecutionException e) {
            // El rechazo del circuito ocurre dentro del hilo de la llamada y llega envuelto.
            var causa = e.getCause() instanceof CallNotPermittedException
                    ? "circuit_breaker_abierto" : "servicio_no_disponible";
            rechazar(estudianteId, causa, e.getCause());
        }
    }

    private void rechazar(long estudianteId, String causa, Throwable error) {
        log.warn("evento=verificacion_academica_fallida estudianteId={} causa={} estadoCircuito={}",
                estudianteId, causa, circuitBreaker.getState());
        throw new ServicioExternoNoDisponibleException();
    }

    @PreDestroy
    void cerrar() { executor.close(); }
}
