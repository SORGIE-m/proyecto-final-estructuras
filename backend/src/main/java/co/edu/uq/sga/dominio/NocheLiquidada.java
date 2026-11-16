package co.edu.uq.sga.dominio;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** Una línea del desglose noche por noche de una estancia. */
public record NocheLiquidada(LocalDate fecha, TipoTemporada temporada, long tarifaPorPersona,
                             int personasQuePagan, int personasExentas, long subtotal) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
