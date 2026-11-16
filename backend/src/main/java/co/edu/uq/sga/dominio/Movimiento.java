package co.edu.uq.sga.dominio;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Movimiento inalterable del folio. Un reverso referencia el movimiento que corrige. */
public record Movimiento(int numero, TipoMovimiento tipo, String concepto, long monto,
                         LocalDateTime fecha, Integer numeroReferencia, String usuario) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public long efectoEnSaldo() {
        return tipo.signo() * monto;
    }
}
