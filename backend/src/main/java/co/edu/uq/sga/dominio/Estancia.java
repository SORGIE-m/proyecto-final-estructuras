package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.FechasInvalidasException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Estancia [entrada, salida): la noche de salida no se cobra ni se ocupa.
 * Es iterable: {@code for (LocalDate noche : estancia)} recorre las noches a cobrar.
 */
public record Estancia(LocalDate entrada, LocalDate salida) implements Iterable<LocalDate>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int MAXIMO_NOCHES = 60;

    public Estancia {
        if (entrada == null || salida == null) {
            throw new FechasInvalidasException("Debe indicar la fecha de entrada y la de salida");
        }
        if (!salida.isAfter(entrada)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la de entrada");
        }
        if (ChronoUnit.DAYS.between(entrada, salida) > MAXIMO_NOCHES) {
            throw new FechasInvalidasException("Una estancia no puede superar " + MAXIMO_NOCHES + " noches");
        }
    }

    public int noches() {
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }

    /** Dos estancias se solapan si comparten al menos una noche. */
    public boolean solapaCon(Estancia otra) {
        return entrada.isBefore(otra.salida) && otra.entrada.isBefore(salida);
    }

    /** ¿La noche indicada pertenece a la estancia? */
    public boolean incluyeNoche(LocalDate noche) {
        return !noche.isBefore(entrada) && noche.isBefore(salida);
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return new Iterator<>() {
            private LocalDate actual = entrada;

            @Override
            public boolean hasNext() {
                return actual.isBefore(salida);
            }

            @Override
            public LocalDate next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                LocalDate noche = actual;
                actual = actual.plusDays(1);
                return noche;
            }
        };
    }
}
