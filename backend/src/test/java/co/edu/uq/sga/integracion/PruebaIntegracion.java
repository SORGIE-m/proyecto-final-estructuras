package co.edu.uq.sga.integracion;

import co.edu.uq.sga.servicio.ServicioEscenario;
import co.edu.uq.sga.soporte.Reloj;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base de las pruebas de integración: contexto Spring real, reloj fijo (16/11/2026 09:00),
 * archivo de datos temporal y el escenario inicial recargado antes de cada prueba.
 */
@SpringBootTest(properties = {"sga.tareas.habilitadas=false", "sga.datos.escenario-si-vacio=false"})
@AutoConfigureMockMvc
@Import(PruebaIntegracion.Configuracion.class)
public abstract class PruebaIntegracion {

    private static final Path DIRECTORIO;

    static {
        try {
            DIRECTORIO = Files.createTempDirectory("sga-pruebas");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TestConfiguration
    static class Configuracion {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("sga.datos.archivo", () -> DIRECTORIO.resolve("sga.dat").toString());
    }

    @Autowired
    protected ServicioEscenario escenario;

    @Autowired
    protected Reloj reloj;

    protected RelojAjustable relojAjustable() {
        return (RelojAjustable) reloj;
    }

    @BeforeEach
    void cargarEscenario() {
        relojAjustable().reiniciar();
        escenario.cargarPredeterminado();
    }
}
