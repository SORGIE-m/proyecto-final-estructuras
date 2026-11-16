package co.edu.uq.sga.estructuras;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Las estructuras se persisten con serialización propia (iterativa) y conservan su forma. */
class SerializacionEstructurasTest {

    @SuppressWarnings("unchecked")
    private static <T> T copiar(T objeto) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(objeto);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    void listaLargaSinDesbordarLaPila() throws Exception {
        ListaDoble<Integer> l = new ListaDoble<>();
        for (int i = 0; i < 100_000; i++) {
            l.agregar(i);
        }
        ListaDoble<Integer> copia = copiar(l);
        assertThat(copia.tamanio()).isEqualTo(100_000);
        assertThat(copia.ultimo()).isEqualTo(99_999);
    }

    @Test
    void arbolConservaSuForma() throws Exception {
        ArbolBinarioBusqueda<Integer> a = new ArbolBinarioBusqueda<>();
        for (int v : new int[]{50, 30, 70, 20, 40, 60, 80}) {
            a.insertar(v);
        }
        ArbolBinarioBusqueda<Integer> copia = copiar(a);
        assertThat(copia.raiz().dato()).isEqualTo(50);
        assertThat(copia.altura()).isEqualTo(3);
        assertThat(copia).containsExactly(20, 30, 40, 50, 60, 70, 80);
    }

    @Test
    void pilaColaYCircularConservanElOrden() throws Exception {
        Pila<String> p = new Pila<>();
        p.apilar("a");
        p.apilar("b");
        Cola<String> c = new Cola<>();
        c.encolar("x");
        c.encolar("y");
        ListaCircular<String> lc = new ListaCircular<>();
        lc.agregar("1");
        lc.agregar("2");
        lc.agregar("3");
        lc.avanzar();
        assertThat(copiar(p)).containsExactly("b", "a");
        assertThat(copiar(c)).containsExactly("x", "y");
        ListaCircular<String> lc2 = copiar(lc);
        assertThat(lc2.actual()).isEqualTo("2");
        assertThat(lc2).containsExactly("2", "3", "1");
    }

    @Test
    void grafoConservaPasillosYEstados() throws Exception {
        Grafo<String> g = new Grafo<>();
        g.agregarVertice("A");
        g.agregarVertice("B");
        g.agregarVertice("C");
        g.conectar("A", "B", 4);
        g.conectar("B", "C", 5);
        g.cambiarEstadoConexion("B", "C", false);
        Grafo<String> copia = copiar(g);
        assertThat(copia.cantidadConexiones()).isEqualTo(2);
        assertThat(copia.dijkstra("A", "C").existe()).isFalse();
        assertThat(copia.dijkstra("A", "B").costo()).isEqualTo(4.0);
    }
}
