package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Datos del titular; si no está registrado, recepción lo crea como huésped. */
public record DatosTitular(
        @NotBlank(message = "El documento del titular es obligatorio") String documento,
        @NotBlank(message = "El nombre del titular es obligatorio") String nombre,
        @NotNull(message = "La fecha de nacimiento del titular es obligatoria") LocalDate fechaNacimiento,
        @NotBlank(message = "El teléfono del titular es obligatorio") String telefono,
        @NotBlank(message = "El correo del titular es obligatorio") @Email(message = "El correo del titular no es válido") String correo) {
}
