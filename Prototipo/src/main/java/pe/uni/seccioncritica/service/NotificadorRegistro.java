package pe.uni.seccioncritica.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.uni.seccioncritica.model.Cuenta;

/**
 * El prototipo no tiene servidor de correo: el enlace queda en el registro de la aplicación.
 * Para enviarlo de verdad basta otra implementación de {@link Notificador} con spring-boot-starter-mail.
 */
@Component
public class NotificadorRegistro implements Notificador {
    private static final Logger log = LoggerFactory.getLogger(NotificadorRegistro.class);

    @Override
    public void enviarRecuperacion(Cuenta cuenta, String enlace) {
        log.info("Correo de recuperación para {} <{}>: {}", cuenta.codigo(), cuenta.correoOculto(), enlace);
    }
}
