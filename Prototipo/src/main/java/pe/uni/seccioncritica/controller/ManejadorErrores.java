package pe.uni.seccioncritica.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import pe.uni.seccioncritica.exception.RecursoNoEncontradoException;

/** Ninguna excepción llega al usuario como traza: todas terminan en la página error.html con un mensaje claro. */
@ControllerAdvice
public class ManejadorErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView noEncontrado(RecursoNoEncontradoException e) {
        log.warn("Recurso inexistente: {}", e.getMessage());
        return pagina(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView parametroInvalido(MethodArgumentTypeMismatchException e) {
        return pagina(HttpStatus.BAD_REQUEST, "El enlace no es válido. Vuelve al inicio e intenta nuevamente.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView baseDeDatos(DataAccessException e) {
        log.error("Falla de acceso a datos", e);
        return pagina(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio está ocupado o la base de datos no responde. Intenta nuevamente en unos segundos.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView inesperado(Exception e) {
        // Las excepciones propias de Spring MVC (404 de recursos, 405, parámetros faltantes) conservan su código.
        if (e instanceof ErrorResponse respuesta) {
            HttpStatusCode estado = respuesta.getStatusCode();
            return pagina(estado, estado.value() == 404
                    ? "La página que buscas no existe."
                    : "La solicitud no es válida. Vuelve al inicio e intenta nuevamente.");
        }
        log.error("Error inesperado", e);
        return pagina(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Vuelve al inicio e intenta nuevamente.");
    }

    private static ModelAndView pagina(HttpStatusCode estado, String mensaje) {
        var vista = new ModelAndView("error", estado);
        vista.addObject("mensaje", mensaje);
        return vista;
    }
}
