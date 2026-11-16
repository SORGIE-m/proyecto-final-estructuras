package co.edu.uq.sga.estructuras;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;

/**
 * Diccionario clave-valor construido sobre el ArbolBinarioBusqueda propio: cada entrada se
 * compara por su clave. Permite indexar, por ejemplo, reservas por su código.
 */
public class Diccionario<K extends Comparable<K>, V> implements Coleccion<V>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final class Entrada<K extends Comparable<K>, V>
            implements Comparable<Entrada<K, V>>, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final K clave;
        private final V valor;

        public Entrada(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }

        public K clave() {
            return clave;
        }

        public V valor() {
            return valor;
        }

        @Override
        public int compareTo(Entrada<K, V> o) {
            return clave.compareTo(o.clave);
        }
    }

    private final ArbolBinarioBusqueda<Entrada<K, V>> arbol = new ArbolBinarioBusqueda<>();

    public void poner(K clave, V valor) {
        arbol.insertar(new Entrada<>(clave, valor));
    }

    public V obtener(K clave) {
        Entrada<K, V> e = arbol.buscarPor(n -> clave.compareTo(n.clave));
        return e == null ? null : e.valor;
    }

    public boolean contieneClave(K clave) {
        return obtener(clave) != null;
    }

    public boolean eliminar(K clave) {
        return arbol.eliminar(new Entrada<>(clave, null));
    }

    @Override
    public int tamanio() {
        return arbol.tamanio();
    }

    /** Itera los valores en orden ascendente de clave. */
    @Override
    public Iterator<V> iterator() {
        Iterator<Entrada<K, V>> it = arbol.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public V next() {
                return it.next().valor;
            }
        };
    }

    public Iterable<Entrada<K, V>> entradas() {
        return arbol;
    }
}
