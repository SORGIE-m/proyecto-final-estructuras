package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.estructuras.ListaDoble;

public interface RepositorioTemporadas {
    ListaDoble<Temporada> todas();

    String siguienteId();
}
