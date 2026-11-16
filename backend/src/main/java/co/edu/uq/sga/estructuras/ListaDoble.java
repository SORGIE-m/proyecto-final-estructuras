package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Lista doblemente enlazada genérica. Se usa para el historial de estancias, los ocupantes
 * de una reserva, el desglose de noches liquidadas y como base de otras estructuras.
 */
public class ListaDoble<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final class Nodo<T> {
        T dato;
        Nodo<T> anterior;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private transient Nodo<T> cabeza;
    private transient Nodo<T> cola;
    private transient int tamanio;

    public ListaDoble() {
        // lista vacía
    }

    @SafeVarargs
    public static <T> ListaDoble<T> de(T... elementos) {
        ListaDoble<T> lista = new ListaDoble<>();
        for (T e : elementos) {
            lista.agregar(e);
        }
        return lista;
    }

    public static <T> ListaDoble<T> desde(Iterable<T> origen) {
        ListaDoble<T> lista = new ListaDoble<>();
        for (T e : origen) {
            lista.agregar(e);
        }
        return lista;
    }

    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cola == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.anterior = cola;
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamanio++;
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.siguiente = cabeza;
            cabeza.anterior = nuevo;
            cabeza = nuevo;
        }
        tamanio++;
    }

    public void agregarTodos(Iterable<T> otros) {
        for (T e : otros) {
            agregar(e);
        }
    }

    public T obtener(int indice) {
        return nodoEn(indice).dato;
    }

    public void reemplazar(int indice, T dato) {
        nodoEn(indice).dato = dato;
    }

    public T primero() {
        if (cabeza == null) {
            throw new EstructuraVaciaException("ListaDoble");
        }
        return cabeza.dato;
    }

    public T ultimo() {
        if (cola == null) {
            throw new EstructuraVaciaException("ListaDoble");
        }
        return cola.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) {
            throw new EstructuraVaciaException("ListaDoble");
        }
        T dato = cabeza.dato;
        desenlazar(cabeza);
        return dato;
    }

    public T eliminarUltimo() {
        if (cola == null) {
            throw new EstructuraVaciaException("ListaDoble");
        }
        T dato = cola.dato;
        desenlazar(cola);
        return dato;
    }

    public T eliminarEn(int indice) {
        Nodo<T> nodo = nodoEn(indice);
        desenlazar(nodo);
        return nodo.dato;
    }

    /** Elimina la primera aparición (según equals). */
    public boolean eliminar(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (Objects.equals(n.dato, dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    /** Elimina todos los elementos que cumplen la condición; devuelve cuántos eliminó. */
    public int eliminarSi(Predicate<T> condicion) {
        int eliminados = 0;
        Nodo<T> n = cabeza;
        while (n != null) {
            Nodo<T> sig = n.siguiente;
            if (condicion.test(n.dato)) {
                desenlazar(n);
                eliminados++;
            }
            n = sig;
        }
        return eliminados;
    }

    public boolean contiene(T dato) {
        return indiceDe(dato) >= 0;
    }

    public int indiceDe(T dato) {
        int i = 0;
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente, i++) {
            if (Objects.equals(n.dato, dato)) {
                return i;
            }
        }
        return -1;
    }

    public T buscar(Predicate<T> condicion) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (condicion.test(n.dato)) {
                return n.dato;
            }
        }
        return null;
    }

    public ListaDoble<T> filtrar(Predicate<T> condicion) {
        ListaDoble<T> resultado = new ListaDoble<>();
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (condicion.test(n.dato)) {
                resultado.agregar(n.dato);
            }
        }
        return resultado;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamanio = 0;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    /** Ordena la lista en el lugar con Merge Sort recursivo (estable, O(n log n)). */
    public void ordenar(Comparator<? super T> comparador) {
        cabeza = mergeSort(cabeza, comparador);
        // recomponer enlaces "anterior" y la cola
        Nodo<T> previo = null;
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            n.anterior = previo;
            previo = n;
        }
        cola = previo;
    }

    private Nodo<T> mergeSort(Nodo<T> inicio, Comparator<? super T> c) {
        if (inicio == null || inicio.siguiente == null) {
            return inicio;
        }
        Nodo<T> medio = mitad(inicio);
        Nodo<T> segunda = medio.siguiente;
        medio.siguiente = null;
        return mezclar(mergeSort(inicio, c), mergeSort(segunda, c), c);
    }

    private Nodo<T> mitad(Nodo<T> inicio) {
        Nodo<T> lento = inicio;
        Nodo<T> rapido = inicio.siguiente;
        while (rapido != null && rapido.siguiente != null) {
            lento = lento.siguiente;
            rapido = rapido.siguiente.siguiente;
        }
        return lento;
    }

    private Nodo<T> mezclar(Nodo<T> a, Nodo<T> b, Comparator<? super T> c) {
        Nodo<T> centinela = new Nodo<>(null);
        Nodo<T> actual = centinela;
        while (a != null && b != null) {
            if (c.compare(a.dato, b.dato) <= 0) {
                actual.siguiente = a;
                a = a.siguiente;
            } else {
                actual.siguiente = b;
                b = b.siguiente;
            }
            actual = actual.siguiente;
        }
        actual.siguiente = (a != null) ? a : b;
        return centinela.siguiente;
    }

    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0," + tamanio + ")");
        }
        // recorre desde el extremo más cercano gracias al doble enlace
        if (indice < tamanio / 2) {
            Nodo<T> n = cabeza;
            for (int i = 0; i < indice; i++) {
                n = n.siguiente;
            }
            return n;
        }
        Nodo<T> n = cola;
        for (int i = tamanio - 1; i > indice; i--) {
            n = n.anterior;
        }
        return n;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior == null) {
            cabeza = n.siguiente;
        } else {
            n.anterior.siguiente = n.siguiente;
        }
        if (n.siguiente == null) {
            cola = n.anterior;
        } else {
            n.siguiente.anterior = n.anterior;
        }
        n.anterior = n.siguiente = null;
        tamanio--;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    /** Recorrido inverso aprovechando el enlace "anterior". */
    public Iterable<T> enReversa() {
        return () -> new Iterator<>() {
            private Nodo<T> actual = cola;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.anterior;
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }

    // Serialización iterativa: evita desbordar la pila con listas largas.
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(tamanio);
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            out.writeObject(n.dato);
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            agregar((T) in.readObject());
        }
    }
}
