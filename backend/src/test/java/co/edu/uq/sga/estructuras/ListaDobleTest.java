package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListaDobleTest {

    @Test
    void agregaEnAmbosExtremosYRecorreEnAmbosSentidos() {
        ListaDoble<Integer> l = new ListaDoble<>();
        l.agregar(2);
        l.agregar(3);
        l.agregarAlInicio(1);
        assertThat(l.tamanio()).isEqualTo(3);
        assertThat(l).containsExactly(1, 2, 3);
        assertThat(l.enReversa()).containsExactly(3, 2, 1);
        assertThat(l.primero()).isEqualTo(1);
        assertThat(l.ultimo()).isEqualTo(3);
    }

    @Test
    void obtieneEliminaYBusca() {
        ListaDoble<String> l = ListaDoble.de("a", "b", "c", "d");
        assertThat(l.obtener(2)).isEqualTo("c");
        assertThat(l.eliminarEn(1)).isEqualTo("b");
        assertThat(l.eliminar("z")).isFalse();
        assertThat(l.eliminar("d")).isTrue();
        assertThat(l).containsExactly("a", "c");
        assertThat(l.indiceDe("c")).isEqualTo(1);
        assertThat(l.buscar(s -> s.startsWith("c"))).isEqualTo("c");
        assertThat(l.eliminarSi(s -> true)).isEqualTo(2);
        assertThat(l.estaVacia()).isTrue();
    }

    @Test
    void lanzaExcepcionesPropiasEnListaVaciaYFueraDeRango() {
        ListaDoble<Integer> l = new ListaDoble<>();
        assertThatThrownBy(l::primero).isInstanceOf(EstructuraVaciaException.class);
        assertThatThrownBy(l::eliminarUltimo).isInstanceOf(EstructuraVaciaException.class);
        assertThatThrownBy(() -> l.obtener(0)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void ordenaConMergeSortDeFormaEstable() {
        ListaDoble<String> l = ListaDoble.de("pera", "uva", "kiwi", "fresa", "mango", "lima");
        l.ordenar(Comparator.comparingInt(String::length));
        assertThat(l).containsExactly("uva", "pera", "kiwi", "lima", "fresa", "mango");
        assertThat(l.enReversa()).first().isEqualTo("mango");
        assertThat(l.ultimo()).isEqualTo("mango");
    }

    @Test
    void ordenaListasGrandes() {
        ListaDoble<Integer> l = new ListaDoble<>();
        for (int i = 5000; i > 0; i--) {
            l.agregar(i);
        }
        l.ordenar(Comparator.naturalOrder());
        assertThat(l.primero()).isEqualTo(1);
        assertThat(l.ultimo()).isEqualTo(5000);
        assertThat(l.obtener(2499)).isEqualTo(2500);
    }

    @Test
    void filtraYOfreceFlujo() {
        ListaDoble<Integer> l = ListaDoble.de(1, 2, 3, 4, 5, 6);
        assertThat(l.filtrar(n -> n % 2 == 0)).containsExactly(2, 4, 6);
        assertThat(l.flujo().mapToInt(Integer::intValue).sum()).isEqualTo(21);
    }
}
