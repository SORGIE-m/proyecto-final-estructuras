package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.estructuras.Grafo;

public interface RepositorioMapa {
    Grafo<NodoEdificio> grafo();
}
