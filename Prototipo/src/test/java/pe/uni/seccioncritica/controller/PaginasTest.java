package pe.uni.seccioncritica.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

    @Test
    void paginasPrincipalesSeRenderizan() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Ana Torres")));
        for (long estudiante = 1; estudiante <= 3; estudiante++) {
            mvc.perform(get("/oferta").param("estudiante", "" + estudiante))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Oferta académica")));
            mvc.perform(get("/matriculas").param("estudiante", "" + estudiante))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Agenda semanal")));
        }
    }

    @Test
    void recorridoDeLaDemostracion() throws Exception {
        mvc.perform(post("/matricular").param("estudiante", "1").param("seccion", "4"))
                .andExpect(redirectedUrl("/oferta?estudiante=1"))
                .andExpect(flash().attribute("exito", containsString("Matrícula confirmada")));
        mvc.perform(get("/matriculas").param("estudiante", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Construcción de software")))
                .andExpect(content().string(containsString("Miércoles · 14:00–16:00")));

        mvc.perform(post("/matricular").param("estudiante", "2").param("seccion", "4"))
                .andExpect(redirectedUrl("/oferta?estudiante=2"))
                .andExpect(flash().attribute("error", containsString("no tiene vacantes")));
        mvc.perform(get("/oferta").param("estudiante", "2"))
                .andExpect(content().string(containsString("Sin vacantes")));

        mvc.perform(post("/retirar").param("estudiante", "1").param("seccion", "4"))
                .andExpect(redirectedUrl("/matriculas?estudiante=1"))
                .andExpect(flash().attribute("exito", containsString("Retiro realizado")));
        mvc.perform(post("/retirar").param("estudiante", "1").param("seccion", "4"))
                .andExpect(flash().attribute("error", containsString("ya no está activa")));
    }

    @Test
    void elDialogoDeErrorMuestraElMotivo() throws Exception {
        mvc.perform(get("/oferta").param("estudiante", "2").flashAttr("error", "Motivo de prueba"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motivo de prueba")));
    }

    @Test
    void estudianteInexistenteDa404Amigable() throws Exception {
        mvc.perform(get("/oferta").param("estudiante", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No se encontró el estudiante")));
        mvc.perform(post("/matricular").param("estudiante", "1").param("seccion", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enlacesMalFormadosNoRompenLaPagina() throws Exception {
        mvc.perform(get("/oferta")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Volver al inicio")));
        mvc.perform(get("/oferta").param("estudiante", "abc")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Volver al inicio")));
        mvc.perform(post("/matricular").param("estudiante", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/matricular")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/no-existe")).andExpect(status().isNotFound())
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
