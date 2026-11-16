package co.edu.uq.sga.dominio;

/** Temporadas tarifarias. BAJA es la temporada base (aplica cuando ninguna otra cubre la fecha). */
public enum TipoTemporada {
    BAJA("Baja"), MEDIA("Media"), ALTA("Alta");

    private final String etiqueta;

    TipoTemporada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
