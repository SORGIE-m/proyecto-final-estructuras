package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.estructuras.Pila;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cuenta de una reserva. Los movimientos nunca se borran: deshacer registra el movimiento
 * contrario de la última operación apilada.
 */
public class Folio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ListaDoble<Movimiento> movimientos = new ListaDoble<>();
    private final Pila<Movimiento> operaciones = new Pila<>();
    private int consecutivo;

    /**
     * Registra un cargo o un pago.
     * @param deshacible si se apila para poder deshacerlo desde recepción.
     */
    public Movimiento registrar(TipoMovimiento tipo, String concepto, long monto, LocalDateTime fecha,
                                String usuario, boolean deshacible) {
        if (monto <= 0) {
            throw new ReglaNegocioException("El monto del movimiento debe ser mayor que cero");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaNegocioException("El concepto del movimiento es obligatorio");
        }
        Movimiento m = new Movimiento(++consecutivo, tipo, concepto.trim(), monto, fecha, null, usuario);
        movimientos.agregar(m);
        if (deshacible) {
            operaciones.apilar(m);
        }
        return m;
    }

    /** Deshace la última operación apilada registrando su contrario. */
    public Movimiento deshacerUltima(LocalDateTime fecha, String usuario) {
        if (operaciones.estaVacia()) {
            throw new EstructuraVaciaException("Pila de operaciones del folio (no hay nada que deshacer)");
        }
        Movimiento original = operaciones.desapilar();
        Movimiento reverso = new Movimiento(++consecutivo, original.tipo().contrario(),
                "Anulación #" + original.numero() + ": " + original.concepto(), original.monto(),
                fecha, original.numero(), usuario);
        movimientos.agregar(reverso);
        return reverso;
    }

    public Movimiento verUltimaOperacion() {
        return operaciones.estaVacia() ? null : operaciones.cima();
    }

    /** Saldo a cargo del huésped (positivo = debe; negativo = saldo a favor). */
    public long saldo() {
        long s = 0;
        for (Movimiento m : movimientos) {
            s += m.efectoEnSaldo();
        }
        return s;
    }

    public long totalCargos() {
        long s = 0;
        for (Movimiento m : movimientos) {
            if (m.tipo() == TipoMovimiento.CARGO) {
                s += m.monto();
            } else if (m.tipo() == TipoMovimiento.REVERSO_CARGO) {
                s -= m.monto();
            }
        }
        return s;
    }

    /** Pagos netos (pagos menos reversos de pago). */
    public long totalPagado() {
        long s = 0;
        for (Movimiento m : movimientos) {
            if (m.tipo() == TipoMovimiento.PAGO) {
                s += m.monto();
            } else if (m.tipo() == TipoMovimiento.REVERSO_PAGO) {
                s -= m.monto();
            }
        }
        return s;
    }

    public ListaDoble<Movimiento> getMovimientos() {
        return movimientos;
    }

    public int operacionesDeshacibles() {
        return operaciones.tamanio();
    }
}
