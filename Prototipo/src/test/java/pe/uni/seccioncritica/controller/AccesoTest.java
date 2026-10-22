package pe.uni.seccioncritica.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
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

/** Inicio de sesión, protección de páginas y recuperación de contraseña de punta a punta. */
@SpringBootTest
@AutoConfigureMockMvc
class AccesoTest {
    // Hash BCrypt de "Matricula2026", el mismo de data.sql.
    static final String CLAVE_DEMO = "$2a$10$14j6fa5SqAJ0aXOGDQDKzO.Q/w7Wd7xHgiSMvudW3QKyLQggKh5Hy";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void reiniciar() {
        db.update("DELETE FROM recuperacion");
        db.update("UPDATE cuenta SET clave_hash = ?", CLAVE_DEMO);
    }

    @Test
    void sinSesionTodoLlevaAlLogin() throws Exception {
        for (var ruta : new String[]{"/", "/oferta", "/matriculas"})
            mvc.perform(get(ruta)).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/matricular").with(csrf()).param("seccion", "4"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void laPantallaDeLoginSeRenderiza() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")))
                .andExpect(content().string(containsString("¿Olvidaste tu contraseña?")))
                .andExpect(content().string(containsString("20230001A")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginCorrectoEntraALaOferta() throws Exception {
        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("20230001A").password("Matricula2026"))
                .andExpect(authenticated().withUsername("20230001A"))
                .andExpect(redirectedUrl("/oferta"));
        // El código no distingue mayúsculas.
        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("20230002b").password("Matricula2026"))
                .andExpect(authenticated());
    }

    @Test
    void loginIncorrectoNoRevelaQueFallo() throws Exception {
        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("20230001A").password("otra-clave"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("NOEXISTE").password("Matricula2026"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Código o contraseña incorrectos.")));
    }

    @Test
    void cerrarSesionVuelveAlLogin() throws Exception {
        mvc.perform(logout()).andExpect(redirectedUrl("/login?salida")).andExpect(unauthenticated());
    }

    @Test
    void sinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/login").param("codigo", "20230001A").param("clave", "Matricula2026"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/recuperar").param("codigo", "20230001A")).andExpect(status().isForbidden());
    }

    @Test
    void recuperacionCompleta() throws Exception {
        var enlace = solicitarEnlace("20230002B");
        var token = enlace.substring(enlace.indexOf("token=") + 6);

        mvc.perform(get("/restablecer").param("token", token))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Crea una contraseña nueva")));

        mvc.perform(post("/restablecer").with(csrf()).param("token", token)
                        .param("clave", "NuevaClave1").param("confirmacion", "OtraClave1"))
                .andExpect(content().string(containsString("Las contraseñas no coinciden.")));
        mvc.perform(post("/restablecer").with(csrf()).param("token", token)
                        .param("clave", "corta1").param("confirmacion", "corta1"))
                .andExpect(content().string(containsString("al menos 8 caracteres")));
        mvc.perform(post("/restablecer").with(csrf()).param("token", token)
                        .param("clave", "solamenteletras").param("confirmacion", "solamenteletras"))
                .andExpect(content().string(containsString("letras y números")));

        mvc.perform(post("/restablecer").with(csrf()).param("token", token)
                        .param("clave", "NuevaClave1").param("confirmacion", "NuevaClave1"))
                .andExpect(redirectedUrl("/login?restablecida"));

        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("20230002B").password("NuevaClave1"))
                .andExpect(authenticated());
        mvc.perform(formLogin("/login").userParameter("codigo").passwordParam("clave")
                        .user("20230002B").password("Matricula2026"))
                .andExpect(unauthenticated());

        // El enlace es de un solo uso.
        mvc.perform(post("/restablecer").with(csrf()).param("token", token)
                        .param("clave", "OtraClave2").param("confirmacion", "OtraClave2"))
                .andExpect(content().string(containsString("El enlace ya no es válido")));
    }

    @Test
    void codigoInexistenteRecibeLaMismaRespuesta() throws Exception {
        mvc.perform(post("/recuperar").with(csrf()).param("codigo", "NOEXISTE"))
                .andExpect(redirectedUrl("/recuperar"))
                .andExpect(flash().attribute("enviado", true))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void unEnlaceNuevoAnulaElAnterior() throws Exception {
        var primero = solicitarEnlace("20230001A");
        var segundo = solicitarEnlace("20230001A");
        mvc.perform(get(primero.substring(primero.indexOf("/restablecer"))))
                .andExpect(content().string(containsString("El enlace ya no es válido")));
        mvc.perform(get(segundo.substring(segundo.indexOf("/restablecer"))))
                .andExpect(content().string(containsString("Crea una contraseña nueva")));
    }

    @Test
    void enlaceVencidoOInventadoNoSirve() throws Exception {
        var enlace = solicitarEnlace("20230003C");
        db.update("UPDATE recuperacion SET expira = expira - INTERVAL '31' MINUTE");
        mvc.perform(get(enlace.substring(enlace.indexOf("/restablecer"))))
                .andExpect(content().string(containsString("El enlace ya no es válido")))
                .andExpect(content().string(not(containsString("Guardar contraseña"))));
        mvc.perform(get("/restablecer").param("token", "inventado"))
                .andExpect(content().string(containsString("El enlace ya no es válido")));
        mvc.perform(get("/restablecer")).andExpect(status().isOk())
                .andExpect(content().string(containsString("El enlace ya no es válido")));
    }

    private String solicitarEnlace(String codigo) throws Exception {
        var flash = mvc.perform(post("/recuperar").with(csrf()).param("codigo", codigo))
                .andExpect(redirectedUrl("/recuperar"))
                .andExpect(flash().attribute("enviado", true))
                .andReturn().getFlashMap();
        return (String) flash.get("enlaceDemo");
    }
}
