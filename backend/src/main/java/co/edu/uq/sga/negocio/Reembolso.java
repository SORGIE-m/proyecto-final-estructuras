package co.edu.uq.sga.negocio;

/** Vista previa / resultado de una cancelación. */
public record Reembolso(long pagado, long reembolso, long retenido, int porcentaje, String regla) {
}
