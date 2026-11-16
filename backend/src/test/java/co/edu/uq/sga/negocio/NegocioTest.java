package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.Tarifas;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.dominio.TipoTemporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioTest {

    private static final LocalDate D = LocalDate.of(2026, 12, 13);

    /** Temporada alta desde el 15 de diciembre; el resto es baja. */
    private final ProveedorTemporadas temporadas = f -> f.isBefore(LocalDate.of(2026, 12, 15)) ? TipoTemporada.BAJA : TipoTemporada.ALTA;
    private final LiquidadorEstancia liquidador = new LiquidadorEstancia(temporadas);
    private final DetectorSolapamientos detector = new DetectorSolapamientos();

    private static Apartamento apto(String codigo, int capacidad) {
        return new Apartamento(codigo, "Apto " + codigo, "A", 1, 1, capacidad, new Tarifas(100_000, 120_000, 160_000));
    }

    private static Reserva reserva(String codigo, Apartamento a, LocalDate entrada, int noches, EstadoReserva estado) {
        Estancia e = new Estancia(entrada, entrada.plusDays(noches));
        return new Reserva(codigo, a.getCodigo(), "1", "Titular", ListaDoble.de(new Ocupante("T", "", LocalDate.of(1990, 1, 1))),
                e, Canal.DIRECTA, LocalDateTime.of(2026, 11, 1, 10, 0), null, new ListaDoble<>(), 300_000, estado);
    }

    @Test
    void liquidaNochePorNocheCruzandoTemporadasYExcluyeMenores() {
        Apartamento a = apto("A1", 4);
        ListaDoble<Ocupante> ocupantes = ListaDoble.de(
                new Ocupante("Adulto", "", LocalDate.of(1990, 1, 1)),
                new Ocupante("Joven", "", LocalDate.of(2015, 1, 1)),
                new Ocupante("Bebé", "", LocalDate.of(2024, 1, 1)));
        Liquidacion l = liquidador.liquidar(a, new Estancia(D, D.plusDays(4)), ocupantes); // 13,14 baja; 15,16 alta
        assertThat(l.noches()).isEqualTo(4);
        assertThat(l.personasQuePagan()).isEqualTo(2);
        assertThat(l.personasExentas()).isEqualTo(1);
        assertThat(l.desglose().flujo().map(n -> n.temporada())).containsExactly(
                TipoTemporada.BAJA, TipoTemporada.BAJA, TipoTemporada.ALTA, TipoTemporada.ALTA);
        assertThat(l.total()).isEqualTo(2 * (100_000 * 2 + 160_000 * 2));
    }

    @Test
    void detectaSolapamientosRecorriendoElArbolDelApartamento() {
        Apartamento a = apto("A1", 4);
        a.getReservas().insertar(reserva("R1", a, D, 3, EstadoReserva.CONFIRMADA));           // 13-16
        a.getReservas().insertar(reserva("R2", a, D.plusDays(5), 2, EstadoReserva.PENDIENTE)); // 18-20
        a.getReservas().insertar(reserva("R3", a, D.minusDays(10), 2, EstadoReserva.FINALIZADA));
        a.getReservas().insertar(reserva("R4", a, D.plusDays(3), 2, EstadoReserva.CANCELADA)); // 16-18 cancelada

        assertThat(detector.primerConflicto(a.getReservas(), new Estancia(D.plusDays(2), D.plusDays(4)), null).getCodigo())
                .isEqualTo("R1");
        assertThat(detector.primerConflicto(a.getReservas(), new Estancia(D.plusDays(3), D.plusDays(5)), null))
                .as("16-18 solo choca con una cancelada").isNull();
        assertThat(detector.primerConflicto(a.getReservas(), new Estancia(D.plusDays(6), D.plusDays(9)), null).getCodigo())
                .isEqualTo("R2");
        assertThat(detector.primerConflicto(a.getReservas(), new Estancia(D, D.plusDays(3)), "R1"))
                .as("excluye la propia reserva").isNull();
        assertThat(detector.reservasEnRango(a.getReservas(), new Estancia(D.minusDays(20), D.plusDays(30)), r -> true)
                .flujo().map(Reserva::getCodigo)).containsExactly("R3", "R1", "R4", "R2");
    }

    @Test
    void backtrackingUsaElMenorNumeroDeApartamentos() {
        ListaDoble<Apartamento> libres = ListaDoble.de(apto("P2a", 2), apto("P2b", 2), apto("P3", 3), apto("P4", 4),
                apto("P5", 5), apto("P8", 8));
        assertThat(new AsignadorGrupos().asignar(libres, 9).flujo().map(Apartamento::getCodigo))
                .as("dos apartamentos sin camas sobrantes").containsExactlyInAnyOrder("P5", "P4");
        assertThat(new AsignadorGrupos().asignar(libres, 7).flujo().map(Apartamento::getCodigo))
                .as("uno solo basta").containsExactly("P8");
        assertThat(new AsignadorGrupos().asignar(libres, 12).flujo().map(Apartamento::getCodigo))
                .containsExactlyInAnyOrder("P8", "P4");
        assertThat(new AsignadorGrupos().asignar(libres, 20).tamanio()).isEqualTo(4);
        assertThat(new AsignadorGrupos().asignar(libres, 25).estaVacia()).as("no alcanza la capacidad").isTrue();
    }

    @Test
    void politicaDeCancelacionSegunAnticipacion() {
        Apartamento a = apto("A1", 4);
        PoliticaCancelacion politica = new PoliticaCancelacionEstandar();
        Reserva r = reserva("R1", a, D, 3, EstadoReserva.CONFIRMADA);
        r.getFolio().registrar(TipoMovimiento.CARGO, "Alojamiento", 300_000, LocalDateTime.now(), "s", false);
        r.getFolio().registrar(TipoMovimiento.PAGO, "Anticipo", 90_000, LocalDateTime.now(), "s", false);
        assertThat(politica.calcular(r, D.minusDays(10)).reembolso()).isEqualTo(90_000);
        assertThat(politica.calcular(r, D.minusDays(3)).reembolso()).isEqualTo(45_000);
        assertThat(politica.calcular(r, D.minusDays(1)).reembolso()).isZero();
        assertThat(politica.calcular(r, D.minusDays(1)).retenido()).isEqualTo(90_000);
    }

    @Test
    void codificadorDeClavesConSal() {
        CodificadorClaves c = new CodificadorPbkdf2();
        String h1 = c.codificar("Secreta123*");
        String h2 = c.codificar("Secreta123*");
        assertThat(h1).isNotEqualTo(h2);
        assertThat(c.coincide("Secreta123*", h1)).isTrue();
        assertThat(c.coincide("otra", h1)).isFalse();
        assertThat(c.coincide("x", "mal-formato")).isFalse();
    }
}
