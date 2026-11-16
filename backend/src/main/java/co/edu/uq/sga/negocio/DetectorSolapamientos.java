package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.estructuras.ListaDoble;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/**
 * Detecta choques recorriendo recursivamente el árbol de reservas de UN apartamento
 * (ordenado por fecha de entrada), sin revisar las reservas del resto del alojamiento.
 * Poda: si una reserva entra en o después de la salida pedida, todo su subárbol derecho
 * también, así que solo se explora el izquierdo.
 */
@Component
public class DetectorSolapamientos {

    /** Primera reserva activa que comparte noche con la estancia, o null si no hay choque. */
    public Reserva primerConflicto(ArbolBinarioBusqueda<Reserva> arbol, Estancia estancia, String codigoExcluido) {
        return buscarConflicto(arbol.raiz(), estancia, codigoExcluido);
    }

    private Reserva buscarConflicto(ArbolBinarioBusqueda.Nodo<Reserva> nodo, Estancia e, String excluido) {
        if (nodo == null) {
            return null;
        }
        Reserva r = nodo.dato();
        if (!r.getEstancia().entrada().isBefore(e.salida())) {
            return buscarConflicto(nodo.izquierdo(), e, excluido);
        }
        if (r.esActiva() && !r.getCodigo().equals(excluido) && r.getEstancia().solapaCon(e)) {
            return r;
        }
        Reserva izquierda = buscarConflicto(nodo.izquierdo(), e, excluido);
        return izquierda != null ? izquierda : buscarConflicto(nodo.derecho(), e, excluido);
    }

    /** Todas las reservas que cumplen el filtro y comparten noches con el rango, en orden de entrada. */
    public ListaDoble<Reserva> reservasEnRango(ArbolBinarioBusqueda<Reserva> arbol, Estancia rango,
                                               Predicate<Reserva> filtro) {
        ListaDoble<Reserva> resultado = new ListaDoble<>();
        recolectar(arbol.raiz(), rango, filtro, resultado);
        return resultado;
    }

    private void recolectar(ArbolBinarioBusqueda.Nodo<Reserva> nodo, Estancia rango, Predicate<Reserva> filtro,
                            ListaDoble<Reserva> salida) {
        if (nodo == null) {
            return;
        }
        Reserva r = nodo.dato();
        recolectar(nodo.izquierdo(), rango, filtro, salida); // inorden: quedan ordenadas por entrada
        if (r.getEstancia().entrada().isBefore(rango.salida())) {
            if (filtro.test(r) && r.getEstancia().solapaCon(rango)) {
                salida.agregar(r);
            }
            recolectar(nodo.derecho(), rango, filtro, salida);
        }
    }
}
