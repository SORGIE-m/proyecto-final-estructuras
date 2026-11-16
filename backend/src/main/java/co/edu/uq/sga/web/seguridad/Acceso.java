package co.edu.uq.sga.web.seguridad;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.excepciones.AccesoDenegadoException;
import co.edu.uq.sga.excepciones.AutenticacionException;
import co.edu.uq.sga.servicio.resultados.Sesion;

/** Reglas de propiedad: un huésped solo opera sobre sus propios datos. */
public final class Acceso {

    private Acceso() {
    }

    public static boolean esPersonal(Sesion s) {
        return s != null && (s.rol() == Rol.RECEPCIONISTA || s.rol() == Rol.ADMINISTRADOR);
    }

    public static void exigirPropietarioOPersonal(Sesion s, String documento) {
        if (s == null) {
            throw new AutenticacionException("Debe iniciar sesión");
        }
        if (!esPersonal(s) && !s.identificador().equals(documento)) {
            throw new AccesoDenegadoException("Solo puede consultar o modificar su propia información");
        }
    }
}
