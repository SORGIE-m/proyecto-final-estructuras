package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioCanalExterno;
import co.edu.uq.sga.servicio.comandos.DatosSolicitudExterna;
import co.edu.uq.sga.web.dto.Dtos.ProcesamientoDto;
import co.edu.uq.sga.web.dto.Dtos.SolicitudExternaDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/canal-externo")
@RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
public class CanalExternoControlador {

    private final ServicioCanalExterno servicio;
    private final Mapeador mapeador;

    public CanalExternoControlador(ServicioCanalExterno servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping("/pendientes")
    public SolicitudExternaDto[] pendientes() {
        return Mapeador.arreglo(servicio.pendientes(), mapeador::solicitud, SolicitudExternaDto[]::new);
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudExternaDto recibir(@Valid @RequestBody DatosSolicitudExterna datos) {
        return mapeador.solicitud(servicio.recibir(datos));
    }

    @PostMapping("/simular")
    public SolicitudExternaDto[] simular(@RequestParam(defaultValue = "3") int cantidad) {
        return Mapeador.arreglo(servicio.simular(cantidad), mapeador::solicitud, SolicitudExternaDto[]::new);
    }

    @PostMapping("/procesar-siguiente")
    public ProcesamientoDto procesarSiguiente() {
        return mapeador.procesamiento(servicio.procesarSiguiente());
    }

    @PostMapping("/procesar-todas")
    public ProcesamientoDto[] procesarTodas() {
        return Mapeador.arreglo(servicio.procesarTodas(), mapeador::procesamiento, ProcesamientoDto[]::new);
    }

    @GetMapping("/historial")
    public ProcesamientoDto[] historial() {
        return Mapeador.arreglo(servicio.historial(), mapeador::procesamiento, ProcesamientoDto[]::new);
    }
}
