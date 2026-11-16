package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioApartamentosArchivo implements RepositorioApartamentos {

    private final AlmacenEstado almacen;

    public RepositorioApartamentosArchivo(AlmacenEstado almacen) {
        this.almacen = almacen;
    }

    @Override
    public Apartamento buscar(String codigo) {
        if (codigo == null) {
            return null;
        }
        String clave = codigo.trim().toUpperCase();
        return arbol().buscarPor(a -> clave.compareTo(a.getCodigo()));
    }

    @Override
    public void guardar(Apartamento apartamento) {
        arbol().insertar(apartamento);
    }

    @Override
    public boolean eliminar(String codigo) {
        Apartamento a = buscar(codigo);
        return a != null && arbol().eliminar(a);
    }

    @Override
    public ArbolBinarioBusqueda<Apartamento> arbol() {
        return almacen.actual().apartamentos;
    }
}
