package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Punto del mapa del edificio (vértice del grafo). La identidad es su id. */
public final class NodoEdificio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String nombre;
    private final TipoNodo tipo;
    private final String bloque;
    private final int piso;
    private final double x;
    private final double y;

    public NodoEdificio(String id, String nombre, TipoNodo tipo, String bloque, int piso, double x, double y) {
        if (id == null || id.isBlank()) {
            throw new ReglaNegocioException("El identificador del punto es obligatorio");
        }
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de punto es obligatorio");
        }
        this.id = id.trim().toUpperCase();
        this.nombre = nombre == null || nombre.isBlank() ? this.id : nombre.trim();
        this.tipo = tipo;
        this.bloque = bloque == null ? "" : bloque.trim().toUpperCase();
        this.piso = piso;
        this.x = x;
        this.y = y;
    }

    /** Nodo "muestra" para buscar en el grafo por id. */
    public static NodoEdificio referencia(String id) {
        return new NodoEdificio(id, id, TipoNodo.PASILLO, "", 0, 0, 0);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NodoEdificio n && id.equals(n.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }

    public String id() { return id; }
    public String nombre() { return nombre; }
    public TipoNodo tipo() { return tipo; }
    public String bloque() { return bloque; }
    public int piso() { return piso; }
    public double x() { return x; }
    public double y() { return y; }
}
