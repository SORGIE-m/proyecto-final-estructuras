package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 9, 0);

    @Test
    void estanciaEsIterableYNoCobraLaNocheDeSalida() {
        Estancia e = new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        ListaDoble<LocalDate> noches = new ListaDoble<>();
        for (LocalDate noche : e) {
            noches.agregar(noche);
        }
        assertThat(e.noches()).isEqualTo(2);
        assertThat(noches).containsExactly(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11));
        assertThat(e.incluyeNoche(LocalDate.of(2026, 3, 12))).isFalse();
    }

    @Test
    void estanciasQueSoloSeTocanNoSeSolapan() {
        Estancia a = new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        Estancia b = new Estancia(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 14));
        Estancia c = new Estancia(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 13));
        assertThat(a.solapaCon(b)).isFalse();
        assertThat(a.solapaCon(c)).isTrue();
        assertThat(c.solapaCon(b)).isTrue();
    }

    @Test
    void estanciaValidaFechas() {
        LocalDate d = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> new Estancia(d, d)).isInstanceOf(FechasInvalidasException.class);
        assertThatThrownBy(() -> new Estancia(d, d.minusDays(1))).isInstanceOf(FechasInvalidasException.class);
        assertThatThrownBy(() -> new Estancia(d, d.plusDays(61))).isInstanceOf(FechasInvalidasException.class);
        assertThatThrownBy(() -> new Estancia(null, d)).isInstanceOf(FechasInvalidasException.class);
    }

    @Test
    void menoresDeSeisNoPagan() {
        LocalDate entrada = LocalDate.of(2026, 11, 20);
        assertThat(new Ocupante("Niña", "", LocalDate.of(2020, 11, 21)).pagaTarifa(entrada)).isFalse();
        assertThat(new Ocupante("Niño", "", LocalDate.of(2020, 11, 20)).pagaTarifa(entrada)).isTrue();
    }

    @Test
    void folioDeshaceRegistrandoElMovimientoContrario() {
        Folio f = new Folio();
        f.registrar(TipoMovimiento.CARGO, "Alojamiento", 500_000, AHORA, "sistema", false);
        f.registrar(TipoMovimiento.CARGO, "Minibar", 40_000, AHORA, "recepcion", true);
        f.registrar(TipoMovimiento.PAGO, "Pago", 100_000, AHORA, "recepcion", true);
        assertThat(f.saldo()).isEqualTo(440_000);

        Movimiento reversoPago = f.deshacerUltima(AHORA, "recepcion");
        assertThat(reversoPago.tipo()).isEqualTo(TipoMovimiento.REVERSO_PAGO);
        assertThat(reversoPago.numeroReferencia()).isEqualTo(3);
        assertThat(f.saldo()).isEqualTo(540_000);

        Movimiento reversoCargo = f.deshacerUltima(AHORA, "recepcion");
        assertThat(reversoCargo.tipo()).isEqualTo(TipoMovimiento.REVERSO_CARGO);
        assertThat(f.saldo()).isEqualTo(500_000);
        // los movimientos nunca se borran
        assertThat(f.getMovimientos().tamanio()).isEqualTo(5);
        // el cargo del sistema no es deshacible
        assertThatThrownBy(() -> f.deshacerUltima(AHORA, "recepcion")).isInstanceOf(EstructuraVaciaException.class);
        assertThat(f.totalCargos()).isEqualTo(500_000);
        assertThat(f.totalPagado()).isZero();
    }

    @Test
    void folioRechazaMontosInvalidos() {
        Folio f = new Folio();
        assertThatThrownBy(() -> f.registrar(TipoMovimiento.PAGO, "x", 0, AHORA, "u", true))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cicloDeVidaDeLaReserva() {
        assertThat(EstadoReserva.PENDIENTE.puedePasarA(EstadoReserva.CONFIRMADA)).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.puedePasarA(EstadoReserva.EN_CURSO)).isTrue();
        assertThat(EstadoReserva.EN_CURSO.puedePasarA(EstadoReserva.FINALIZADA)).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.puedePasarA(EstadoReserva.NO_SHOW)).isTrue();
        assertThat(EstadoReserva.PENDIENTE.puedePasarA(EstadoReserva.EN_CURSO)).isFalse();
        assertThat(EstadoReserva.FINALIZADA.puedePasarA(EstadoReserva.CANCELADA)).isFalse();
        assertThat(EstadoReserva.EN_CURSO.puedePasarA(EstadoReserva.CANCELADA)).isFalse();
    }

    @Test
    void apartamentoExigeMotivoParaFueraDeServicioYValidaTransiciones() {
        Apartamento a = new Apartamento("x1", "Prueba", "a", 1, 1, 2, new Tarifas(1, 2, 3));
        assertThat(a.getCodigo()).isEqualTo("X1");
        assertThatThrownBy(() -> a.cambiarEstado(EstadoApartamento.FUERA_SERVICIO, " ", AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        a.cambiarEstado(EstadoApartamento.OCUPADO, null, AHORA);
        assertThatThrownBy(() -> a.cambiarEstado(EstadoApartamento.PREPARADO, null, AHORA))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        a.cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION, null, AHORA);
        assertThat(a.getPendienteDesde()).isEqualTo(AHORA);
        a.cambiarEstado(EstadoApartamento.EN_PREPARACION, null, AHORA.plusMinutes(10));
        assertThat(a.getPendienteDesde()).isEqualTo(AHORA);
        a.cambiarEstado(EstadoApartamento.FUERA_SERVICIO, "Fuga de agua", AHORA);
        assertThat(a.getMotivoFueraServicio()).isEqualTo("Fuga de agua");
        assertThat(a.admiteReservas()).isFalse();
    }
}
