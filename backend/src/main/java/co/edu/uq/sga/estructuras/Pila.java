package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Pila LIFO enlazada. Deshacer operaciones del folio y recorrido iterativo del árbol. */
public class Pila<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final class Nodo<T> {
        final T dato;
        final Nodo<T> debajo;

        Nodo(T dato, Nodo<T> debajo) {
            this.dato = dato;
            this.debajo = debajo;
        }
    }

    private transient Nodo<T> cima;
    private transient int tamanio;

    public void apilar(T dato) {
        cima = new Nodo<>(dato, cima);
        tamanio++;
    }

    public T desapilar() {
        if (cima == null) {
            throw new EstructuraVaciaException("Pila");
        }
        T dato = cima.dato;
        cima = cima.debajo;
        tamanio--;
        return dato;
    }

    public T cima() {
        if (cima == null) {
            throw new EstructuraVaciaException("Pila");
        }
        return cima.dato;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    /** Itera desde la cima hacia el fondo. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cima;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T d = actual.dato;
                actual = actual.debajo;
                return d;
            }
        };
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // se escribe del fondo a la cima para reconstruir el mismo orden
        ListaDoble<T> orden = new ListaDoble<>();
        for (T e : this) {
            orden.agregarAlInicio(e);
        }
        out.writeInt(tamanio);
        for (T e : orden) {
            out.writeObject(e);
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            apilar((T) in.readObject());
        }
    }
}
