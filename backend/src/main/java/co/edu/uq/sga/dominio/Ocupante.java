package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

/** Persona que se aloja en una reserva. Los menores de 6 años ocupan cupo pero no pagan. */
public record Ocupante(String nombre, String documento, LocalDate fechaNacimiento) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int EDAD_MINIMA_PAGO = 6;

    public Ocupante {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("Cada ocupante debe tener nombre");
        }
        if (fechaNacimiento == null) {
            throw new FechasInvalidasException("Cada ocupante debe tener fecha de nacimiento");
        }
        nombre = nombre.trim();
        documento = documento == null ? "" : documento.trim();
    }

    public int edadEn(LocalDate fecha) {
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /** La edad se evalúa en la fecha de entrada de la estancia. */
    public boolean pagaTarifa(LocalDate fechaEntrada) {
        return edadEn(fechaEntrada) >= EDAD_MINIMA_PAGO;
    }
}
