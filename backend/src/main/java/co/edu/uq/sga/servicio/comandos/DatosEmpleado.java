package co.edu.uq.sga.servicio.comandos;

import co.edu.uq.sga.dominio.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DatosEmpleado(
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        @NotBlank(message = "El usuario es obligatorio") String usuario,
        String correo,
        String telefono,
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String clave,
        Boolean activo) {
}
