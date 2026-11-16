package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.resultados.RutaPreparacion;

/** Cola de preparación inteligente y rutas del personal. */
public interface ServicioPreparacion {
    /** Agrega a la cola un apartamento que quedó Pendiente de preparación. */
    void encolar(String codigoApartamento);

    void retirar(String codigoApartamento);

    /** Cola en orden de prioridad (prioridades recalculadas para el día de hoy). */
    ListaDoble<ItemPreparacion> cola();

    /** Toma el apartamento más prioritario y lo pasa a En preparación. */
    ItemPreparacion despacharSiguiente(String responsable);

    /** Inicia la preparación de un apartamento concreto de la cola. */
    Apartamento iniciar(String codigoApartamento, String responsable);

    /** Termina la preparación: el apartamento queda Preparado. */
    Apartamento finalizar(String codigoApartamento, String responsable);

    ListaDoble<Apartamento> enPreparacion();

    /** @param algoritmo DIJKSTRA (menor distancia) o BFS (menos tramos). */
    RutaPreparacion ruta(String algoritmo, String origen);
}
