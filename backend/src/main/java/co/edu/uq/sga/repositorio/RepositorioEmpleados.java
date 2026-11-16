package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;

/** Empleados indexados por documento en un árbol binario de búsqueda. */
public interface RepositorioEmpleados {
    Empleado buscar(String documento);

    Empleado buscarPorUsuario(String usuario);

    void guardar(Empleado empleado);

    boolean eliminar(String documento);

    ArbolBinarioBusqueda<Empleado> arbol();
}
