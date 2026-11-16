package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;

/** Catálogo de apartamentos indexado por código en un árbol binario de búsqueda. */
public interface RepositorioApartamentos {
    Apartamento buscar(String codigo);

    void guardar(Apartamento apartamento);

    boolean eliminar(String codigo);

    ArbolBinarioBusqueda<Apartamento> arbol();
}
