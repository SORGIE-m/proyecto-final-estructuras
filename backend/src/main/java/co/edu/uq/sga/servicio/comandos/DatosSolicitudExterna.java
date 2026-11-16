package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Solicitud que envía la plataforma de terceros. */
public record DatosSolicitudExterna(
        @NotBlank(message = "La plataforma es obligatoria") String plataforma,
        String referencia,
        @NotBlank(message = "El apartamento es obligatorio") String apartamentoCodigo,
        @NotNull @Valid DatosTitular titular,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate entrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate salida,
        LocalTime horaLlegada,
        @NotEmpty(message = "Debe incluir ocupantes") @Valid DatosOcupante[] ocupantes) {
}
