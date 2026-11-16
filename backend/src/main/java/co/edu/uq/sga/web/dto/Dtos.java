package co.edu.uq.sga.web.dto;

import co.edu.uq.sga.dominio.Rol;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Objetos de transferencia (solo arreglos: ninguna colección de java.util cruza la API). */
public final class Dtos {

    private Dtos() {
    }

    public record ApartamentoDto(String codigo, String nombre, String bloque, int piso, int dormitorios, int capacidad,
                                 String[] caracteristicas, long tarifaBaja, long tarifaMedia, long tarifaAlta,
                                 String descripcion, String imagenUrl, String estado, String estadoEtiqueta,
                                 String motivoFueraServicio, LocalDateTime pendienteDesde,
                                 LocalDateTime enPreparacionDesde, int totalReservas, int reservasActivas) {
    }

    public record HuespedDto(String documento, String nombre, LocalDate fechaNacimiento, String telefono, String correo,
                             String foto, String[] favoritos, int estancias, boolean cuentaActiva) {
    }

    public record EmpleadoDto(String documento, String nombre, Rol rol, String usuario, String correo, String telefono,
                              boolean activo) {
    }

    public record OcupanteDto(String nombre, String documento, LocalDate fechaNacimiento, int edad, boolean paga) {
    }

    public record NocheDto(LocalDate fecha, String temporada, long tarifaPorPersona, int personasQuePagan,
                           int personasExentas, long subtotal) {
    }

    public record MovimientoDto(int numero, String tipo, String tipoEtiqueta, String concepto, long monto, long efecto,
                                LocalDateTime fecha, Integer numeroReferencia, String usuario) {
    }

    public record FolioDto(String reserva, MovimientoDto[] movimientos, long totalCargos, long totalPagado, long saldo,
                           MovimientoDto ultimaOperacion, int deshacibles) {
    }

    public record ReservaDto(String codigo, String apartamentoCodigo, String apartamentoNombre, String imagenUrl,
                             String titularDocumento, String titularNombre, OcupanteDto[] ocupantes, LocalDate entrada,
                             LocalDate salida, int noches, String canal, String estado, String estadoEtiqueta,
                             LocalDateTime creadaEn, LocalDateTime venceEn, LocalTime horaLlegada, long total,
                             long anticipoRequerido, long anticipoPendiente, long pagado, long saldo,
                             LocalDateTime checkInEn, LocalDateTime checkOutEn, String motivoCierre,
                             String referenciaExterna, String observaciones, NocheDto[] desglose) {
    }

    public record CotizacionDto(String apartamentoCodigo, LocalDate entrada, LocalDate salida, int noches,
                                int personasQuePagan, int personasExentas, long total, long promedioNoche,
                                long anticipo, NocheDto[] desglose) {
    }

    public record OpcionDto(ApartamentoDto apartamento, long total, int noches, long promedioNoche) {
    }

    public record ItemPreparacionDto(String apartamentoCodigo, String apartamentoNombre, String bloque, int piso,
                                     int prioridad, boolean entradaHoy, LocalTime horaLlegada, String reservaEntrante,
                                     String huespedEntrante, LocalDateTime pendienteDesde) {
    }

    public record NodoDto(String id, String nombre, String tipo, String bloque, int piso, double x, double y,
                          String estadoApartamento) {
    }

    public record PasilloDto(String origen, String destino, double metros, boolean habilitado) {
    }

    public record MapaDto(NodoDto[] nodos, PasilloDto[] pasillos) {
    }

    public record RutaDto(String algoritmo, String[] camino, double metros, String[] paradas, String[] inalcanzables) {
    }

    public record GrupoBloqueDto(String bloque, String[] nodos, String[] apartamentos) {
    }

    public record SolicitudExternaDto(String id, String plataforma, String referencia, String apartamento,
                                      String titularNombre, String titularDocumento, LocalDate entrada, LocalDate salida,
                                      int personas, LocalDateTime recibidaEn) {
    }

    public record ProcesamientoDto(SolicitudExternaDto solicitud, String resultado, String reservaCodigo,
                                   String reservaEnConflicto, String motivo, LocalDateTime procesadaEn) {
    }

    public record TurnoDto(EmpleadoDto actual, String franja, EmpleadoDto[] siguientes) {
    }

    public record PropuestaGrupoDto(int personas, ApartamentoDto[] apartamentos, int capacidadTotal, long totalEstimado,
                                    int candidatosEvaluados) {
    }

    public record SesionDto(String token, Rol rol, String identificador, String nombre, String usuario,
                            LocalDateTime expira) {
    }

    public record MensajeDto(String mensaje) {
    }

    // ---- cuerpos de solicitud simples

    public record CambioEstadoApartamento(String estado, String motivo) {
    }

    public record CancelacionDto(String motivo) {
    }

    public record EstadoPasilloDto(String origen, String destino, boolean habilitado) {
    }

    public record CotizacionSolicitud(String apartamento, LocalDate entrada, LocalDate salida,
                                      co.edu.uq.sga.servicio.comandos.DatosOcupante[] ocupantes) {
    }
}
