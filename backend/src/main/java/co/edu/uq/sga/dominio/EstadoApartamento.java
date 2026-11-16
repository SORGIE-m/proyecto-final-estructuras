package co.edu.uq.sga.dominio;

/** Estado operativo de un apartamento y sus transiciones permitidas. */
public enum EstadoApartamento {
    PREPARADO("Preparado"),
    OCUPADO("Ocupado"),
    PENDIENTE_PREPARACION("Pendiente de preparación"),
    EN_PREPARACION("En preparación"),
    FUERA_SERVICIO("Fuera de servicio");

    private final String etiqueta;

    EstadoApartamento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean puedePasarA(EstadoApartamento destino) {
        return switch (this) {
            case PREPARADO -> destino == OCUPADO || destino == FUERA_SERVICIO || destino == PENDIENTE_PREPARACION;
            case OCUPADO -> destino == PENDIENTE_PREPARACION;
            case PENDIENTE_PREPARACION -> destino == EN_PREPARACION || destino == FUERA_SERVICIO;
            case EN_PREPARACION -> destino == PREPARADO || destino == PENDIENTE_PREPARACION || destino == FUERA_SERVICIO;
            case FUERA_SERVICIO -> destino == PENDIENTE_PREPARACION;
        };
    }
}
