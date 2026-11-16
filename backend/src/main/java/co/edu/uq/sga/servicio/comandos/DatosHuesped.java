package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** La clave es obligatoria al registrarse y opcional al actualizar. */
public record DatosHuesped(
        @NotBlank(message = "El documento es obligatorio") @Size(max = 20) String documento,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotNull(message = "La fecha de nacimiento es obligatoria") @Past(message = "La fecha de nacimiento debe estar en el pasado") LocalDate fechaNacimiento,
        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 20) String telefono,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String clave,
        String foto) {
}
