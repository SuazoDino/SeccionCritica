package pe.uni.seccioncritica.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.uni.seccioncritica.exception.ClaveInvalidaException;
import pe.uni.seccioncritica.exception.EnlaceInvalidoException;
import pe.uni.seccioncritica.seguridad.UsuarioSesion;
import pe.uni.seccioncritica.service.CuentaService;
import pe.uni.seccioncritica.service.MatriculaService;

/** Inicio de sesión y recuperación de contraseña. El POST de /login y /logout lo atiende Spring Security. */
@Controller
public class AccesoController {
    private final CuentaService cuentas;
    private final MatriculaService matricula;
    private final boolean demo;

    public AccesoController(CuentaService cuentas, MatriculaService matricula,
                            @Value("${seccioncritica.demo}") boolean demo) {
        this.cuentas = cuentas;
        this.matricula = matricula;
        this.demo = demo;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
        if (usuario != null) return "redirect:/oferta";
        if (demo) model.addAttribute("cuentasDemo", matricula.estudiantes());
        return "login";
    }

    @GetMapping("/recuperar")
    public String recuperar() { return "recuperar"; }

    @PostMapping("/recuperar")
    public String solicitar(@RequestParam(defaultValue = "") String codigo, RedirectAttributes flash) {
        if (codigo.isBlank()) {
            flash.addFlashAttribute("error", "Ingresa tu código de estudiante.");
            return "redirect:/recuperar";
        }
        // Misma respuesta exista o no la cuenta: la página no confirma qué códigos están registrados.
        var enlace = cuentas.solicitarRecuperacion(codigo);
        flash.addFlashAttribute("enviado", true);
        if (demo) enlace.ifPresent(e -> flash.addFlashAttribute("enlaceDemo", e));
        return "redirect:/recuperar";
    }

    @GetMapping("/restablecer")
    public String restablecer(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("vigente", cuentas.enlaceVigente(token));
        return "restablecer";
    }

    @PostMapping("/restablecer")
    public String cambiar(@RequestParam(required = false) String token,
                          @RequestParam(defaultValue = "") String clave,
                          @RequestParam(defaultValue = "") String confirmacion, Model model) {
        try {
            cuentas.restablecer(token, clave, confirmacion);
            return "redirect:/login?restablecida";
        } catch (ClaveInvalidaException e) {
            model.addAttribute("token", token);
            model.addAttribute("vigente", cuentas.enlaceVigente(token));
            model.addAttribute("error", e.getMessage());
        } catch (EnlaceInvalidoException e) {
            model.addAttribute("vigente", false);
        }
        return "restablecer";
    }
}
