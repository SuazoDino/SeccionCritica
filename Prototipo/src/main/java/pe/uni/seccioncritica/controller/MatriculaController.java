package pe.uni.seccioncritica.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.uni.seccioncritica.exception.MatriculaRechazadaException;
import pe.uni.seccioncritica.seguridad.UsuarioSesion;
import pe.uni.seccioncritica.service.MatriculaService;

@Controller
public class MatriculaController {
    private static final Logger log = LoggerFactory.getLogger(MatriculaController.class);

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) { this.service = service; }

    @GetMapping("/")
    public String inicio() { return "redirect:/oferta"; }

    @GetMapping("/oferta")
    public String oferta(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
        long estudiante = usuario.estudianteId();
        cargarPanel(estudiante, model, "oferta");
        model.addAttribute("cursos", service.oferta(estudiante));
        return "panel";
    }

    @GetMapping("/matriculas")
    public String matriculas(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
        long estudiante = usuario.estudianteId();
        cargarPanel(estudiante, model, "matriculas");
        model.addAttribute("agenda", service.agenda(estudiante));
        model.addAttribute("inscripciones", service.inscripciones(estudiante));
        return "panel";
    }

    @PostMapping("/matricular")
    public String matricular(@AuthenticationPrincipal UsuarioSesion usuario, @RequestParam long seccion,
                             RedirectAttributes flash) {
        long estudiante = usuario.estudianteId();
        ejecutar(() -> service.matricular(estudiante, seccion), "matricular", estudiante, seccion,
                "Matrícula confirmada. Tu vacante está reservada.", flash);
        return "redirect:/oferta";
    }

    @PostMapping("/retirar")
    public String retirar(@AuthenticationPrincipal UsuarioSesion usuario, @RequestParam long seccion,
                          RedirectAttributes flash) {
        long estudiante = usuario.estudianteId();
        ejecutar(() -> service.retirar(estudiante, seccion), "retirar", estudiante, seccion,
                "Retiro realizado. La vacante vuelve a estar disponible.", flash);
        return "redirect:/matriculas";
    }

    private void cargarPanel(long estudiante, Model model, String vista) {
        model.addAttribute("estudiante", service.estudiante(estudiante));
        model.addAttribute("creditos", service.creditos(estudiante));
        model.addAttribute("vista", vista);
    }

    /**
     * Los rechazos por regla y la contención momentánea de la base vuelven al panel con un mensaje,
     * en vez de una página de error. Lo demás lo resuelve {@link ManejadorErrores}.
     */
    private void ejecutar(Runnable operacion, String accion, long estudiante, long seccion,
                          String exito, RedirectAttributes flash) {
        try {
            operacion.run();
            flash.addFlashAttribute("exito", exito);
        } catch (MatriculaRechazadaException e) {
            log.warn("Rechazo al {}: estudiante={} seccion={} regla={} motivo={}",
                    accion, estudiante, seccion, e.getClass().getSimpleName(), e.getMessage());
            flash.addFlashAttribute("error", e.getMessage());
        } catch (TransientDataAccessException e) {
            log.warn("Contención al {}: estudiante={} seccion={} causa={}", accion, estudiante, seccion, e.toString());
            flash.addFlashAttribute("error",
                    "Hay muchas solicitudes en este momento y no se modificó nada. Intenta nuevamente en unos segundos.");
        }
    }
}
