package co.edu.uq.sga.estructuras;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Contrato común de todas las estructuras propias (principio de segregación de interfaces:
 * solo lo que comparten todas). Toda colección es iterable con un for-each.
 */
public interface Coleccion<T> extends Iterable<T> {

    int tamanio();

    default boolean estaVacia() {
        return tamanio() == 0;
    }

    /** Permite usar la API de streams sobre una estructura propia (sin copiar a java.util). */
    default Stream<T> flujo() {
        return StreamSupport.stream(
                Spliterators.spliterator(iterator(), tamanio(), Spliterator.ORDERED), false);
    }
}
