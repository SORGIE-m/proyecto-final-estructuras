package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.estructuras.ListaDoble;

/** Turno vigente de recepción y los que siguen en la rotación circular. */
public record TurnoRecepcion(Empleado actual, String franja, ListaDoble<Empleado> siguientes) {
}
