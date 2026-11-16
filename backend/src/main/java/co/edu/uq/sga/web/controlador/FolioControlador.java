package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioFolio;
import co.edu.uq.sga.servicio.comandos.DatosMovimiento;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.web.dto.Dtos.FolioDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.Acceso;
import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/{codigo}/folio")
public class FolioControlador {

    private static final String SESION = InterceptorAutenticacion.ATRIBUTO_SESION;

    private final ServicioFolio servicio;
    private final Mapeador mapeador;

    public FolioControlador(ServicioFolio servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    private void verificar(Sesion sesion, String codigo) {
        Reserva r = servicio.reserva(codigo);
        Acceso.exigirPropietarioOPersonal(sesion, r.getTitularDocumento());
    }

    @GetMapping
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public FolioDto folio(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        verificar(sesion, codigo);
        return mapeador.folio(servicio.reserva(codigo));
    }

    @PostMapping("/cargos")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public FolioDto cargo(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo,
                          @Valid @RequestBody DatosMovimiento datos) {
        servicio.registrarCargo(codigo, datos, sesion.usuario());
        return mapeador.folio(servicio.reserva(codigo));
    }

    /** Recepción registra pagos; el huésped también puede abonar a su propio folio desde el portal. */
    @PostMapping("/pagos")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public FolioDto pago(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo,
                         @Valid @RequestBody DatosMovimiento datos) {
        verificar(sesion, codigo);
        servicio.registrarPago(codigo, datos, sesion.usuario());
        return mapeador.folio(servicio.reserva(codigo));
    }

    @PostMapping("/anticipo")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public FolioDto anticipo(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        verificar(sesion, codigo);
        servicio.pagarAnticipo(codigo, sesion.usuario());
        return mapeador.folio(servicio.reserva(codigo));
    }

    @PostMapping("/deshacer")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public FolioDto deshacer(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        servicio.deshacer(codigo, sesion.usuario());
        return mapeador.folio(servicio.reserva(codigo));
    }
}
