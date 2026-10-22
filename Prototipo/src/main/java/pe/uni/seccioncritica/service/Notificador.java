package pe.uni.seccioncritica.service;

import pe.uni.seccioncritica.model.Cuenta;

/** Canal por el que el estudiante recibe el enlace de recuperación. */
public interface Notificador {
    void enviarRecuperacion(Cuenta cuenta, String enlace);
}
