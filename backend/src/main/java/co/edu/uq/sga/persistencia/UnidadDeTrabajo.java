package co.edu.uq.sga.persistencia;

import java.util.function.Supplier;

/**
 * Delimita operaciones de lectura y escritura. Una escritura es atómica: si falla, el estado
 * en memoria vuelve a la última versión guardada; si termina bien, se persiste.
 */
public interface UnidadDeTrabajo {

    <R> R leer(Supplier<R> operacion);

    <R> R escribir(Supplier<R> operacion);

    /**
     * Acceso exclusivo que solo toca índices derivados en memoria (no persistidos), como
     * refrescar las prioridades de una cola. No escribe en disco.
     */
    <R> R exclusivo(Supplier<R> operacion);

    default void ejecutar(Runnable operacion) {
        escribir(() -> {
            operacion.run();
            return null;
        });
    }

    /** Reemplaza todo el estado (carga de escenario) y lo persiste. */
    void reemplazarEstado(EstadoSistema nuevo);
}
