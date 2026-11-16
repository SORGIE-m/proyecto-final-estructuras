package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.ServicioTemporadas;
import co.edu.uq.sga.servicio.comandos.DatosTemporada;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temporadas")
public class TemporadaControlador {

    private final ServicioTemporadas servicio;

    public TemporadaControlador(ServicioTemporadas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Temporada[] listar() {
        ListaDoble<Temporada> lista = servicio.listar();
        return lista.flujo().toArray(Temporada[]::new);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol(Rol.ADMINISTRADOR)
    public Temporada crear(@Valid @RequestBody DatosTemporada datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Temporada actualizar(@PathVariable String id, @Valid @RequestBody DatosTemporada datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MensajeDto eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return new MensajeDto("Temporada eliminada");
    }
}
