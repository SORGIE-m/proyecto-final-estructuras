package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Cola de prioridad implementada como montículo binario (min-heap) sobre un arreglo.
 * El "menor" según el comparador sale primero. Usos: cola de preparación y vencimiento de
 * reservas pendientes. Insertar y extraer cuestan O(log n).
 */
public class ColaPrioridad<T> implements Coleccion<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private final Comparator<? super T> comparador;
    private Object[] monticulo;
    private int tamanio;

    public ColaPrioridad(Comparator<? super T> comparador) {
        this.comparador = comparador;
        this.monticulo = new Object[CAPACIDAD_INICIAL];
    }

    public void insertar(T dato) {
        if (tamanio == monticulo.length) {
            Object[] mayor = new Object[monticulo.length * 2];
            System.arraycopy(monticulo, 0, mayor, 0, tamanio);
            monticulo = mayor;
        }
        monticulo[tamanio] = dato;
        flotar(tamanio);
        tamanio++;
    }

    public T extraer() {
        if (tamanio == 0) {
            throw new EstructuraVaciaException("ColaPrioridad");
        }
        T raiz = elemento(0);
        tamanio--;
        monticulo[0] = monticulo[tamanio];
        monticulo[tamanio] = null;
        if (tamanio > 0) {
            hundir(0);
        }
        return raiz;
    }

    public T verPrimero() {
        if (tamanio == 0) {
            throw new EstructuraVaciaException("ColaPrioridad");
        }
        return elemento(0);
    }

    public boolean contiene(Predicate<T> condicion) {
        for (int i = 0; i < tamanio; i++) {
            if (condicion.test(elemento(i))) {
                return true;
            }
        }
        return false;
    }

    /** Elimina los elementos que cumplen la condición y restaura la propiedad de montículo. */
    public int eliminarSi(Predicate<T> condicion) {
        int escritura = 0;
        for (int i = 0; i < tamanio; i++) {
            T e = elemento(i);
            if (!condicion.test(e)) {
                monticulo[escritura++] = e;
            }
        }
        int eliminados = tamanio - escritura;
        for (int i = escritura; i < tamanio; i++) {
            monticulo[i] = null;
        }
        tamanio = escritura;
        reordenar();
        return eliminados;
    }

    /** Heapify ascendente O(n): útil cuando cambian las prioridades (p. ej. al cambiar el día). */
    public void reordenar() {
        for (int i = tamanio / 2 - 1; i >= 0; i--) {
            hundir(i);
        }
    }

    public void limpiar() {
        for (int i = 0; i < tamanio; i++) {
            monticulo[i] = null;
        }
        tamanio = 0;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    private void flotar(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (comparador.compare(elemento(i), elemento(padre)) >= 0) {
                return;
            }
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void hundir(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menor = i;
            if (izq < tamanio && comparador.compare(elemento(izq), elemento(menor)) < 0) {
                menor = izq;
            }
            if (der < tamanio && comparador.compare(elemento(der), elemento(menor)) < 0) {
                menor = der;
            }
            if (menor == i) {
                return;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        Object t = monticulo[a];
        monticulo[a] = monticulo[b];
        monticulo[b] = t;
    }

    @SuppressWarnings("unchecked")
    private T elemento(int i) {
        return (T) monticulo[i];
    }

    /** Itera en orden de prioridad sin modificar la cola (trabaja sobre una copia). */
    @Override
    public Iterator<T> iterator() {
        ColaPrioridad<T> copia = new ColaPrioridad<>(comparador);
        for (int i = 0; i < tamanio; i++) {
            copia.insertar(elemento(i));
        }
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !copia.estaVacia();
            }

            @Override
            public T next() {
                if (copia.estaVacia()) {
                    throw new NoSuchElementException();
                }
                return copia.extraer();
            }
        };
    }
}
