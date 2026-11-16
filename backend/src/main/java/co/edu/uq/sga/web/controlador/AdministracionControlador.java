package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.servicio.ServicioEscenario;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/admin")
@RequiereRol(Rol.ADMINISTRADOR)
public class AdministracionControlador {

    private final ServicioEscenario escenario;

    public AdministracionControlador(ServicioEscenario escenario) {
        this.escenario = escenario;
    }

    /** Botón "cargar escenario de prueba". */
    @PostMapping("/escenario")
    public MensajeDto cargarPredeterminado() {
        return new MensajeDto(escenario.cargarPredeterminado());
    }

    /** Carga un escenario desde un archivo plano JSON subido por el administrador. */
    @PostMapping("/escenario/archivo")
    public MensajeDto cargarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo está vacío");
        }
        try (InputStream in = archivo.getInputStream()) {
            return new MensajeDto(escenario.cargar(in));
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo");
        }
    }
}
