package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioEmpleados;
import co.edu.uq.sga.servicio.comandos.DatosEmpleado;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.web.dto.Dtos.EmpleadoDto;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.dto.Dtos.TurnoDto;
import co.edu.uq.sga.web.dto.Mapeador;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoControlador {

    private final ServicioEmpleados servicio;
    private final Mapeador mapeador;

    public EmpleadoControlador(ServicioEmpleados servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    @RequiereRol(Rol.ADMINISTRADOR)
    public EmpleadoDto[] listar() {
        return Mapeador.arreglo(servicio.listar(), mapeador::empleado, EmpleadoDto[]::new);
    }

    @GetMapping("/{documento}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public EmpleadoDto obtener(@PathVariable String documento) {
        return mapeador.empleado(servicio.obtener(documento));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol(Rol.ADMINISTRADOR)
    public EmpleadoDto crear(@Valid @RequestBody DatosEmpleado datos) {
        return mapeador.empleado(servicio.crear(datos));
    }

    @PutMapping("/{documento}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public EmpleadoDto actualizar(@PathVariable String documento, @Valid @RequestBody DatosEmpleado datos) {
        return mapeador.empleado(servicio.actualizar(documento, datos));
    }

    @DeleteMapping("/{documento}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MensajeDto eliminar(@RequestAttribute(InterceptorAutenticacion.ATRIBUTO_SESION) Sesion sesion,
                               @PathVariable String documento) {
        if (sesion.identificador().equals(documento)) {
            throw new ReglaNegocioException("No puede eliminar su propia cuenta");
        }
        servicio.eliminar(documento);
        return new MensajeDto("Empleado " + documento + " eliminado");
    }

    @GetMapping("/turno")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public TurnoDto turno() {
        return mapeador.turno(servicio.turnoActual());
    }

    @PostMapping("/turno/rotar")
    @RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public TurnoDto rotar() {
        return mapeador.turno(servicio.rotarTurno());
    }
}
