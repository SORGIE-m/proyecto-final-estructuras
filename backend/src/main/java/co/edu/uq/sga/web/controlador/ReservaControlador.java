package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.negocio.Reembolso;
import co.edu.uq.sga.servicio.ServicioGrupos;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.SolicitudGrupo;
import co.edu.uq.sga.servicio.comandos.SolicitudReserva;
import co.edu.uq.sga.servicio.comandos.SolicitudReservaDirecta;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.soporte.Reloj;
import co.edu.uq.sga.web.dto.Dtos.CancelacionDto;
import co.edu.uq.sga.web.dto.Dtos.ReservaDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.Acceso;
import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservas")
public class ReservaControlador {

    private static final String SESION = InterceptorAutenticacion.ATRIBUTO_SESION;

    private final ServicioReservas servicio;
    private final ServicioGrupos grupos;
    private final Mapeador mapeador;
    private final Reloj reloj;

    public ReservaControlador(ServicioReservas servicio, ServicioGrupos grupos, Mapeador mapeador, Reloj reloj) {
        this.servicio = servicio;
        this.grupos = grupos;
        this.mapeador = mapeador;
        this.reloj = reloj;
    }

    @PostMapping("/portal")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol(Rol.HUESPED)
    public ReservaDto crearPortal(@RequestAttribute(SESION) Sesion sesion, @Valid @RequestBody SolicitudReserva s) {
        return mapeador.reserva(servicio.crearPortal(sesion.identificador(), s));
    }

    @PostMapping("/directa")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto crearDirecta(@Valid @RequestBody SolicitudReservaDirecta s) {
        return mapeador.reserva(servicio.crearDirecta(s));
    }

    @PostMapping("/grupo")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] crearGrupo(@Valid @RequestBody SolicitudGrupo s) {
        return mapeador.reservas(grupos.reservarGrupo(s));
    }

    @GetMapping
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] listar(@RequestParam(required = false) EstadoReserva estado,
                               @RequestParam(required = false) Canal canal,
                               @RequestParam(required = false) String texto) {
        return mapeador.reservas(servicio.listar(estado, canal, texto));
    }

    @GetMapping("/llegadas")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] llegadas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return mapeador.reservas(servicio.llegadasDe(fecha == null ? reloj.hoy() : fecha));
    }

    @GetMapping("/salidas")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] salidas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return mapeador.reservas(servicio.salidasDe(fecha == null ? reloj.hoy() : fecha));
    }

    @GetMapping("/por-vencer")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] porVencer() {
        return mapeador.reservas(servicio.pendientesPorVencer());
    }

    @PostMapping("/vencer")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] vencer() {
        return mapeador.reservas(servicio.vencerPendientes());
    }

    @GetMapping("/{codigo}")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto obtener(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        return mapeador.reserva(propia(sesion, codigo));
    }

    private Reserva propia(Sesion sesion, String codigo) {
        Reserva r = servicio.obtener(codigo);
        Acceso.exigirPropietarioOPersonal(sesion, r.getTitularDocumento());
        return r;
    }

    @PostMapping("/{codigo}/confirmar")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto confirmar(@PathVariable String codigo) {
        return mapeador.reserva(servicio.confirmar(codigo));
    }

    @GetMapping("/{codigo}/cancelacion")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public Reembolso previsualizar(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        propia(sesion, codigo);
        return servicio.previsualizarCancelacion(codigo);
    }

    @PostMapping("/{codigo}/cancelar")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto cancelar(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo,
                               @RequestBody(required = false) CancelacionDto cuerpo) {
        propia(sesion, codigo);
        return mapeador.reserva(servicio.cancelar(codigo, cuerpo == null ? null : cuerpo.motivo(), sesion.usuario()));
    }

    @PostMapping("/{codigo}/check-in")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto checkIn(@PathVariable String codigo) {
        return mapeador.reserva(servicio.checkIn(codigo));
    }

    @PostMapping("/{codigo}/check-out")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto checkOut(@PathVariable String codigo) {
        return mapeador.reserva(servicio.checkOut(codigo));
    }

    @PostMapping("/{codigo}/no-show")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto noShow(@PathVariable String codigo) {
        return mapeador.reserva(servicio.marcarNoShow(codigo));
    }
}
