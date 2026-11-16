package co.edu.uq.sga.dominio;

/**
 * Tipos de movimiento del folio. Los reversos son los "movimientos contrarios" que corrigen
 * sin borrar. El signo indica el efecto sobre el saldo (positivo = el huésped debe).
 */
public enum TipoMovimiento {
    CARGO(1, "Cargo"),
    PAGO(-1, "Pago"),
    REVERSO_CARGO(-1, "Reverso de cargo"),
    REVERSO_PAGO(1, "Reverso de pago");

    private final int signo;
    private final String etiqueta;

    TipoMovimiento(int signo, String etiqueta) {
        this.signo = signo;
        this.etiqueta = etiqueta;
    }

    public int signo() {
        return signo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public TipoMovimiento contrario() {
        return switch (this) {
            case CARGO -> REVERSO_CARGO;
            case PAGO -> REVERSO_PAGO;
            case REVERSO_CARGO -> CARGO;
            case REVERSO_PAGO -> PAGO;
        };
    }
}
