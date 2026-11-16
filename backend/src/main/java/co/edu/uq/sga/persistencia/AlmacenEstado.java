package co.edu.uq.sga.persistencia;

import org.springframework.stereotype.Component;

/** Contenedor del estado vigente; los repositorios siempre acceden a través de él. */
@Component
public class AlmacenEstado {

    private volatile EstadoSistema estado = new EstadoSistema();

    public EstadoSistema actual() {
        return estado;
    }

    public void reemplazar(EstadoSistema nuevo) {
        this.estado = nuevo;
    }
}
