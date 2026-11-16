package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;

/** Huéspedes indexados por documento en un árbol binario de búsqueda. */
public interface RepositorioHuespedes {
    Huesped buscar(String documento);

    Huesped buscarPorCorreo(String correo);

    void guardar(Huesped huesped);

    boolean eliminar(String documento);

    ArbolBinarioBusqueda<Huesped> arbol();
}
