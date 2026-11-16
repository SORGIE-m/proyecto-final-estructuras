package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.negocio.Liquidacion;
import co.edu.uq.sga.servicio.resultados.OpcionDisponible;
import co.edu.uq.sga.servicio.resultados.PropuestaGrupo;

import java.time.LocalDate;

/** Búsqueda de disponibilidad, cotización y asignación de grupos grandes. */
public interface ServicioDisponibilidad {
    /** @param orden precio | capacidad | dormitorios | reservas | codigo */
    ListaDoble<OpcionDisponible> buscar(LocalDate entrada, LocalDate salida, int personas, String orden);

    Liquidacion cotizar(String codigoApartamento, LocalDate entrada, LocalDate salida, ListaDoble<Ocupante> ocupantes);

    Liquidacion cotizar(String codigoApartamento, LocalDate entrada, LocalDate salida, int pagan, int exentos);

    PropuestaGrupo proponerGrupo(LocalDate entrada, LocalDate salida, int personas);

    /** Apartamentos disponibles (sin filtrar por capacidad) para el rango. */
    ListaDoble<co.edu.uq.sga.dominio.Apartamento> libres(LocalDate entrada, LocalDate salida);
}
