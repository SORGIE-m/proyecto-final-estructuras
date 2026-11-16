package co.edu.uq.sga.persistencia;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Unidad de trabajo con bloqueo lectores/escritor. Las escrituras pueden anidarse: solo la más
 * externa persiste al terminar bien, o revierte el estado a la última versión guardada si falla.
 */
@Component
public class UnidadDeTrabajoArchivo implements UnidadDeTrabajo {

    private final AlmacenEstado almacen;
    private final PersistenciaEstado persistencia;
    private final ReentrantReadWriteLock candado = new ReentrantReadWriteLock(true);
    /** Profundidad de anidamiento; solo la modifica el hilo que posee el candado de escritura. */
    private int profundidad;

    public UnidadDeTrabajoArchivo(AlmacenEstado almacen, PersistenciaEstado persistencia) {
        this.almacen = almacen;
        this.persistencia = persistencia;
    }

    @Override
    public <R> R leer(Supplier<R> operacion) {
        candado.readLock().lock();
        try {
            return operacion.get();
        } finally {
            candado.readLock().unlock();
        }
    }

    @Override
    public <R> R escribir(Supplier<R> operacion) {
        candado.writeLock().lock();
        profundidad++;
        try {
            R resultado;
            try {
                resultado = operacion.get();
            } catch (RuntimeException | Error e) {
                if (profundidad == 1) {
                    revertir();
                }
                throw e;
            }
            if (profundidad == 1) {
                persistencia.guardar(almacen.actual());
            }
            return resultado;
        } finally {
            profundidad--;
            candado.writeLock().unlock();
        }
    }

    @Override
    public <R> R exclusivo(Supplier<R> operacion) {
        candado.writeLock().lock();
        try {
            return operacion.get();
        } finally {
            candado.writeLock().unlock();
        }
    }

    @Override
    public void reemplazarEstado(EstadoSistema nuevo) {
        candado.writeLock().lock();
        try {
            almacen.reemplazar(nuevo);
            persistencia.guardar(nuevo);
        } finally {
            candado.writeLock().unlock();
        }
    }

    private void revertir() {
        EstadoSistema guardado = persistencia.cargar();
        almacen.reemplazar(guardado != null ? guardado : new EstadoSistema());
    }
}
