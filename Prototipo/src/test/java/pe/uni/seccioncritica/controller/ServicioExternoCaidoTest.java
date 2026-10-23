package pe.uni.seccioncritica.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Fallback de punta a punta: con el verificador caído se responde 503 y no se toca ningún cupo. */
@SpringBootTest(properties = "demo.verificacion-academica.modo=fallo")
@AutoConfigureMockMvc
class ServicioExternoCaidoTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reiniciar() {
        db.update("DELETE FROM matricula");
        db.update("UPDATE seccion SET cupo_disponible = cupo_maximo");
    }

    @Test
    void verificadorCaidoResponde503SinConsumirCupo() throws Exception {
        mvc.perform(post("/matricular").with(PaginasTest.sesion(1)).with(csrf()).param("seccion", "4"))
                .andExpect(status().isServiceUnavailable());
        assertEquals(1, db.queryForObject("SELECT cupo_disponible FROM seccion WHERE id = 4", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM matricula", Integer.class));
    }
}
