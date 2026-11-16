package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** Periodo [inicio, fin] (ambos inclusive) con un tipo de temporada. */
public record Temporada(String id, String nombre, TipoTemporada tipo, LocalDate inicio, LocalDate fin)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Temporada {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("La temporada debe tener nombre");
        }
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de temporada");
        }
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new FechasInvalidasException("El fin de la temporada no puede ser anterior a su inicio");
        }
    }

    public boolean incluye(LocalDate fecha) {
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }

    public boolean solapaCon(Temporada otra) {
        return !inicio.isAfter(otra.fin) && !otra.inicio.isAfter(fin);
    }
}
