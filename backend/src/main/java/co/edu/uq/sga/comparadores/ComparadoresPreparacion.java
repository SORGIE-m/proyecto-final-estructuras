package co.edu.uq.sga.comparadores;

import co.edu.uq.sga.dominio.ItemPreparacion;

import java.util.Comparator;

/** Prioridad de la cola de preparación combinando comparadores con thenComparing. */
public final class ComparadoresPreparacion {

    private ComparadoresPreparacion() {
    }

    /**
     * 1) Entrada hoy primero (prioridad 1 antes que 2).
     * 2) Hora estimada de llegada del siguiente grupo (la más temprana primero).
     * 3) Antigüedad en la cola (quien lleva más tiempo pendiente).
     * 4) Código, para un orden total y determinista.
     */
    public static Comparator<ItemPreparacion> prioridad() {
        return Comparator.comparingInt(ItemPreparacion::prioridad)
                .thenComparing(ItemPreparacion::horaLlegada, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ItemPreparacion::pendienteDesde, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ItemPreparacion::apartamentoCodigo);
    }
}
