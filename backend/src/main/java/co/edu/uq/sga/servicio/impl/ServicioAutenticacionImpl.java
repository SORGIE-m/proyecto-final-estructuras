package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.estructuras.Diccionario;
import co.edu.uq.sga.excepciones.AutenticacionException;
import co.edu.uq.sga.negocio.CodificadorClaves;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioEmpleados;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import co.edu.uq.sga.servicio.ServicioAutenticacion;
import co.edu.uq.sga.servicio.comandos.Credenciales;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ServicioAutenticacionImpl implements ServicioAutenticacion {

    private static final int HORAS_SESION = 12;
    private static final String MENSAJE = "Usuario o contraseña incorrectos";

    private final RepositorioEmpleados empleados;
    private final RepositorioHuespedes huespedes;
    private final CodificadorClaves codificador;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;
    private final SecureRandom aleatorio = new SecureRandom();
    /** Sesiones activas en memoria (token → sesión), en el diccionario propio. */
    private final Diccionario<String, Sesion> sesiones = new Diccionario<>();

    public ServicioAutenticacionImpl(RepositorioEmpleados empleados, RepositorioHuespedes huespedes,
                                     CodificadorClaves codificador, UnidadDeTrabajo uow, Reloj reloj) {
        this.empleados = empleados;
        this.huespedes = huespedes;
        this.codificador = codificador;
        this.uow = uow;
        this.reloj = reloj;
    }

    @Override
    public Sesion iniciarSesion(Credenciales c) {
        Sesion sesion = uow.leer(() -> {
            Empleado e = empleados.buscarPorUsuario(c.usuario());
            if (e != null) {
                if (!e.isActivo() || !codificador.coincide(c.clave(), e.getClaveHash())) {
                    throw new AutenticacionException(MENSAJE);
                }
                return nueva(e.getRol(), e.getDocumento(), e.getNombre(), e.getUsuario());
            }
            Huesped h = huespedes.buscarPorCorreo(c.usuario());
            if (h == null) {
                h = huespedes.buscar(c.usuario());
            }
            if (h == null || h.getClaveHash() == null || !codificador.coincide(c.clave(), h.getClaveHash())) {
                throw new AutenticacionException(MENSAJE);
            }
            return nueva(Rol.HUESPED, h.getDocumento(), h.getNombre(), h.getCorreo());
        });
        synchronized (sesiones) {
            sesiones.poner(sesion.token(), sesion);
        }
        return sesion;
    }

    private Sesion nueva(Rol rol, String id, String nombre, String usuario) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Sesion(token, rol, id, nombre, usuario, reloj.ahora().plusHours(HORAS_SESION));
    }

    @Override
    public Sesion validar(String token) {
        if (token == null || token.isBlank()) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        synchronized (sesiones) {
            Sesion s = sesiones.obtener(token);
            if (s == null) {
                throw new AutenticacionException("La sesión no es válida; inicie sesión de nuevo");
            }
            if (s.expira().isBefore(reloj.ahora())) {
                sesiones.eliminar(token);
                throw new AutenticacionException("La sesión expiró; inicie sesión de nuevo");
            }
            return s;
        }
    }

    @Override
    public void cerrarSesion(String token) {
        if (token != null) {
            synchronized (sesiones) {
                sesiones.eliminar(token);
            }
        }
    }
}
