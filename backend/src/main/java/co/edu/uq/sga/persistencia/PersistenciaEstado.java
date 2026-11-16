package co.edu.uq.sga.persistencia;

/** Puerto de persistencia (DIP): los servicios no conocen el formato ni el medio. */
public interface PersistenciaEstado {

    /** @return el estado guardado o null si aún no existe. */
    EstadoSistema cargar();

    void guardar(EstadoSistema estado);
}
