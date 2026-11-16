package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.estructuras.ListaCircular;

/** Rotación de turnos de recepción (documentos de los recepcionistas). */
public interface RepositorioTurnos {
    ListaCircular<String> turnos();
}
