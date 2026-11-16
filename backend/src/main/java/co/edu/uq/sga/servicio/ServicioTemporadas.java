package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.negocio.ProveedorTemporadas;
import co.edu.uq.sga.servicio.comandos.DatosTemporada;

/** Definición de temporadas; también resuelve la temporada vigente de una fecha. */
public interface ServicioTemporadas extends ProveedorTemporadas {
    ListaDoble<Temporada> listar();

    Temporada crear(DatosTemporada datos);

    Temporada actualizar(String id, DatosTemporada datos);

    void eliminar(String id);
}
