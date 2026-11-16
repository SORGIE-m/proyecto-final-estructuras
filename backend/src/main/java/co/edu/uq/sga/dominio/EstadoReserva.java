package co.edu.uq.sga.dominio;

/** Ciclo de vida: Pendiente → Confirmada → En curso → Finalizada (o Cancelada / No-show). */
public enum EstadoReserva {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada"),
    NO_SHOW("No-show");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Una reserva activa bloquea sus noches en el apartamento. */
    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }

    /** Noches que cuentan como ocupadas para estadísticas y calendario. */
    public boolean ocupaNoches() {
        return this == CONFIRMADA || this == EN_CURSO || this == FINALIZADA;
    }

    public boolean puedePasarA(EstadoReserva destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == EN_CURSO || destino == CANCELADA || destino == NO_SHOW;
            case EN_CURSO -> destino == FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;
        };
    }
}
