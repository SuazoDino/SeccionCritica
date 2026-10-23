package pe.uni.seccioncritica.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.uni.seccioncritica.seguridad.UsuarioSesion;

/** Recorre la demo completa y los enlaces mal formados: ninguna respuesta debe ser un 500. */
@SpringBootTest
@AutoConfigureMockMvc
class PaginasTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reiniciar() {
        db.update("DELETE FROM matricula");
        db.update("UPDATE seccion SET cupo_disponible = cupo_maximo");
    }

    /** Sesión del estudiante indicado, tal como la crea el inicio de sesión. */
    static RequestPostProcessor sesion(long estudianteId) {
        return user(new UsuarioSesion(estudianteId, "codigo-" + estudianteId, "sin-clave"));
    }

    @Test
    void paginasPrincipalesSeRenderizan() throws Exception {
        mvc.perform(get("/").with(sesion(1))).andExpect(redirectedUrl("/oferta"));
        for (long estudiante = 1; estudiante <= 3; estudiante++) {
            mvc.perform(get("/oferta").with(sesion(estudiante)))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Oferta académica")))
                    .andExpect(content().string(containsString("Cerrar sesión")));
            mvc.perform(get("/matriculas").with(sesion(estudiante)))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Agenda semanal")));
        }
    }

    @Test
    void recorridoDeLaDemostracion() throws Exception {
        mvc.perform(post("/matricular").with(sesion(1)).with(csrf()).param("seccion", "4"))
                .andExpect(redirectedUrl("/oferta"))
                .andExpect(flash().attribute("exito", containsString("Matrícula confirmada")));
        mvc.perform(get("/matriculas").with(sesion(1)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Construcción de software")))
                .andExpect(content().string(containsString("Miércoles · 14:00–16:00")));

        mvc.perform(post("/matricular").with(sesion(2)).with(csrf()).param("seccion", "4"))
                .andExpect(redirectedUrl("/oferta"))
                .andExpect(flash().attribute("error", containsString("no tiene vacantes")));
        mvc.perform(get("/oferta").with(sesion(2)))
                .andExpect(content().string(containsString("Sin vacantes")));

        mvc.perform(post("/retirar").with(sesion(1)).with(csrf()).param("seccion", "4"))
                .andExpect(redirectedUrl("/matriculas"))
                .andExpect(flash().attribute("exito", containsString("Retiro realizado")));
        mvc.perform(post("/retirar").with(sesion(1)).with(csrf()).param("seccion", "4"))
                .andExpect(flash().attribute("error", containsString("ya no está activa")));
    }

    @Test
    void elEstudianteSaleDeLaSesionNoDeLaUrl() throws Exception {
        // Un parámetro "estudiante" sobrante se ignora: Luis no puede matricular a Ana.
        mvc.perform(post("/matricular").with(sesion(2)).with(csrf()).param("estudiante", "1").param("seccion", "1"))
                .andExpect(redirectedUrl("/oferta"));
        mvc.perform(get("/matriculas").with(sesion(1)))
                .andExpect(content().string(not(containsString("Programación I"))));
    }

    @Test
    void elDialogoDeErrorMuestraElMotivo() throws Exception {
        mvc.perform(get("/oferta").with(sesion(2)).flashAttr("error", "Motivo de prueba"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motivo de prueba")));
    }

    @Test
    void estudianteInexistenteDa404Amigable() throws Exception {
        mvc.perform(get("/oferta").with(sesion(999)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No se encontró el estudiante")));
        mvc.perform(post("/matricular").with(sesion(1)).with(csrf()).param("seccion", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void identificadoresNegativosYParametrosAusentesSonControlados() throws Exception {
        // Sin sesión no se llega al panel: el estudiante sale de la sesión, no de un parámetro manipulable.
        mvc.perform(get("/oferta").param("estudiante", "-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/retirar").with(sesion(1)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))));
        mvc.perform(post("/retirar").with(sesion(1)).with(csrf()).param("seccion", "-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void enlacesMalFormadosNoRompenLaPagina() throws Exception {
        mvc.perform(post("/matricular").with(sesion(1)).with(csrf()).param("seccion", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Volver al inicio")));
        mvc.perform(post("/matricular").with(sesion(1)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(get("/matricular").with(sesion(1))).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/no-existe").with(sesion(1))).andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void recursosEstaticosYSaludDisponibles() throws Exception {
        mvc.perform(get("/app.css")).andExpect(status().isOk());
        mvc.perform(get("/matricula.css")).andExpect(status().isOk());
        mvc.perform(get("/matricula.js")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(content().string(containsString("UP")));
    }
}
