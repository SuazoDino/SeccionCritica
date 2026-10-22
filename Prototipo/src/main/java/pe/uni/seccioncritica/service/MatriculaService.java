package pe.uni.seccioncritica.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.uni.seccioncritica.exception.MatriculaRechazadaException;
import pe.uni.seccioncritica.exception.RecursoNoEncontradoException;
import pe.uni.seccioncritica.model.Curso;
import pe.uni.seccioncritica.model.CursoOferta;
import pe.uni.seccioncritica.model.Estudiante;
import pe.uni.seccioncritica.model.EventoAgenda;
import pe.uni.seccioncritica.model.Inscripcion;
import pe.uni.seccioncritica.model.Seccion;
import pe.uni.seccioncritica.model.SeccionOferta;
import pe.uni.seccioncritica.model.Sesion;
import pe.uni.seccioncritica.repository.CursoRepository;
import pe.uni.seccioncritica.repository.EstudianteRepository;
import pe.uni.seccioncritica.repository.MatriculaRepository;
import pe.uni.seccioncritica.repository.SeccionRepository;

@Service
public class MatriculaService {
    private static final Logger log = LoggerFactory.getLogger(MatriculaService.class);
    private final EstudianteRepository estudiantes;
    private final CursoRepository cursos;
    private final SeccionRepository secciones;
    private final MatriculaRepository matriculas;
    private final MatriculaCommandService comandos;
    private final VerificacionAcademicaService verificacion;

    public MatriculaService(EstudianteRepository estudiantes, CursoRepository cursos,
                            SeccionRepository secciones, MatriculaRepository matriculas,
                            MatriculaCommandService comandos, VerificacionAcademicaService verificacion) {
        this.estudiantes = estudiantes;
        this.cursos = cursos;
        this.secciones = secciones;
        this.matriculas = matriculas;
        this.comandos = comandos;
        this.verificacion = verificacion;
    }

    public List<Estudiante> estudiantes() { return estudiantes.listar(); }

    public Estudiante estudiante(long id) {
        return estudiantes.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("el estudiante", id));
    }

    @Transactional(readOnly = true)
    public List<CursoOferta> oferta(long estudianteId) {
        var habilitados = cursos.habilitadosPara(estudianteId);
        var seccionesPorCurso = secciones.deCursos(habilitados.stream().map(Curso::id).toList())
                .stream().collect(groupingBy(Seccion::cursoId));
        var sesionesPorSeccion = secciones.sesiones(seccionesPorCurso.values().stream()
                        .flatMap(List::stream).map(Seccion::id).toList())
                .stream().collect(groupingBy(Sesion::seccionId));
        return habilitados.stream().filter(c -> seccionesPorCurso.containsKey(c.id()))
                .map(c -> new CursoOferta(c, seccionesPorCurso.get(c.id()).stream()
                        .map(s -> new SeccionOferta(s, sesionesPorSeccion.getOrDefault(s.id(), List.of())))
                        .toList())).toList();
    }

    public List<Inscripcion> inscripciones(long estudianteId) { return matriculas.inscripciones(estudianteId); }
    public List<EventoAgenda> agenda(long estudianteId) { return matriculas.agenda(estudianteId); }
    public int creditos(long estudianteId) { return matriculas.creditos(estudianteId); }

    public void matricular(long estudianteId, long seccionId) {
        try {
            verificacion.verificar(estudianteId);
            comandos.matricular(estudianteId, seccionId);
        } catch (MatriculaRechazadaException e) {
            log.warn("evento=matricula_rechazada estudianteId={} seccionId={} regla={} motivo={}",
                    estudianteId, seccionId, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
        log.info("evento=matricula_confirmada estudianteId={} seccionId={}", estudianteId, seccionId);
    }

    public void retirar(long estudianteId, long seccionId) {
        try {
            comandos.retirar(estudianteId, seccionId);
        } catch (MatriculaRechazadaException e) {
            log.warn("evento=retiro_rechazado estudianteId={} seccionId={} regla={} motivo={}",
                    estudianteId, seccionId, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
        log.info("evento=retiro_confirmado estudianteId={} seccionId={}", estudianteId, seccionId);
    }
}
