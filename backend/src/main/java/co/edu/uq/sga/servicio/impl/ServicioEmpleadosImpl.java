package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.estructuras.ListaCircular;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.DatoDuplicadoException;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.negocio.CodificadorClaves;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioEmpleados;
import co.edu.uq.sga.repositorio.RepositorioTurnos;
import co.edu.uq.sga.servicio.ServicioEmpleados;
import co.edu.uq.sga.servicio.comandos.DatosEmpleado;
import co.edu.uq.sga.servicio.resultados.TurnoRecepcion;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

@Service
public class ServicioEmpleadosImpl implements ServicioEmpleados {

    private final RepositorioEmpleados empleados;
    private final RepositorioTurnos turnos;
    private final CodificadorClaves codificador;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioEmpleadosImpl(RepositorioEmpleados empleados, RepositorioTurnos turnos,
                                 CodificadorClaves codificador, UnidadDeTrabajo uow, Reloj reloj) {
        this.empleados = empleados;
        this.turnos = turnos;
        this.codificador = codificador;
        this.uow = uow;
        this.reloj = reloj;
    }

    @Override
    public Empleado crear(DatosEmpleado d) {
        return uow.escribir(() -> {
            if (empleados.buscar(d.documento()) != null) {
                throw new DatoDuplicadoException("Ya existe un empleado con documento " + d.documento().trim());
            }
            if (empleados.buscarPorUsuario(d.usuario()) != null) {
                throw new DatoDuplicadoException("El usuario " + d.usuario() + " ya está en uso");
            }
            if (d.clave() == null || d.clave().length() < 8) {
                throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres");
            }
            Empleado e = new Empleado(d.documento(), d.nombre(), d.rol(), d.usuario(), d.correo(), d.telefono());
            e.setClaveHash(codificador.codificar(d.clave()));
            e.setActivo(d.activo() == null || d.activo());
            empleados.guardar(e);
            sincronizarTurno(e);
            return e;
        });
    }

    @Override
    public Empleado actualizar(String documento, DatosEmpleado d) {
        return uow.escribir(() -> {
            Empleado e = requerir(documento);
            Empleado otro = empleados.buscarPorUsuario(d.usuario());
            if (otro != null && !otro.equals(e)) {
                throw new DatoDuplicadoException("El usuario " + d.usuario() + " ya está en uso");
            }
            e.actualizar(d.nombre(), d.rol(), d.usuario(), d.correo(), d.telefono());
            if (d.activo() != null) {
                e.setActivo(d.activo());
            }
            if (d.clave() != null && !d.clave().isBlank()) {
                e.setClaveHash(codificador.codificar(d.clave()));
            }
            sincronizarTurno(e);
            return e;
        });
    }

    /** Solo los recepcionistas activos participan en la rotación. */
    private void sincronizarTurno(Empleado e) {
        ListaCircular<String> lista = turnos.turnos();
        boolean debeEstar = e.isActivo() && e.getRol() == Rol.RECEPCIONISTA;
        if (debeEstar && !lista.contiene(e.getDocumento())) {
            lista.agregar(e.getDocumento());
        } else if (!debeEstar) {
            lista.eliminar(e.getDocumento());
        }
    }

    @Override
    public void eliminar(String documento) {
        uow.ejecutar(() -> {
            Empleado e = requerir(documento);
            if (e.getRol() == Rol.ADMINISTRADOR) {
                long admins = empleados.arbol().flujo()
                        .filter(x -> x.getRol() == Rol.ADMINISTRADOR && x.isActivo()).count();
                if (admins <= 1) {
                    throw new ReglaNegocioException("Debe existir al menos un administrador activo");
                }
            }
            turnos.turnos().eliminar(e.getDocumento());
            empleados.eliminar(e.getDocumento());
        });
    }

    @Override
    public Empleado obtener(String documento) {
        return uow.leer(() -> requerir(documento));
    }

    private Empleado requerir(String documento) {
        Empleado e = empleados.buscar(documento);
        if (e == null) {
            throw new RecursoNoEncontradoException("No existe un empleado con documento " + documento);
        }
        return e;
    }

    @Override
    public ListaDoble<Empleado> listar() {
        return uow.leer(() -> empleados.arbol().enInorden());
    }

    @Override
    public TurnoRecepcion turnoActual() {
        return uow.leer(this::construirTurno);
    }

    @Override
    public TurnoRecepcion rotarTurno() {
        return uow.escribir(() -> {
            ListaCircular<String> lista = turnos.turnos();
            if (lista.estaVacia()) {
                throw new EstructuraVaciaException("Rotación de turnos (no hay recepcionistas activos)");
            }
            lista.avanzar();
            return construirTurno();
        });
    }

    private TurnoRecepcion construirTurno() {
        ListaCircular<String> lista = turnos.turnos();
        if (lista.estaVacia()) {
            throw new EstructuraVaciaException("Rotación de turnos (no hay recepcionistas activos)");
        }
        Empleado actual = empleados.buscar(lista.actual());
        ListaDoble<Empleado> siguientes = new ListaDoble<>();
        for (int i = 1; i < lista.tamanio(); i++) {
            Empleado e = empleados.buscar(lista.verSiguiente(i));
            if (e != null) {
                siguientes.agregar(e);
            }
        }
        return new TurnoRecepcion(actual, franja(reloj.ahora().getHour()), siguientes);
    }

    private static String franja(int hora) {
        if (hora >= 6 && hora < 14) {
            return "Mañana (06:00 – 14:00)";
        }
        if (hora >= 14 && hora < 22) {
            return "Tarde (14:00 – 22:00)";
        }
        return "Noche (22:00 – 06:00)";
    }
}
