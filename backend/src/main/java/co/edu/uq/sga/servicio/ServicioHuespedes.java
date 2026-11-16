package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.comandos.DatosHuesped;
import co.edu.uq.sga.servicio.comandos.DatosTitular;

/** Perfiles de huésped, favoritos e historial. */
public interface ServicioHuespedes {
    Huesped registrar(DatosHuesped datos);

    Huesped actualizar(String documento, DatosHuesped datos);

    void eliminar(String documento);

    Huesped obtener(String documento);

    /** Búsqueda por texto (nombre, documento o correo); vacío = todos, ordenados por documento. */
    ListaDoble<Huesped> listar(String texto);

    /** Devuelve el huésped o lo crea (sin contraseña) con los datos del titular. */
    Huesped obtenerOCrear(DatosTitular titular);

    boolean agregarFavorito(String documento, String codigoApartamento);

    boolean quitarFavorito(String documento, String codigoApartamento);

    ListaDoble<Apartamento> favoritos(String documento);

    /** Estancias finalizadas, de la más reciente a la más antigua. */
    ListaDoble<Reserva> historial(String documento);

    /** Todas las reservas del huésped (más recientes primero). */
    ListaDoble<Reserva> reservas(String documento);
}
