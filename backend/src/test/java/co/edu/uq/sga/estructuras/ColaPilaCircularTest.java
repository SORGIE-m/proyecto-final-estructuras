package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColaPilaCircularTest {

    @Test
    void colaRespetaOrdenFifo() {
        Cola<String> c = new Cola<>();
        c.encolar("primera");
        c.encolar("segunda");
        c.encolar("tercera");
        assertThat(c).containsExactly("primera", "segunda", "tercera");
        assertThat(c.frente()).isEqualTo("primera");
        assertThat(c.desencolar()).isEqualTo("primera");
        assertThat(c.desencolar()).isEqualTo("segunda");
        c.encolar("cuarta");
        assertThat(c.desencolar()).isEqualTo("tercera");
        assertThat(c.desencolar()).isEqualTo("cuarta");
        assertThat(c.estaVacia()).isTrue();
    }

    @Test
    void desencolarColaVaciaLanzaEstructuraVacia() {
        assertThatThrownBy(() -> new Cola<Integer>().desencolar())
                .isInstanceOf(EstructuraVaciaException.class)
                .hasMessageContaining("Cola");
    }

    @Test
    void pilaRespetaOrdenLifo() {
        Pila<Integer> p = new Pila<>();
        p.apilar(1);
        p.apilar(2);
        p.apilar(3);
        assertThat(p).containsExactly(3, 2, 1);
        assertThat(p.cima()).isEqualTo(3);
        assertThat(p.desapilar()).isEqualTo(3);
        assertThat(p.tamanio()).isEqualTo(2);
        assertThatThrownBy(() -> {
            p.desapilar();
            p.desapilar();
            p.desapilar();
        }).isInstanceOf(EstructuraVaciaException.class);
    }

    @Test
    void listaCircularRotaYVuelveAlInicio() {
        ListaCircular<String> turnos = new ListaCircular<>();
        turnos.agregar("Pedro");
        turnos.agregar("Paula");
        turnos.agregar("Diego");
        assertThat(turnos.actual()).isEqualTo("Pedro");
        assertThat(turnos.avanzar()).isEqualTo("Paula");
        assertThat(turnos.avanzar()).isEqualTo("Diego");
        assertThat(turnos.avanzar()).isEqualTo("Pedro");
        assertThat(turnos.verSiguiente(4)).isEqualTo("Paula");
        assertThat(turnos).containsExactly("Pedro", "Paula", "Diego");
    }

    @Test
    void listaCircularEliminaAlActualYConservaLaRotacion() {
        ListaCircular<String> turnos = new ListaCircular<>();
        turnos.agregar("A");
        turnos.agregar("B");
        turnos.agregar("C");
        turnos.avanzar(); // B
        assertThat(turnos.eliminar("B")).isTrue();
        assertThat(turnos.actual()).isEqualTo("C");
        assertThat(turnos).containsExactly("C", "A");
        assertThat(turnos.eliminar("C")).isTrue();
        assertThat(turnos.eliminar("A")).isTrue();
        assertThat(turnos.estaVacia()).isTrue();
        assertThatThrownBy(turnos::actual).isInstanceOf(EstructuraVaciaException.class);
    }

    @Test
    void conjuntoNoAdmiteRepetidos() {
        Conjunto<String> favoritos = new Conjunto<>();
        assertThat(favoritos.agregar("A101")).isTrue();
        assertThat(favoritos.agregar("C201")).isTrue();
        assertThat(favoritos.agregar("A101")).isFalse();
        assertThat(favoritos.tamanio()).isEqualTo(2);
        Conjunto<String> otros = new Conjunto<>();
        otros.agregar("C201");
        otros.agregar("B102");
        assertThat(favoritos.union(otros)).containsExactlyInAnyOrder("A101", "C201", "B102");
        assertThat(favoritos.interseccion(otros)).containsExactly("C201");
        assertThat(favoritos.diferencia(otros)).containsExactly("A101");
    }

    @Test
    void diccionarioSobreArbolBuscaPorClave() {
        Diccionario<String, Integer> d = new Diccionario<>();
        d.poner("R-00002", 2);
        d.poner("R-00001", 1);
        d.poner("R-00003", 3);
        d.poner("R-00002", 22);
        assertThat(d.tamanio()).isEqualTo(3);
        assertThat(d.obtener("R-00002")).isEqualTo(22);
        assertThat(d.obtener("R-99999")).isNull();
        assertThat(d).containsExactly(1, 22, 3);
        assertThat(d.eliminar("R-00001")).isTrue();
        assertThat(d.contieneClave("R-00001")).isFalse();
    }
}
