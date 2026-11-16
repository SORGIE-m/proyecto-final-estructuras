package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioHuespedesArchivo implements RepositorioHuespedes {

    private final AlmacenEstado almacen;

    public RepositorioHuespedesArchivo(AlmacenEstado almacen) {
        this.almacen = almacen;
    }

    @Override
    public Huesped buscar(String documento) {
        if (documento == null) {
            return null;
        }
        String clave = documento.trim();
        return arbol().buscarPor(h -> clave.compareTo(h.getDocumento()));
    }

    @Override
    public Huesped buscarPorCorreo(String correo) {
        if (correo == null) {
            return null;
        }
        String c = correo.trim().toLowerCase();
        for (Huesped h : arbol()) {
            if (h.getCorreo().equals(c)) {
                return h;
            }
        }
        return null;
    }

    @Override
    public void guardar(Huesped huesped) {
        arbol().insertar(huesped);
    }

    @Override
    public boolean eliminar(String documento) {
        Huesped h = buscar(documento);
        return h != null && arbol().eliminar(h);
    }

    @Override
    public ArbolBinarioBusqueda<Huesped> arbol() {
        return almacen.actual().huespedes;
    }
}
