package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.servicio.ServicioApartamentos;
import co.edu.uq.sga.servicio.comandos.DatosApartamento;
import co.edu.uq.sga.web.dto.Dtos.ApartamentoDto;
import co.edu.uq.sga.web.dto.Dtos.CambioEstadoApartamento;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartamentos")
public class ApartamentoControlador {

    private final ServicioApartamentos servicio;
    private final Mapeador mapeador;

    public ApartamentoControlador(ServicioApartamentos servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    public ApartamentoDto[] listar(@RequestParam(required = false) String orden) {
        return mapeador.apartamentos(servicio.listar(orden));
    }

    @GetMapping("/{codigo}")
    public ApartamentoDto obtener(@PathVariable String codigo) {
        return mapeador.apartamento(servicio.obtener(codigo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol(Rol.ADMINISTRADOR)
    public ApartamentoDto crear(@Valid @RequestBody DatosApartamento datos) {
        return mapeador.apartamento(servicio.crear(datos));
    }

    @PutMapping("/{codigo}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public ApartamentoDto actualizar(@PathVariable String codigo, @Valid @RequestBody DatosApartamento datos) {
        return mapeador.apartamento(servicio.actualizar(codigo, datos));
    }

    @DeleteMapping("/{codigo}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MensajeDto eliminar(@PathVariable String codigo) {
        servicio.eliminar(codigo);
        return new MensajeDto("Apartamento " + codigo.toUpperCase() + " eliminado");
    }

    @PatchMapping("/{codigo}/estado")
    @RequiereRol({Rol.ADMINISTRADOR, Rol.RECEPCIONISTA})
    public ApartamentoDto cambiarEstado(@PathVariable String codigo, @RequestBody CambioEstadoApartamento cambio) {
        EstadoApartamento destino;
        try {
            destino = EstadoApartamento.valueOf(cambio.estado());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ReglaNegocioException("Estado de apartamento no válido");
        }
        return mapeador.apartamento(servicio.cambiarEstado(codigo, destino, cambio.motivo()));
    }
}
