package pe.uni.seccioncritica.model;

import java.util.List;

public record CursoOferta(Curso curso, List<SeccionOferta> secciones) {
}
