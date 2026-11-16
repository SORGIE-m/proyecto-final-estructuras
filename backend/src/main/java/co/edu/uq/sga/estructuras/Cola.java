package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Cola FIFO enlazada. Solicitudes del canal externo, procesadas en orden de llegada. */
public class Cola<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final class Nodo<T> {
        final T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private transient Nodo<T> frente;
    private transient Nodo<T> fin;
    private transient int tamanio;

    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = fin = nuevo;
        } else {
            fin.siguiente = nuevo;
            fin = nuevo;
        }
        tamanio++;
    }

    public T desencolar() {
        if (frente == null) {
            throw new EstructuraVaciaException("Cola");
        }
        T dato = frente.dato;
        frente = frente.siguiente;
        if (frente == null) {
            fin = null;
        }
        tamanio--;
        return dato;
    }

    public T frente() {
        if (frente == null) {
            throw new EstructuraVaciaException("Cola");
        }
        return frente.dato;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = frente;

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
                actual = actual.siguiente;
                return d;
            }
        };
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(tamanio);
        for (T e : this) {
            out.writeObject(e);
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            encolar((T) in.readObject());
        }
    }
}
