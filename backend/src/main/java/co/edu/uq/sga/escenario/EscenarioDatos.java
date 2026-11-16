package co.edu.uq.sga.escenario;

import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.dominio.TipoNodo;
import co.edu.uq.sga.dominio.TipoTemporada;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Estructura del archivo plano (JSON) de un escenario de prueba. Las fechas de reservas son
 * desplazamientos en días respecto a "hoy", para que la demostración siempre tenga datos vigentes.
 */
public record EscenarioDatos(String nombre, TemporadaE[] temporadas, ApartamentoE[] apartamentos, NodoE[] nodos,
                             PasilloE[] pasillos, HuespedE[] huespedes, EmpleadoE[] empleados, ReservaE[] reservas,
                             SolicitudE[] solicitudesExternas, ConflictoE[] conflictosHistoricos,
                             PreparacionE[] preparaciones) {

    /** inicio/fin en formato MM-dd; se replican para el año anterior, el actual y el siguiente. */
    public record TemporadaE(String nombre, TipoTemporada tipo, String inicio, String fin) {
    }

    public record ApartamentoE(String codigo, String nombre, String bloque, int piso, int dormitorios, int capacidad,
                               String[] caracteristicas, long tarifaBaja, long tarifaMedia, long tarifaAlta,
                               String descripcion, String imagenUrl, EstadoApartamento estado, String motivo) {
    }

    public record NodoE(String id, String nombre, TipoNodo tipo, String bloque, int piso, double x, double y) {
    }

    public record PasilloE(String origen, String destino, double metros, Boolean habilitado) {
    }

    public record HuespedE(String documento, String nombre, LocalDate fechaNacimiento, String telefono, String correo,
                           String clave, String[] favoritos) {
    }

    public record EmpleadoE(String documento, String nombre, Rol rol, String usuario, String correo, String telefono,
                            String clave) {
    }

    public record OcupanteE(String nombre, String documento, LocalDate fechaNacimiento) {
    }

    public record CargoE(String concepto, long monto) {
    }

    /** pago: TOTAL, ANTICIPO o NINGUNO. creadaDiasAntes: días antes de la entrada (o horas si se indica). */
    public record ReservaE(String apartamento, String titular, OcupanteE[] ocupantes, int entrada, int noches,
                           Canal canal, EstadoReserva estado, LocalTime horaLlegada, String pago,
                           Integer creadaDiasAntes, Integer creadaHorasAtras, CargoE[] cargos) {
    }

    public record TitularE(String documento, String nombre, LocalDate fechaNacimiento, String telefono, String correo) {
    }

    public record SolicitudE(String plataforma, String referencia, String apartamento, TitularE titular, int entrada,
                             int noches, LocalTime horaLlegada, OcupanteE[] ocupantes) {
    }

    public record ConflictoE(String plataforma, String apartamento, int entrada, int noches, String titular,
                             int diasAtras) {
    }

    public record PreparacionE(String apartamento, int diasAtras, int esperaMinutos, int duracionMinutos,
                               String responsable) {
    }
}
