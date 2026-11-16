package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Reserva de un apartamento completo. Orden natural por fecha de entrada (desempata el código),
 * lo que permite guardarla en el árbol de reservas del apartamento.
 */
public class Reserva implements Comparable<Reserva>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Porcentaje del anticipo exigido a las reservas del portal. */
    public static final int PORCENTAJE_ANTICIPO = 30;
    public static final int HORAS_VENCIMIENTO = 24;
    public static final LocalTime HORA_LLEGADA_POR_DEFECTO = LocalTime.of(15, 0);

    private final String codigo;
    private final String apartamentoCodigo;
    private final String titularDocumento;
    private final String titularNombre;
    private final ListaDoble<Ocupante> ocupantes;
    private final Estancia estancia;
    private final Canal canal;
    private final LocalDateTime creadaEn;
    private final LocalTime horaLlegada;
    private final ListaDoble<NocheLiquidada> desglose;
    private final long total;
    private final Folio folio = new Folio();
    private EstadoReserva estado;
    private LocalDateTime venceEn;
    private LocalDateTime checkInEn;
    private LocalDateTime checkOutEn;
    private String motivoCierre;
    private String referenciaExterna;
    private String observaciones;

    public Reserva(String codigo, String apartamentoCodigo, String titularDocumento, String titularNombre,
                   ListaDoble<Ocupante> ocupantes, Estancia estancia, Canal canal, LocalDateTime creadaEn,
                   LocalTime horaLlegada, ListaDoble<NocheLiquidada> desglose, long total, EstadoReserva estadoInicial) {
        this.codigo = Objects.requireNonNull(codigo);
        this.apartamentoCodigo = apartamentoCodigo;
        this.titularDocumento = titularDocumento;
        this.titularNombre = titularNombre;
        this.ocupantes = ocupantes;
        this.estancia = estancia;
        this.canal = canal;
        this.creadaEn = creadaEn;
        this.horaLlegada = horaLlegada == null ? HORA_LLEGADA_POR_DEFECTO : horaLlegada;
        this.desglose = desglose;
        this.total = total;
        this.estado = estadoInicial;
        if (estadoInicial == EstadoReserva.PENDIENTE) {
            this.venceEn = creadaEn.plusHours(HORAS_VENCIMIENTO);
        }
    }

    public void transitar(EstadoReserva destino) {
        if (!estado.puedePasarA(destino)) {
            throw new TransicionEstadoInvalidaException("La reserva " + codigo + " no puede pasar de "
                    + estado.etiqueta() + " a " + destino.etiqueta());
        }
        estado = destino;
        if (destino != EstadoReserva.PENDIENTE) {
            venceEn = null;
        }
    }

    /** Anticipo mínimo (30 %) redondeado hacia arriba. */
    public long anticipoRequerido() {
        return (total * PORCENTAJE_ANTICIPO + 99) / 100;
    }

    public long anticipoPendiente() {
        return Math.max(0, anticipoRequerido() - folio.totalPagado());
    }

    public boolean esActiva() {
        return estado.esActiva();
    }

    public int personas() {
        return ocupantes.tamanio();
    }

    public int personasQuePagan() {
        int n = 0;
        for (Ocupante o : ocupantes) {
            if (o.pagaTarifa(estancia.entrada())) {
                n++;
            }
        }
        return n;
    }

    @Override
    public int compareTo(Reserva o) {
        int cmp = estancia.entrada().compareTo(o.estancia.entrada());
        return cmp != 0 ? cmp : codigo.compareTo(o.codigo);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reserva r && codigo.equals(r.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    public String getCodigo() { return codigo; }
    public String getApartamentoCodigo() { return apartamentoCodigo; }
    public String getTitularDocumento() { return titularDocumento; }
    public String getTitularNombre() { return titularNombre; }
    public ListaDoble<Ocupante> getOcupantes() { return ocupantes; }
    public Estancia getEstancia() { return estancia; }
    public Canal getCanal() { return canal; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalTime getHoraLlegada() { return horaLlegada; }
    public ListaDoble<NocheLiquidada> getDesglose() { return desglose; }
    public long getTotal() { return total; }
    public Folio getFolio() { return folio; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime venceEn) { this.venceEn = venceEn; }
    public LocalDateTime getCheckInEn() { return checkInEn; }
    public void setCheckInEn(LocalDateTime checkInEn) { this.checkInEn = checkInEn; }
    public LocalDateTime getCheckOutEn() { return checkOutEn; }
    public void setCheckOutEn(LocalDateTime checkOutEn) { this.checkOutEn = checkOutEn; }
    public String getMotivoCierre() { return motivoCierre; }
    public void setMotivoCierre(String motivoCierre) { this.motivoCierre = motivoCierre; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
