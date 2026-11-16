package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.comandos.DatosEmpleado;
import co.edu.uq.sga.servicio.resultados.TurnoRecepcion;

/** Gestión de empleados y rotación de turnos de recepción. */
public interface ServicioEmpleados {
    Empleado crear(DatosEmpleado datos);

    Empleado actualizar(String documento, DatosEmpleado datos);

    void eliminar(String documento);

    Empleado obtener(String documento);

    ListaDoble<Empleado> listar();

    TurnoRecepcion turnoActual();

    /** Pasa el turno al siguiente recepcionista de la lista circular. */
    TurnoRecepcion rotarTurno();
}
