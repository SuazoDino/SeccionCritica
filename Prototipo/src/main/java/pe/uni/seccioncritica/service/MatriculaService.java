package pe.uni.seccioncritica.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.uni.seccioncritica.exception.CreditosExcedidosException;
import pe.uni.seccioncritica.exception.CruceDeHorarioException;
import pe.uni.seccioncritica.exception.CupoNoDisponibleException;
import pe.uni.seccioncritica.exception.MatriculaRechazadaException;
import pe.uni.seccioncritica.exception.PrerrequisitoNoCumplidoException;
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
    private final EstudianteRepository estudiantes;
    private final CursoRepository cursos;
    private final SeccionRepository secciones;
    private final MatriculaRepository matriculas;

    public MatriculaService(EstudianteRepository estudiantes, CursoRepository cursos,
                            SeccionRepository secciones, MatriculaRepository matriculas) {
        this.estudiantes = estudiantes;
        this.cursos = cursos;
        this.secciones = secciones;
        this.matriculas = matriculas;
    }

    public List<Estudiante> estudiantes() { return estudiantes.listar(); }

    public Estudiante estudiante(long id) {
        return estudiantes.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("el estudiante", id));
    }

    /** Oferta agrupada por curso. Usa tres consultas sin importar cuántas secciones haya. */
    @Transactional(readOnly = true)
    public List<CursoOferta> oferta(long estudianteId) {
        var habilitados = cursos.habilitadosPara(estudianteId);
        var seccionesPorCurso = secciones.deCursos(habilitados.stream().map(Curso::id).toList())
                .stream().collect(groupingBy(Seccion::cursoId));
        var sesionesPorSeccion = secciones.sesiones(seccionesPorCurso.values().stream()
                        .flatMap(List::stream).map(Seccion::id).toList())
                .stream().collect(groupingBy(Sesion::seccionId));
        return habilitados.stream()
                .filter(c -> seccionesPorCurso.containsKey(c.id()))
                .map(c -> new CursoOferta(c, seccionesPorCurso.get(c.id()).stream()
                        .map(s -> new SeccionOferta(s, sesionesPorSeccion.getOrDefault(s.id(), List.of())))
                        .toList()))
                .toList();
    }

    public List<Inscripcion> inscripciones(long estudianteId) { return matriculas.inscripciones(estudianteId); }

    public List<EventoAgenda> agenda(long estudianteId) { return matriculas.agenda(estudianteId); }

    public int creditos(long estudianteId) { return matriculas.creditos(estudianteId); }

    @Transactional(timeout = 10)
    public void matricular(long estudianteId, long seccionId) {
        // El orden estudiante -> sección protege también créditos y horarios entre secciones distintas.
        var estudiante = estudiantes.bloquear(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el estudiante", estudianteId));
        var seccion = secciones.bloquear(seccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("la sección", seccionId));
        var curso = cursos.buscar(seccion.cursoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("el curso", seccion.cursoId()));

        if (estudiantes.aprobo(estudianteId, curso.id()))
            throw new MatriculaRechazadaException("Ya aprobaste este curso; no puedes volver a matricularte.");
        if (matriculas.matriculadoEnCurso(estudianteId, curso.id()))
            throw new MatriculaRechazadaException("Ya estás matriculado en una sección de este curso.");
        if (seccion.agotada())
            throw new CupoNoDisponibleException();
        if (curso.tienePrerrequisito() && !estudiantes.aprobo(estudianteId, curso.prerrequisito()))
            throw new PrerrequisitoNoCumplidoException();
        if (matriculas.creditos(estudianteId) + curso.creditos() > estudiante.maxCreditos())
            throw new CreditosExcedidosException();
        validarHorario(curso, secciones.sesiones(seccionId), matriculas.agenda(estudianteId));

        matriculas.registrar(estudianteId, seccionId);
        secciones.ajustarCupo(seccionId, -1);
    }

    @Transactional(timeout = 10)
    public void retirar(long estudianteId, long seccionId) {
        estudiantes.bloquear(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el estudiante", estudianteId));
        secciones.bloquear(seccionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("la sección", seccionId));
        if (!matriculas.eliminar(estudianteId, seccionId))
            throw new MatriculaRechazadaException("La matrícula ya no está activa; no se liberó otro cupo.");
        secciones.ajustarCupo(seccionId, +1);
    }

    private static void validarHorario(Curso curso, List<Sesion> nuevas, List<EventoAgenda> actuales) {
        for (var nueva : nuevas)
            for (var actual : actuales)
                if (nueva.cruzaPracticaCon(actual))
                    throw new CruceDeHorarioException(curso.nombre(), actual.curso(), nueva, actual);
    }
}
