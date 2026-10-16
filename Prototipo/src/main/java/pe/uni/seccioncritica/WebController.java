package pe.uni.seccioncritica;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private final MatriculaService service;
    public WebController(MatriculaService service) { this.service=service; }
    @GetMapping("/")
    public String inicio(Model model) { model.addAttribute("estudiantes",service.estudiantes()); return "inicio"; }
    @GetMapping({"/oferta", "/matriculas"})
    public String oferta(@RequestParam long estudiante, Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("estudiante",service.estudiante(estudiante));
        model.addAttribute("oferta",service.oferta());
        model.addAttribute("matriculas",service.matriculas(estudiante));
        model.addAttribute("creditos",service.creditos(estudiante));
        model.addAttribute("vista",request.getRequestURI().equals("/matriculas") ? "matriculas" : "oferta");
        return "panel";
    }
    @PostMapping("/matricular")
    public String matricular(@RequestParam long estudiante, @RequestParam long seccion, RedirectAttributes flash) {
        try { service.matricular(estudiante,seccion); flash.addFlashAttribute("exito","Matrícula confirmada. Tu vacante está reservada."); }
        catch (MatriculaService.Rechazo e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/oferta?estudiante="+estudiante;
    }
    @PostMapping("/retirar")
    public String retirar(@RequestParam long estudiante, @RequestParam long seccion, RedirectAttributes flash) {
        try { service.retirar(estudiante,seccion); flash.addFlashAttribute("exito","Retiro realizado. La vacante vuelve a estar disponible."); }
        catch (MatriculaService.Rechazo e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/matriculas?estudiante="+estudiante;
    }
    @ExceptionHandler(MatriculaService.Rechazo.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public String noEncontrado(Model model, MatriculaService.Rechazo error) {
        model.addAttribute("mensaje",error.getMessage()); return "error";
    }
    public static String horario(int dia,int inicio,int fin) {
        return new String[]{"","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"}[dia]+" · "+hora(inicio)+"–"+hora(fin);
    }
    private static String hora(int value) { return "%02d:%02d".formatted(value/60,value%60); }
}
