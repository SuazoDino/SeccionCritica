package pe.uni.seccioncritica.model;

import java.util.List;

public record SeccionOferta(Seccion seccion, List<Sesion> sesiones) {
}
