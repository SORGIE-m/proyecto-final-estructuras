package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.excepciones.AutenticacionException;
import co.edu.uq.sga.servicio.ServicioAutenticacion;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.comandos.Credenciales;
import co.edu.uq.sga.servicio.comandos.DatosHuesped;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.web.dto.Dtos.HuespedDto;
import co.edu.uq.sga.web.dto.Dtos.MensajeDto;
import co.edu.uq.sga.web.dto.Dtos.SesionDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionControlador {

    private final ServicioAutenticacion autenticacion;
    private final ServicioHuespedes huespedes;
    private final Mapeador mapeador;

    public AutenticacionControlador(ServicioAutenticacion autenticacion, ServicioHuespedes huespedes, Mapeador mapeador) {
        this.autenticacion = autenticacion;
        this.huespedes = huespedes;
        this.mapeador = mapeador;
    }

    @PostMapping("/login")
    public SesionDto login(@Valid @RequestBody Credenciales credenciales) {
        return mapeador.sesion(autenticacion.iniciarSesion(credenciales));
    }

    /** Registro público de huéspedes; devuelve la sesión iniciada. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionDto registro(@Valid @RequestBody DatosHuesped datos) {
        if (datos.clave() == null) {
            throw new AutenticacionException("Debe definir una contraseña");
        }
        huespedes.registrar(datos);
        return mapeador.sesion(autenticacion.iniciarSesion(new Credenciales(datos.documento(), datos.clave())));
    }

    @GetMapping("/sesion")
    @RequiereRol({Rol.HUESPED, Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
    public SesionDto sesion(@RequestAttribute(InterceptorAutenticacion.ATRIBUTO_SESION) Sesion sesion) {
        return mapeador.sesion(sesion);
    }

    @GetMapping("/perfil")
    @RequiereRol(Rol.HUESPED)
    public HuespedDto perfil(@RequestAttribute(InterceptorAutenticacion.ATRIBUTO_SESION) Sesion sesion) {
        return mapeador.huesped(huespedes.obtener(sesion.identificador()));
    }

    @PostMapping("/logout")
    public MensajeDto logout(@RequestHeader(value = "Authorization", required = false) String cabecera) {
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            autenticacion.cerrarSesion(cabecera.substring(7).trim());
        }
        return new MensajeDto("Sesión cerrada");
    }
}
