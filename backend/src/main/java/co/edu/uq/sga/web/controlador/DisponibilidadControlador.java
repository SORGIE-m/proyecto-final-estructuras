package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.servicio.ServicioDisponibilidad;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.web.dto.Dtos.CotizacionDto;
import co.edu.uq.sga.web.dto.Dtos.CotizacionSolicitud;
import co.edu.uq.sga.web.dto.Dtos.OpcionDto;
import co.edu.uq.sga.web.dto.Dtos.PropuestaGrupoDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadControlador {

    private final ServicioDisponibilidad servicio;
    private final Mapeador mapeador;

    public DisponibilidadControlador(ServicioDisponibilidad servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    public OpcionDto[] buscar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
                              @RequestParam(defaultValue = "1") int personas,
                              @RequestParam(required = false) String orden) {
        return Mapeador.arreglo(servicio.buscar(entrada, salida, personas, orden), mapeador::opcion, OpcionDto[]::new);
    }

    /** Cotización rápida por cantidad de personas que pagan y menores exentos. */
    @GetMapping("/cotizacion")
    public CotizacionDto cotizar(@RequestParam String apartamento,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
                                 @RequestParam(defaultValue = "1") int adultos,
                                 @RequestParam(defaultValue = "0") int ninos) {
        return mapeador.cotizacion(apartamento.toUpperCase(),
                servicio.cotizar(apartamento, entrada, salida, adultos, ninos), entrada, salida);
    }

    /** Cotización exacta con las fechas de nacimiento de los ocupantes. */
    @PostMapping("/cotizacion")
    public CotizacionDto cotizarOcupantes(@RequestBody CotizacionSolicitud s) {
        if (s.apartamento() == null || s.ocupantes() == null || s.ocupantes().length == 0) {
            throw new ReglaNegocioException("Indique el apartamento y los ocupantes");
        }
        ListaDoble<Ocupante> ocupantes = new ListaDoble<>();
        for (DatosOcupante o : s.ocupantes()) {
            ocupantes.agregar(o.aDominio());
        }
        return mapeador.cotizacion(s.apartamento().toUpperCase(),
                servicio.cotizar(s.apartamento(), s.entrada(), s.salida(), ocupantes), s.entrada(), s.salida());
    }

    @GetMapping("/grupos")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public PropuestaGrupoDto grupo(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
                                   @RequestParam int personas) {
        return mapeador.propuesta(servicio.proponerGrupo(entrada, salida, personas));
    }
}
