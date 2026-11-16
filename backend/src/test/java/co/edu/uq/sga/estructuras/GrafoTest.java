package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.SgaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Grafo de prueba:
 * <pre>
 *   R --2-- X --2-- Y
 *   |               |
 *   10              1
 *   |               |
 *   Z ------1------ W        Q (aislado)
 * </pre>
 */
class GrafoTest {

    private Grafo<String> g;

    @BeforeEach
    void preparar() {
        g = new Grafo<>();
        for (String v : new String[]{"R", "X", "Y", "Z", "W", "Q"}) {
            g.agregarVertice(v);
        }
        g.conectar("R", "X", 2);
        g.conectar("X", "Y", 2);
        g.conectar("Y", "W", 1);
        g.conectar("R", "Z", 10);
        g.conectar("Z", "W", 1);
    }

    @Test
    void dijkstraEncuentraElCaminoDeMenorPeso() {
        Ruta<String> r = g.dijkstra("R", "Z");
        assertThat(r.camino()).containsExactly("R", "X", "Y", "W", "Z");
        assertThat(r.costo()).isEqualTo(6.0);
    }

    @Test
    void bfsEncuentraElCaminoConMenosTramos() {
        Ruta<String> r = g.rutaBfs("R", "Z");
        assertThat(r.camino()).containsExactly("R", "Z");
        assertThat(r.costo()).isEqualTo(10.0);
    }

    @Test
    void recorridosBfsYDfs() {
        assertThat(g.bfs("R")).containsExactly("R", "X", "Z", "Y", "W");
        assertThat(g.dfs("R")).containsExactly("R", "X", "Y", "W", "Z");
    }

    @Test
    void cerrarPasillosCambiaRutasYAlcanzabilidad() {
        g.cambiarEstadoConexion("Y", "W", false);
        assertThat(g.dijkstra("R", "W").camino()).containsExactly("R", "Z", "W");
        g.cambiarEstadoConexion("R", "Z", false);
        assertThat(g.dijkstra("R", "W").existe()).isFalse();
        assertThat(g.inalcanzablesDesde("R", (a, b) -> true)).containsExactly("Z", "W", "Q");
        g.cambiarEstadoConexion("R", "Z", true);
        assertThat(g.inalcanzablesDesde("R", (a, b) -> true)).containsExactly("Q");
    }

    @Test
    void simularCierreSinModificarElGrafo() {
        ListaDoble<String> sin = g.inalcanzablesDesde("R",
                (a, b) -> !((a.equals("R") && b.equals("X")) || (a.equals("X") && b.equals("R"))));
        assertThat(sin).containsExactly("Q");
        assertThat(g.conexiones().flujo().allMatch(Grafo.Conexion::habilitada)).isTrue();
    }

    @Test
    void componentesConexasConDfs() {
        ListaDoble<ListaDoble<String>> comps = g.componentesConexas((a, b) -> true);
        assertThat(comps.tamanio()).isEqualTo(2);
        assertThat(comps.obtener(1)).containsExactly("Q");
    }

    @Test
    void conexionesSeListanUnaVezYSePuedenEliminar() {
        assertThat(g.cantidadConexiones()).isEqualTo(5);
        assertThat(g.desconectar("R", "Z")).isTrue();
        assertThat(g.cantidadConexiones()).isEqualTo(4);
        assertThat(g.eliminarVertice("W")).isTrue();
        assertThat(g.cantidadConexiones()).isEqualTo(2);
        assertThat(g.tamanio()).isEqualTo(5);
        assertThat(g.dijkstra("R", "Y").costo()).isEqualTo(4.0);
    }

    @Test
    void validaPesosYVertices() {
        assertThatThrownBy(() -> g.conectar("R", "Y", 0)).isInstanceOf(SgaException.class);
        assertThatThrownBy(() -> g.conectar("R", "R", 3)).isInstanceOf(SgaException.class);
        assertThatThrownBy(() -> g.dijkstra("R", "NADA")).isInstanceOf(SgaException.class);
        assertThat(g.agregarVertice("R")).isFalse();
    }
}
