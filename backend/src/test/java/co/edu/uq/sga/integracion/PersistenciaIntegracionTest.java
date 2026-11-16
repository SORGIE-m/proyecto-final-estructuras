package co.edu.uq.sga.integracion;

import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.persistencia.AlmacenEstado;
import co.edu.uq.sga.persistencia.EstadoSistema;
import co.edu.uq.sga.persistencia.PersistenciaEstado;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.servicio.ServicioEmpleados;
import co.edu.uq.sga.servicio.ServicioFolio;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.DatosMovimiento;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.servicio.comandos.SolicitudReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los datos sobreviven a un reinicio y una operación fallida no deja cambios a medias. */
class PersistenciaIntegracionTest extends PruebaIntegracion {

    @Autowired PersistenciaEstado persistencia;
    @Autowired AlmacenEstado almacen;
    @Autowired UnidadDeTrabajo uow;
    @Autowired ServicioReservas reservas;
    @Autowired ServicioFolio folio;
    @Autowired ServicioEmpleados empleados;
    @Autowired ServicioPreparacion preparacion;
    @Autowired RepositorioApartamentos apartamentos;

    @Test
    void elEstadoSeRecuperaCompletoDesdeElArchivo() {
        Reserva r = reservas.crearPortal("1094000003", new SolicitudReserva("C102", reloj.hoy().plusDays(45),
                reloj.hoy().plusDays(47), null, new DatosOcupante[]{new DatosOcupante("María", "", LocalDate.of(1990, 12, 11))}, null));
        folio.pagarAnticipo(r.getCodigo(), "maria");
        empleados.rotarTurno();

        // simula un reinicio: se descarta la memoria y se lee el archivo
        EstadoSistema leido = persistencia.cargar();
        almacen.reemplazar(leido);

        Reserva recuperada = reservas.obtener(r.getCodigo());
        assertThat(recuperada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(recuperada.getFolio().getMovimientos().tamanio()).isEqualTo(2);
        assertThat(apartamentos.buscar("C102").getReservas().contiene(recuperada))
                .as("el árbol de reservas del apartamento comparte la misma instancia").isTrue();
        assertThat(apartamentos.buscar("C102").getReservas().buscar(recuperada)).isSameAs(recuperada);
        assertThat(empleados.turnoActual().actual().getUsuario()).isEqualTo("phenao");
        assertThat(preparacion.cola().tamanio()).as("la cola de prioridad se reconstruye").isEqualTo(4);
    }

    @Test
    void unaEscrituraFallidaRevierteLosCambios() {
        assertThatThrownBy(() -> uow.ejecutar(() -> {
            apartamentos.eliminar("A101");
            throw new IllegalStateException("falla simulada");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(apartamentos.buscar("A101")).isNotNull();
        folio.registrarCargo("R-00019", new DatosMovimiento("Cargo válido", 1000), "recepcion");
        assertThat(reservas.obtener("R-00019").getFolio().saldo()).isEqualTo(1000);
    }
}
