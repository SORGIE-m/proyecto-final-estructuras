package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.repositorio.RepositorioEmpleados;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEmpleadosArchivo implements RepositorioEmpleados {

    private final AlmacenEstado almacen;

    public RepositorioEmpleadosArchivo(AlmacenEstado almacen) {
        this.almacen = almacen;
    }

    @Override
    public Empleado buscar(String documento) {
        if (documento == null) {
            return null;
        }
        String clave = documento.trim();
        return arbol().buscarPor(e -> clave.compareTo(e.getDocumento()));
    }

    @Override
    public Empleado buscarPorUsuario(String usuario) {
        if (usuario == null) {
            return null;
        }
        String u = usuario.trim().toLowerCase();
        for (Empleado e : arbol()) {
            if (e.getUsuario().equals(u)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public void guardar(Empleado empleado) {
        arbol().insertar(empleado);
    }

    @Override
    public boolean eliminar(String documento) {
        Empleado e = buscar(documento);
        return e != null && arbol().eliminar(e);
    }

    @Override
    public ArbolBinarioBusqueda<Empleado> arbol() {
        return almacen.actual().empleados;
    }
}
