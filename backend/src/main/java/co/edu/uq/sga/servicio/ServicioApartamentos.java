package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.comandos.DatosApartamento;

/** Gestión (CRUD) del catálogo de apartamentos. */
public interface ServicioApartamentos {
    /** @param orden codigo | precio | capacidad | dormitorios | reservas */
    ListaDoble<Apartamento> listar(String orden);

    Apartamento obtener(String codigo);

    Apartamento crear(DatosApartamento datos);

    Apartamento actualizar(String codigo, DatosApartamento datos);

    void eliminar(String codigo);

    /** Fuera de servicio (con motivo) o regreso al servicio (queda pendiente de preparación). */
    Apartamento cambiarEstado(String codigo, EstadoApartamento destino, String motivo);
}
