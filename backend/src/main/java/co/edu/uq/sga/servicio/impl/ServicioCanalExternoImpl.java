package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.ResultadoSolicitud;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.SgaException;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioCanalExterno;
import co.edu.uq.sga.servicio.ServicioCanalExterno;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.servicio.comandos.DatosSolicitudExterna;
import co.edu.uq.sga.servicio.comandos.DatosTitular;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.random.RandomGenerator;

@Service
public class ServicioCanalExternoImpl implements ServicioCanalExterno {

    private static final String[] PLATAFORMAS = {"BookStay", "TripNest", "AlojaYa"};
    private static final String[] NOMBRES = {"Valentina Ríos", "Santiago Mejía", "Camila Ospina", "Mateo Cárdenas",
            "Isabella Gómez", "Samuel Restrepo", "Mariana López", "Tomás Herrera", "Luciana Duque", "Emilio Vargas"};

    private final RepositorioCanalExterno repositorio;
    private final RepositorioApartamentos apartamentos;
    private final ServicioReservas reservas;
    private final ServicioHuespedes huespedes;
    private final DetectorSolapamientos detector;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;
    private final RandomGenerator aleatorio = RandomGenerator.getDefault();

    public ServicioCanalExternoImpl(RepositorioCanalExterno repositorio, RepositorioApartamentos apartamentos,
                                    ServicioReservas reservas, ServicioHuespedes huespedes,
                                    DetectorSolapamientos detector, UnidadDeTrabajo uow, Reloj reloj) {
        this.repositorio = repositorio;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.huespedes = huespedes;
        this.detector = detector;
        this.uow = uow;
        this.reloj = reloj;
    }

    @Override
    public SolicitudExterna recibir(DatosSolicitudExterna d) {
        return uow.escribir(() -> {
            ListaDoble<Ocupante> ocupantes = new ListaDoble<>();
            for (DatosOcupante o : d.ocupantes()) {
                ocupantes.agregar(o.aDominio());
            }
            DatosTitular t = d.titular();
            String id = repositorio.siguienteId();
            SolicitudExterna s = new SolicitudExterna(id, d.plataforma().trim(),
                    d.referencia() == null || d.referencia().isBlank() ? id : d.referencia().trim(),
                    d.apartamentoCodigo().trim().toUpperCase(), t.documento().trim(), t.nombre().trim(),
                    t.fechaNacimiento(), t.telefono().trim(), t.correo().trim().toLowerCase(), ocupantes,
                    d.entrada(), d.salida(), d.horaLlegada(), reloj.ahora());
            repositorio.cola().encolar(s);
            return s;
        });
    }

    @Override
    public ListaDoble<SolicitudExterna> simular(int cantidad) {
        if (cantidad < 1 || cantidad > 20) {
            throw new ReglaNegocioException("Se pueden simular entre 1 y 20 solicitudes");
        }
        return uow.escribir(() -> {
            ListaDoble<Apartamento> aptos = apartamentos.arbol().enInorden().filtrar(Apartamento::admiteReservas);
            if (aptos.estaVacia()) {
                throw new ReglaNegocioException("No hay apartamentos para simular solicitudes");
            }
            ListaDoble<SolicitudExterna> creadas = new ListaDoble<>();
            for (int i = 0; i < cantidad; i++) {
                Apartamento a = aptos.obtener(aleatorio.nextInt(aptos.tamanio()));
                LocalDate entrada = reloj.hoy().plusDays(1 + aleatorio.nextInt(40));
                LocalDate salida = entrada.plusDays(1 + aleatorio.nextInt(5));
                String nombre = NOMBRES[aleatorio.nextInt(NOMBRES.length)];
                String documento = String.valueOf(1_000_000_000L + aleatorio.nextLong(8_999_999_999L));
                LocalDate nacimiento = LocalDate.of(1965 + aleatorio.nextInt(35), 1 + aleatorio.nextInt(12),
                        1 + aleatorio.nextInt(28));
                int personas = 1 + aleatorio.nextInt(Math.max(1, a.getCapacidad()));
                DatosOcupante[] ocupantes = new DatosOcupante[personas];
                ocupantes[0] = new DatosOcupante(nombre, documento, nacimiento);
                for (int k = 1; k < personas; k++) {
                    ocupantes[k] = new DatosOcupante("Acompañante " + k, "",
                            reloj.hoy().minusYears(3 + aleatorio.nextInt(50)));
                }
                String plataforma = PLATAFORMAS[aleatorio.nextInt(PLATAFORMAS.length)];
                String correo = nombre.toLowerCase().replace(' ', '.')
                        .replace("í", "i").replace("é", "e").replace("á", "a").replace("ó", "o")
                        + "." + documento.substring(6) + "@correo.com";
                creadas.agregar(recibir(new DatosSolicitudExterna(plataforma,
                        plataforma.substring(0, 3).toUpperCase() + "-" + (100000 + aleatorio.nextInt(900000)),
                        a.getCodigo(), new DatosTitular(documento, nombre, nacimiento, "3" + (100000000 + aleatorio.nextInt(899999999)), correo),
                        entrada, salida, LocalTime.of(13 + aleatorio.nextInt(8), 0), ocupantes)));
            }
            return creadas;
        });
    }

    @Override
    public ListaDoble<SolicitudExterna> pendientes() {
        return uow.leer(() -> ListaDoble.desde(repositorio.cola()));
    }

    @Override
    public ProcesamientoExterno procesarSiguiente() {
        return uow.escribir(() -> {
            SolicitudExterna s = repositorio.cola().desencolar(); // FIFO; lanza EstructuraVacia si no hay
            ProcesamientoExterno p = procesar(s);
            repositorio.procesamientos().agregarAlInicio(p);
            return p;
        });
    }

    private ProcesamientoExterno procesar(SolicitudExterna s) {
        try {
            Huesped titular = huespedes.obtenerOCrear(new DatosTitular(s.titularDocumento(), s.titularNombre(),
                    s.titularNacimiento(), s.titularTelefono(), s.titularCorreo()));
            Reserva r = reservas.registrar(titular, s.apartamentoCodigo(), s.entrada(), s.salida(), s.horaLlegada(),
                    s.ocupantes(), Canal.EXTERNA, "Solicitud " + s.plataforma() + " " + s.referenciaExterna(),
                    s.referenciaExterna());
            return new ProcesamientoExterno(s, ResultadoSolicitud.ACEPTADA, r.getCodigo(), null,
                    "Reserva creada y confirmada", reloj.ahora());
        } catch (ReservaSolapadaException e) {
            Apartamento a = apartamentos.buscar(s.apartamentoCodigo());
            Reserva choque = a == null ? null : detector.primerConflicto(a.getReservas(),
                    new co.edu.uq.sga.dominio.Estancia(s.entrada(), s.salida()), null);
            return new ProcesamientoExterno(s, ResultadoSolicitud.CONFLICTO, null,
                    choque == null ? null : choque.getCodigo(), e.getMessage(), reloj.ahora());
        } catch (SgaException e) {
            return new ProcesamientoExterno(s, ResultadoSolicitud.RECHAZADA, null, null, e.getMessage(), reloj.ahora());
        }
    }

    @Override
    public ListaDoble<ProcesamientoExterno> procesarTodas() {
        return uow.escribir(() -> {
            ListaDoble<ProcesamientoExterno> resultados = new ListaDoble<>();
            while (!repositorio.cola().estaVacia()) {
                resultados.agregar(procesarSiguiente());
            }
            return resultados;
        });
    }

    @Override
    public ListaDoble<ProcesamientoExterno> historial() {
        return uow.leer(() -> ListaDoble.desde(repositorio.procesamientos()));
    }
}
