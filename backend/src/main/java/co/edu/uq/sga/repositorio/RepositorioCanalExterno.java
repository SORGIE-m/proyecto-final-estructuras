package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.estructuras.Cola;
import co.edu.uq.sga.estructuras.ListaDoble;

public interface RepositorioCanalExterno {
    Cola<SolicitudExterna> cola();

    ListaDoble<ProcesamientoExterno> procesamientos();

    String siguienteId();
}
