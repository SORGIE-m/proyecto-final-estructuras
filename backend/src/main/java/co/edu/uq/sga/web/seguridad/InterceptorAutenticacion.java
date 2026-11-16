package co.edu.uq.sga.web.seguridad;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.excepciones.AccesoDenegadoException;
import co.edu.uq.sga.servicio.ServicioAutenticacion;
import co.edu.uq.sga.servicio.resultados.Sesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Valida el token "Authorization: Bearer ..." y el rol exigido por @RequiereRol. */
@Component
public class InterceptorAutenticacion implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "sesion";

    private final ServicioAutenticacion autenticacion;

    public InterceptorAutenticacion(ServicioAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        RequiereRol requerido = metodo.getMethodAnnotation(RequiereRol.class);
        if (requerido == null) {
            requerido = metodo.getBeanType().getAnnotation(RequiereRol.class);
        }
        String token = extraerToken(request);
        if (requerido == null) {
            if (token != null) {
                try {
                    request.setAttribute(ATRIBUTO_SESION, autenticacion.validar(token));
                } catch (RuntimeException ignorada) {
                    // endpoint público: una sesión inválida simplemente se ignora
                }
            }
            return true;
        }
        Sesion sesion = autenticacion.validar(token);
        for (Rol r : requerido.value()) {
            if (r == sesion.rol()) {
                request.setAttribute(ATRIBUTO_SESION, sesion);
                return true;
            }
        }
        throw new AccesoDenegadoException("Su rol (" + sesion.rol() + ") no tiene permiso para esta operación");
    }

    private static String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            return cabecera.substring(7).trim();
        }
        return null;
    }
}
