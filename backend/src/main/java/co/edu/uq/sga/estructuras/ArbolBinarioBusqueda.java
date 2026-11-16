package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Árbol binario de búsqueda genérico ordenado por el orden natural de T (Comparable).
 * Inserción, búsqueda, eliminación, altura y recorridos son recursivos.
 * No admite duplicados: insertar un elemento igual (compareTo == 0) lo reemplaza.
 */
public class ArbolBinarioBusqueda<T extends Comparable<T>> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Nodo expuesto en solo lectura para algoritmos recursivos externos (p. ej. solapamientos). */
    public static final class Nodo<T> {
        private T dato;
        private Nodo<T> izquierdo;
        private Nodo<T> derecho;

        private Nodo(T dato) {
            this.dato = dato;
        }

        public T dato() {
            return dato;
        }

        public Nodo<T> izquierdo() {
            return izquierdo;
        }

        public Nodo<T> derecho() {
            return derecho;
        }
    }

    private transient Nodo<T> raiz;
    private transient int tamanio;

    // ---------------------------------------------------------------- inserción

    /** @return true si se insertó un elemento nuevo; false si reemplazó uno existente. */
    public boolean insertar(T dato) {
        int antes = tamanio;
        raiz = insertar(raiz, dato);
        return tamanio > antes;
    }

    private Nodo<T> insertar(Nodo<T> nodo, T dato) {
        if (nodo == null) {
            tamanio++;
            return new Nodo<>(dato);
        }
        int cmp = dato.compareTo(nodo.dato);
        if (cmp < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, dato);
        } else if (cmp > 0) {
            nodo.derecho = insertar(nodo.derecho, dato);
        } else {
            nodo.dato = dato;
        }
        return nodo;
    }

    // ---------------------------------------------------------------- búsqueda

    public T buscar(T muestra) {
        return buscar(raiz, muestra);
    }

    private T buscar(Nodo<T> nodo, T muestra) {
        if (nodo == null) {
            return null;
        }
        int cmp = muestra.compareTo(nodo.dato);
        if (cmp == 0) {
            return nodo.dato;
        }
        return cmp < 0 ? buscar(nodo.izquierdo, muestra) : buscar(nodo.derecho, muestra);
    }

    /**
     * Búsqueda por clave sin construir un objeto muestra. La función recibe el dato del nodo y
     * devuelve la comparación clave-vs-nodo (negativo: ir a la izquierda; positivo: derecha).
     */
    public T buscarPor(ToIntFunction<T> comparacionConClave) {
        return buscarPor(raiz, comparacionConClave);
    }

    private T buscarPor(Nodo<T> nodo, ToIntFunction<T> cmpFn) {
        if (nodo == null) {
            return null;
        }
        int cmp = cmpFn.applyAsInt(nodo.dato);
        if (cmp == 0) {
            return nodo.dato;
        }
        return cmp < 0 ? buscarPor(nodo.izquierdo, cmpFn) : buscarPor(nodo.derecho, cmpFn);
    }

    public boolean contiene(T muestra) {
        return buscar(muestra) != null;
    }

    // ---------------------------------------------------------------- eliminación

    public boolean eliminar(T muestra) {
        int antes = tamanio;
        raiz = eliminar(raiz, muestra);
        return tamanio < antes;
    }

    private Nodo<T> eliminar(Nodo<T> nodo, T muestra) {
        if (nodo == null) {
            return null;
        }
        int cmp = muestra.compareTo(nodo.dato);
        if (cmp < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, muestra);
        } else if (cmp > 0) {
            nodo.derecho = eliminar(nodo.derecho, muestra);
        } else {
            if (nodo.izquierdo == null) {
                tamanio--;
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                tamanio--;
                return nodo.izquierdo;
            }
            // dos hijos: se reemplaza por el sucesor inorden (mínimo del subárbol derecho)
            T sucesor = minimo(nodo.derecho).dato;
            nodo.dato = sucesor;
            nodo.derecho = eliminar(nodo.derecho, sucesor);
        }
        return nodo;
    }

    // ---------------------------------------------------------------- consultas

    public T minimo() {
        if (raiz == null) {
            throw new EstructuraVaciaException("ArbolBinarioBusqueda");
        }
        return minimo(raiz).dato;
    }

    private Nodo<T> minimo(Nodo<T> nodo) {
        return nodo.izquierdo == null ? nodo : minimo(nodo.izquierdo);
    }

    public T maximo() {
        if (raiz == null) {
            throw new EstructuraVaciaException("ArbolBinarioBusqueda");
        }
        return maximo(raiz).dato;
    }

    private Nodo<T> maximo(Nodo<T> nodo) {
        return nodo.derecho == null ? nodo : maximo(nodo.derecho);
    }

    /** Altura recursiva: árbol vacío = 0, una hoja = 1. */
    public int altura() {
        return altura(raiz);
    }

    private int altura(Nodo<T> nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
    }

    public Nodo<T> raiz() {
        return raiz;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    public void limpiar() {
        raiz = null;
        tamanio = 0;
    }

    // ---------------------------------------------------------------- recorridos recursivos

    public void inorden(Consumer<T> visitante) {
        inorden(raiz, visitante);
    }

    private void inorden(Nodo<T> nodo, Consumer<T> v) {
        if (nodo != null) {
            inorden(nodo.izquierdo, v);
            v.accept(nodo.dato);
            inorden(nodo.derecho, v);
        }
    }

    public void preorden(Consumer<T> visitante) {
        preorden(raiz, visitante);
    }

    private void preorden(Nodo<T> nodo, Consumer<T> v) {
        if (nodo != null) {
            v.accept(nodo.dato);
            preorden(nodo.izquierdo, v);
            preorden(nodo.derecho, v);
        }
    }

    public void postorden(Consumer<T> visitante) {
        postorden(raiz, visitante);
    }

    private void postorden(Nodo<T> nodo, Consumer<T> v) {
        if (nodo != null) {
            postorden(nodo.izquierdo, v);
            postorden(nodo.derecho, v);
            v.accept(nodo.dato);
        }
    }

    public ListaDoble<T> enInorden() {
        ListaDoble<T> lista = new ListaDoble<>();
        inorden(lista::agregar);
        return lista;
    }

    /** Iterador inorden (ascendente) implementado con la Pila propia. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private final Pila<Nodo<T>> pila = new Pila<>();

            {
                apilarIzquierdos(raiz);
            }

            private void apilarIzquierdos(Nodo<T> n) {
                while (n != null) {
                    pila.apilar(n);
                    n = n.izquierdo;
                }
            }

            @Override
            public boolean hasNext() {
                return !pila.estaVacia();
            }

            @Override
            public T next() {
                if (pila.estaVacia()) {
                    throw new NoSuchElementException();
                }
                Nodo<T> n = pila.desapilar();
                apilarIzquierdos(n.derecho);
                return n.dato;
            }
        };
    }

    // Se serializa en preorden para reconstruir exactamente la misma forma del árbol.
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ListaDoble<T> pre = new ListaDoble<>();
        preorden(pre::agregar);
        out.writeInt(pre.tamanio());
        for (T e : pre) {
            out.writeObject(e);
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            insertar((T) in.readObject());
        }
    }
}
