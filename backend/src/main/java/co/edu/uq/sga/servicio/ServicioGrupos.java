package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.comandos.SolicitudGrupo;

/** Reserva de grupos grandes repartidos en varios apartamentos. */
public interface ServicioGrupos {
    ListaDoble<Reserva> reservarGrupo(SolicitudGrupo solicitud);
}
