package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.comandos.DatosSolicitudExterna;

/** Cola FIFO de solicitudes de la plataforma de terceros. */
public interface ServicioCanalExterno {
    SolicitudExterna recibir(DatosSolicitudExterna datos);

    /** Genera solicitudes aleatorias (algunas pueden chocar) para simular la plataforma. */
    ListaDoble<SolicitudExterna> simular(int cantidad);

    ListaDoble<SolicitudExterna> pendientes();

    ProcesamientoExterno procesarSiguiente();

    ListaDoble<ProcesamientoExterno> procesarTodas();

    ListaDoble<ProcesamientoExterno> historial();
}
