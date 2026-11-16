package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArbolBinarioBusquedaTest {

    private ArbolBinarioBusqueda<Integer> arbolDeEjemplo() {
        ArbolBinarioBusqueda<Integer> a = new ArbolBinarioBusqueda<>();
        for (int v : new int[]{50, 30, 70, 20, 40, 60, 80, 35}) {
            a.insertar(v);
        }
        return a;
    }

    @Test
    void insertaYRecorreEnLosTresOrdenes() {
        ArbolBinarioBusqueda<Integer> a = arbolDeEjemplo();
        ListaDoble<Integer> pre = new ListaDoble<>();
        ListaDoble<Integer> post = new ListaDoble<>();
        a.preorden(pre::agregar);
        a.postorden(post::agregar);
        assertThat(a.enInorden()).containsExactly(20, 30, 35, 40, 50, 60, 70, 80);
        assertThat(pre).containsExactly(50, 30, 20, 40, 35, 70, 60, 80);
        assertThat(post).containsExactly(20, 35, 40, 30, 60, 80, 70, 50);
        assertThat(a).containsExactly(20, 30, 35, 40, 50, 60, 70, 80);
        assertThat(a.altura()).isEqualTo(4);
        assertThat(a.minimo()).isEqualTo(20);
        assertThat(a.maximo()).isEqualTo(80);
    }

    @Test
    void noDuplicaElementosIguales() {
        ArbolBinarioBusqueda<Integer> a = arbolDeEjemplo();
        assertThat(a.insertar(40)).isFalse();
        assertThat(a.tamanio()).isEqualTo(8);
    }

    @Test
    void buscaPorMuestraYPorClave() {
        ArbolBinarioBusqueda<Integer> a = arbolDeEjemplo();
        assertThat(a.buscar(35)).isEqualTo(35);
        assertThat(a.buscar(36)).isNull();
        assertThat(a.buscarPor(n -> Integer.compare(60, n))).isEqualTo(60);
        assertThat(a.contiene(99)).isFalse();
    }

    @Test
    void eliminaHojaNodoConUnHijoYNodoConDosHijos() {
        ArbolBinarioBusqueda<Integer> a = arbolDeEjemplo();
        assertThat(a.eliminar(20)).isTrue();   // hoja
        assertThat(a.eliminar(40)).isTrue();   // un hijo (35)
        assertThat(a.eliminar(50)).isTrue();   // raíz con dos hijos → sucesor 60
        assertThat(a.eliminar(999)).isFalse();
        assertThat(a.enInorden()).containsExactly(30, 35, 60, 70, 80);
        assertThat(a.raiz().dato()).isEqualTo(60);
        assertThat(a.tamanio()).isEqualTo(5);
    }

    @Test
    void arbolVacio() {
        ArbolBinarioBusqueda<String> a = new ArbolBinarioBusqueda<>();
        assertThat(a.altura()).isZero();
        assertThat(a.estaVacia()).isTrue();
        assertThat(a.iterator().hasNext()).isFalse();
        assertThatThrownBy(a::minimo).isInstanceOf(EstructuraVaciaException.class);
    }
}
