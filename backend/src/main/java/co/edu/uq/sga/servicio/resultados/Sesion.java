package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.Rol;

import java.time.LocalDateTime;

/** Sesión autenticada. El identificador es el documento del huésped o del empleado. */
public record Sesion(String token, Rol rol, String identificador, String nombre, String usuario, LocalDateTime expira) {
}
