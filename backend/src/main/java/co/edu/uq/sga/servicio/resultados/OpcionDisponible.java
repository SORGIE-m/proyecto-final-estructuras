package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.negocio.Liquidacion;

/** Apartamento libre para el rango pedido con su cotización. */
public record OpcionDisponible(Apartamento apartamento, Liquidacion cotizacion) {
}
