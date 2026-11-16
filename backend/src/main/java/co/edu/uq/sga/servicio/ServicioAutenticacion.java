package co.edu.uq.sga.servicio;

import co.edu.uq.sga.servicio.comandos.Credenciales;
import co.edu.uq.sga.servicio.resultados.Sesion;

/** Inicio y cierre de sesión con token. */
public interface ServicioAutenticacion {
    Sesion iniciarSesion(Credenciales credenciales);

    /** @return la sesión vigente del token o lanza AutenticacionException. */
    Sesion validar(String token);

    void cerrarSesion(String token);
}
