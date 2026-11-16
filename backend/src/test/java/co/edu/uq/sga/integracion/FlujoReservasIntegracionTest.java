package co.edu.uq.sga.integracion;

import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.Movimiento;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.ResultadoSolicitud;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.CapacidadExcedidaException;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;
import co.edu.uq.sga.negocio.Reembolso;
import co.edu.uq.sga.persistencia.EstadoSistema;
import co.edu.uq.sga.persistencia.PersistenciaEstado;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.servicio.ServicioApartamentos;
import co.edu.uq.sga.servicio.ServicioCanalExterno;
import co.edu.uq.sga.servicio.ServicioDisponibilidad;
import co.edu.uq.sga.servicio.ServicioFolio;
import co.edu.uq.sga.servicio.ServicioGrupos;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.DatosMovimiento;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.servicio.comandos.DatosTitular;
import co.edu.uq.sga.servicio.comandos.SolicitudGrupo;
import co.edu.uq.sga.servicio.comandos.SolicitudReserva;
import co.edu.uq.sga.servicio.comandos.SolicitudReservaDirecta;
import co.edu.uq.sga.servicio.resultados.OpcionDisponible;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas centrales del SGA ejecutadas sobre el escenario inicial. */
class FlujoReservasIntegracionTest extends PruebaIntegracion {

    private static final String LAURA = "1094000001";

    @Autowired ServicioReservas reservas;
    @Autowired ServicioFolio folio;
    @Autowired ServicioPreparacion preparacion;
    @Autowired ServicioApartamentos apartamentos;
    @Autowired ServicioHuespedes huespedes;
    @Autowired ServicioDisponibilidad disponibilidad;
    @Autowired ServicioCanalExterno canalExterno;
    @Autowired ServicioGrupos grupos;
    @Autowired RepositorioReservas repositorioReservas;
    @Autowired PersistenciaEstado persistencia;

    private LocalDate hoy() {
        return reloj.hoy();
    }

    private static DatosOcupante[] adultos(int n) {
        DatosOcupante[] o = new DatosOcupante[n];
        for (int i = 0; i < n; i++) {
            o[i] = new DatosOcupante("Persona " + (i + 1), "", LocalDate.of(1990, 1, 1));
        }
        return o;
    }

    private static DatosTitular titular() {
        return new DatosTitular("2000000001", "Titular Prueba", LocalDate.of(1985, 5, 5), "3000000000", "titular@prueba.com");
    }

    private SolicitudReservaDirecta directa(String apto, LocalDate entrada, int noches, int personas) {
        return new SolicitudReservaDirecta(titular(), apto, entrada, entrada.plusDays(noches), null, adultos(personas), null);
    }

    @Test
    void reservaPorPortalQuedaPendienteYSeConfirmaAlPagarElAnticipo() {
        Reserva r = reservas.crearPortal(LAURA, new SolicitudReserva("B102", hoy().plusDays(30), hoy().plusDays(33),
                null, adultos(2), "Llegamos tarde"));
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(r.getCanal()).isEqualTo(Canal.PORTAL);
        assertThat(r.getVenceEn()).isEqualTo(reloj.ahora().plusHours(24));
        assertThat(r.anticipoRequerido()).isEqualTo((r.getTotal() * 30 + 99) / 100);

        folio.pagarAnticipo(r.getCodigo(), "laura");
        Reserva confirmada = reservas.obtener(r.getCodigo());
        assertThat(confirmada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(confirmada.getVenceEn()).isNull();
        assertThat(reservas.pendientesPorVencer().flujo().map(Reserva::getCodigo)).doesNotContain(r.getCodigo());
    }

    @Test
    void lasPendientesVencenALas24HorasYLiberanSusNoches() {
        LocalDate entrada = hoy().plusDays(40);
        Reserva r = reservas.crearPortal(LAURA, new SolicitudReserva("B102", entrada, entrada.plusDays(2), null, adultos(1), null));
        assertThat(reservas.vencerPendientes().flujo().map(Reserva::getCodigo))
                .as("la del escenario creada hace 26 h vence de inmediato").containsExactly("R-00026");

        relojAjustable().adelantar(Duration.ofHours(25));
        ListaDoble<Reserva> vencidas = reservas.vencerPendientes();
        assertThat(vencidas.flujo().map(Reserva::getCodigo)).contains(r.getCodigo());
        Reserva cancelada = reservas.obtener(r.getCodigo());
        assertThat(cancelada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(cancelada.getMotivoCierre()).contains("Vencida");
        // las noches quedaron libres para otro canal
        Reserva otra = reservas.crearDirecta(directa("B102", entrada, 2, 2));
        assertThat(otra.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void ningunCanalPuedeVenderDosVecesLaMismaNoche() {
        LocalDate entrada = hoy().plusDays(10); // A101 ya tiene R-00024 del +10 al +14
        assertThatThrownBy(() -> reservas.crearDirecta(directa("A101", entrada.plusDays(2), 1, 1)))
                .isInstanceOf(ReservaSolapadaException.class)
                .hasMessageContaining("R-00024");
        assertThatThrownBy(() -> reservas.crearPortal(LAURA, new SolicitudReserva("A101", entrada.minusDays(1),
                entrada.plusDays(1), null, adultos(1), null))).isInstanceOf(ReservaSolapadaException.class);
        // la noche de salida queda libre: del +14 al +16 sí se puede
        assertThat(reservas.crearDirecta(directa("A101", entrada.plusDays(4), 2, 1)).getEstado())
                .isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void validaCapacidadFechasYMenoresAcompanados() {
        assertThatThrownBy(() -> reservas.crearDirecta(directa("A202", hoy().plusDays(30), 2, 3)))
                .isInstanceOf(CapacidadExcedidaException.class);
        assertThatThrownBy(() -> reservas.crearDirecta(directa("A202", hoy().minusDays(1), 2, 1)))
                .isInstanceOf(FechasInvalidasException.class);
        assertThatThrownBy(() -> reservas.crearDirecta(directa("A202", hoy().plusDays(5), 0, 1)))
                .isInstanceOf(FechasInvalidasException.class);
        DatosOcupante[] soloNinos = {new DatosOcupante("Bebé", "", LocalDate.of(2024, 1, 1))};
        assertThatThrownBy(() -> reservas.crearDirecta(new SolicitudReservaDirecta(titular(), "A202",
                hoy().plusDays(30), hoy().plusDays(31), null, soloNinos, null))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> reservas.crearDirecta(directa("B301", hoy().plusDays(30), 1, 1)))
                .as("B301 está fuera de servicio").isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("calentador");
    }

    @Test
    void unaSolicitudRechazadaNoDejaRastros() {
        int antes = repositorioReservas.todas().tamanio();
        assertThatThrownBy(() -> reservas.crearDirecta(new SolicitudReservaDirecta(
                new DatosTitular("3000000001", "Menor de edad", LocalDate.of(2012, 1, 1), "300", "menor@prueba.com"),
                "B102", hoy().plusDays(50), hoy().plusDays(51), null, adultos(1), null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("mayor de edad");
        assertThat(repositorioReservas.todas().tamanio()).isEqualTo(antes);
        assertThat(huespedes.listar("3000000001").estaVacia()).as("se revirtió la creación del titular").isTrue();
        EstadoSistema guardado = persistencia.cargar();
        assertThat(guardado).isNotNull();
    }

    @Test
    void checkInExigeApartamentoPreparadoYCheckOutLoEnviaALaColaDePreparacion() {
        // R-00021: A201 con entrada hoy pero el apartamento está pendiente de preparación
        assertThatThrownBy(() -> reservas.checkIn("R-00021")).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está preparado");

        ListaDoble<ItemPreparacion> cola = preparacion.cola();
        assertThat(cola.flujo().map(ItemPreparacion::apartamentoCodigo))
                .as("prioridad 1 por hora de llegada (11:30, 14:00) y luego prioridad 2")
                .containsExactly("B101", "A201", "B302", "C102");

        assertThat(preparacion.despacharSiguiente("Pedro").apartamentoCodigo()).isEqualTo("B101");
        assertThat(apartamentos.obtener("B101").getEstado()).isEqualTo(EstadoApartamento.EN_PREPARACION);
        preparacion.iniciar("A201", "Paula");
        relojAjustable().adelantar(Duration.ofMinutes(45));
        preparacion.finalizar("A201", "Paula");
        assertThat(apartamentos.obtener("A201").getEstado()).isEqualTo(EstadoApartamento.PREPARADO);

        Reserva enCurso = reservas.checkIn("R-00021");
        assertThat(enCurso.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        assertThat(apartamentos.obtener("A201").getEstado()).isEqualTo(EstadoApartamento.OCUPADO);

        // R-00020: C101 sale hoy y tiene el folio pagado
        Reserva finalizada = reservas.checkOut("R-00020");
        assertThat(finalizada.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(apartamentos.obtener("C101").getEstado()).isEqualTo(EstadoApartamento.PENDIENTE_PREPARACION);
        assertThat(preparacion.cola().flujo().map(ItemPreparacion::apartamentoCodigo)).contains("C101");
        Huesped julian = huespedes.obtener(finalizada.getTitularDocumento());
        assertThat(julian.getHistorial().primero()).isEqualTo("R-00020");
    }

    @Test
    void noSePuedeHacerCheckOutConSaldoPendiente() {
        // R-00018: A102, solo pagó el anticipo y tiene un cargo de lavandería
        assertThatThrownBy(() -> reservas.checkOut("R-00018")).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("saldo pendiente");
        long saldo = reservas.obtener("R-00018").getFolio().saldo();
        folio.registrarPago("R-00018", new DatosMovimiento("Pago con tarjeta", saldo), "recepcion");
        assertThat(reservas.checkOut("R-00018").getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
    }

    @Test
    void deshacerEnElFolioRegistraElMovimientoContrarioSinBorrar() {
        Reserva r = reservas.obtener("R-00019");
        int movimientos = r.getFolio().getMovimientos().tamanio();
        long saldoInicial = r.getFolio().saldo();
        folio.registrarCargo("R-00019", new DatosMovimiento("Cena típica", 80_000), "recepcion");
        folio.registrarPago("R-00019", new DatosMovimiento("Efectivo", 50_000), "recepcion");
        Movimiento reversoPago = folio.deshacer("R-00019", "recepcion");
        Movimiento reversoCargo = folio.deshacer("R-00019", "recepcion");
        assertThat(reversoPago.tipo()).isEqualTo(TipoMovimiento.REVERSO_PAGO);
        assertThat(reversoCargo.tipo()).isEqualTo(TipoMovimiento.REVERSO_CARGO);
        Reserva despues = reservas.obtener("R-00019");
        assertThat(despues.getFolio().saldo()).isEqualTo(saldoInicial);
        assertThat(despues.getFolio().getMovimientos().tamanio()).isEqualTo(movimientos + 4);
        assertThatThrownBy(() -> folio.deshacer("R-00019", "recepcion")).isInstanceOf(EstructuraVaciaException.class);
        assertThatThrownBy(() -> folio.registrarPago("R-00019", new DatosMovimiento("Exceso", 1), "recepcion"))
                .as("no se puede pagar más que el saldo").isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cancelacionMuestraElReembolsoYDejaElFolioEnCero() {
        Reembolso previo = reservas.previsualizarCancelacion("R-00024"); // A101, +10 días, anticipo pagado
        assertThat(previo.porcentaje()).isEqualTo(100);
        assertThat(previo.reembolso()).isEqualTo(previo.pagado()).isPositive();
        Reserva cancelada = reservas.cancelar("R-00024", "Cambio de planes", "laura");
        assertThat(cancelada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(cancelada.getFolio().saldo()).isZero();
        assertThat(cancelada.getFolio().totalPagado()).isZero();
        assertThatThrownBy(() -> reservas.cancelar("R-00024", null, "laura"))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
        assertThatThrownBy(() -> reservas.cancelar("R-00019", null, "laura"))
                .as("una estancia en curso no se cancela").isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void noShowRetieneLoPagadoYAnulaElRestoDelCargo() {
        assertThatThrownBy(() -> reservas.marcarNoShow("R-00023")).isInstanceOf(FechasInvalidasException.class);
        relojAjustable().adelantar(Duration.ofDays(1));
        Reserva r = reservas.marcarNoShow("R-00023");
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.NO_SHOW);
        assertThat(r.getFolio().saldo()).isZero();
    }

    @Test
    void canalExternoProcesaEnOrdenFifoYRegistraConflictos() {
        assertThat(canalExterno.pendientes().flujo().map(s -> s.referenciaExterna()))
                .containsExactly("BKS-551201", "TRN-778812", "ALO-102030", "BKS-551299", "ALO-102077");
        assertThat(canalExterno.procesarSiguiente().resultado()).isEqualTo(ResultadoSolicitud.ACEPTADA);
        var resultados = canalExterno.procesarTodas();
        assertThat(resultados.flujo().map(p -> p.resultado())).containsExactly(ResultadoSolicitud.CONFLICTO,
                ResultadoSolicitud.ACEPTADA, ResultadoSolicitud.CONFLICTO, ResultadoSolicitud.RECHAZADA);
        assertThat(resultados.primero().reservaEnConflicto()).isEqualTo("R-00024");
        assertThat(canalExterno.pendientes().estaVacia()).isTrue();
        assertThatThrownBy(() -> canalExterno.procesarSiguiente()).isInstanceOf(EstructuraVaciaException.class);
        assertThat(reservas.listar(null, Canal.EXTERNA, "BKS-551201").tamanio()).isEqualTo(1);
    }

    @Test
    void disponibilidadOrdenadaYGruposConBacktracking() {
        LocalDate entrada = hoy().plusDays(30);
        ListaDoble<OpcionDisponible> porPrecio = disponibilidad.buscar(entrada, entrada.plusDays(2), 2, "precio");
        assertThat(porPrecio.flujo().map(o -> o.apartamento().getCodigo())).doesNotContain("B301");
        for (int i = 1; i < porPrecio.tamanio(); i++) {
            assertThat(porPrecio.obtener(i).cotizacion().total())
                    .isGreaterThanOrEqualTo(porPrecio.obtener(i - 1).cotizacion().total());
        }
        ListaDoble<OpcionDisponible> porCapacidad = disponibilidad.buscar(entrada, entrada.plusDays(2), 2, "capacidad");
        assertThat(porCapacidad.primero().apartamento().getCodigo()).isEqualTo("C201");

        var propuesta = disponibilidad.proponerGrupo(entrada, entrada.plusDays(2), 13);
        assertThat(propuesta.apartamentos().tamanio()).isEqualTo(2);
        assertThat(propuesta.capacidadTotal()).isGreaterThanOrEqualTo(13);

        ListaDoble<Reserva> creadas = grupos.reservarGrupo(new SolicitudGrupo(titular(), entrada, entrada.plusDays(2),
                null, adultos(13)));
        assertThat(creadas.tamanio()).isEqualTo(2);
        assertThat(creadas.flujo().mapToInt(Reserva::personas).sum()).isEqualTo(13);
    }

    @Test
    void favoritosSinRepetidos() {
        assertThat(huespedes.agregarFavorito(LAURA, "B202")).isTrue();
        assertThat(huespedes.agregarFavorito(LAURA, "b202")).isFalse();
        assertThat(huespedes.favoritos(LAURA).flujo().map(a -> a.getCodigo())).containsExactly("A101", "C201", "A302", "B202");
        assertThat(huespedes.historial(LAURA).tamanio()).isEqualTo(3);
    }
}
