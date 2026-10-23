package pe.uni.seccioncritica.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeccionTest {
    @Test
    void aceptaCuposDentroDelRango() {
        assertDoesNotThrow(() -> new Seccion(1, 1, "A", 30, 30));
        assertTrue(new Seccion(1, 1, "A", 30, 0).agotada());
    }

    @Test
    void rechazaEstadosQueRompenElInvarianteDeCupos() {
        assertThrows(IllegalStateException.class, () -> new Seccion(1, 1, "A", 30, -1));
        assertThrows(IllegalStateException.class, () -> new Seccion(1, 1, "A", 30, 31));
        assertThrows(IllegalStateException.class, () -> new Seccion(1, 1, "A", 0, 0));
    }
}
