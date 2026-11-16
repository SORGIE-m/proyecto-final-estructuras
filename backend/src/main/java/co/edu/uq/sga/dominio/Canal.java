package co.edu.uq.sga.dominio;

/** Canal por el que ingresa una reserva. */
public enum Canal {
    PORTAL("Portal"), DIRECTA("Directa"), EXTERNA("Externa");

    private final String etiqueta;

    Canal(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
