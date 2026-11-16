package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.NotBlank;

/** El usuario puede ser el usuario del empleado, o el correo / documento del huésped. */
public record Credenciales(
        @NotBlank(message = "El usuario es obligatorio") String usuario,
        @NotBlank(message = "La contraseña es obligatoria") String clave) {
}
