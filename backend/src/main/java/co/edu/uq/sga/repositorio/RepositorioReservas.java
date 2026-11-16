package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.Diccionario;

import java.util.function.Supplier;

/** Índice global de reservas por código y cola de vencimiento de pendientes. */
public interface RepositorioReservas {
    Reserva buscar(String codigo);

    void guardar(Reserva reserva);

    Diccionario<String, Reserva> todas();

    String siguienteCodigo();

    /** Devuelve la cola de vencimientos, construyéndola con el proveedor si aún no existe. */
    ColaPrioridad<Reserva> colaVencimientos(Supplier<ColaPrioridad<Reserva>> constructor);
}
