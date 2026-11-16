package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.comandos.DatosHuesped;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.web.dto.Dtos.ApartamentoDto;
import co.edu.uq.sga.web.dto.Dtos.HuespedDto;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.dto.Dtos.ReservaDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.Acceso;
import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/huespedes")
public class HuespedControlador {

    private static final String SESION = InterceptorAutenticacion.ATRIBUTO_SESION;

    private final ServicioHuespedes servicio;
    private final Mapeador mapeador;

    public HuespedControlador(ServicioHuespedes servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public HuespedDto[] listar(@RequestParam(required = false) String texto) {
        return Mapeador.arreglo(servicio.listar(texto), mapeador::huesped, HuespedDto[]::new);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public HuespedDto crear(@Valid @RequestBody DatosHuesped datos) {
        return mapeador.huesped(servicio.registrar(datos));
    }

    @GetMapping("/{documento}")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public HuespedDto obtener(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        return mapeador.huesped(servicio.obtener(documento));
    }

    @PutMapping("/{documento}")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public HuespedDto actualizar(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento,
                                 @Valid @RequestBody DatosHuesped datos) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        return mapeador.huesped(servicio.actualizar(documento, datos));
    }

    @DeleteMapping("/{documento}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MensajeDto eliminar(@PathVariable String documento) {
        servicio.eliminar(documento);
        return new MensajeDto("Huésped " + documento + " eliminado");
    }

    @GetMapping("/{documento}/favoritos")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ApartamentoDto[] favoritos(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        return mapeador.apartamentos(servicio.favoritos(documento));
    }

    @PostMapping("/{documento}/favoritos/{codigo}")
    @RequiereRol(Rol.HUESPED)
    public ApartamentoDto[] agregarFavorito(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento,
                                            @PathVariable String codigo) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        servicio.agregarFavorito(documento, codigo);
        return mapeador.apartamentos(servicio.favoritos(documento));
    }

    @DeleteMapping("/{documento}/favoritos/{codigo}")
    @RequiereRol(Rol.HUESPED)
    public ApartamentoDto[] quitarFavorito(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento,
                                           @PathVariable String codigo) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        servicio.quitarFavorito(documento, codigo);
        return mapeador.apartamentos(servicio.favoritos(documento));
    }

    @GetMapping("/{documento}/historial")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] historial(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        return mapeador.reservas(servicio.historial(documento));
    }

    @GetMapping("/{documento}/reservas")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public ReservaDto[] reservas(@RequestAttribute(SESION) Sesion sesion, @PathVariable String documento) {
        Acceso.exigirPropietarioOPersonal(sesion, documento);
        return mapeador.reservas(servicio.reservas(documento));
    }
}
