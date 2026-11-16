package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;

/** Tarifa por persona por noche en cada temporada (pesos colombianos). */
public record Tarifas(long baja, long media, long alta) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Tarifas {
        if (baja <= 0 || media <= 0 || alta <= 0) {
            throw new ReglaNegocioException("Las tarifas deben ser mayores que cero");
        }
    }

    public long para(TipoTemporada temporada) {
        return switch (temporada) {
            case BAJA -> baja;
            case MEDIA -> media;
            case ALTA -> alta;
        };
    }
}
