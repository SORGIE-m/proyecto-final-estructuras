package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Reserva creada por un huésped desde el portal (el titular es el usuario autenticado). */
public record SolicitudReserva(
        @NotBlank(message = "Debe elegir un apartamento") String apartamentoCodigo,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate entrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate salida,
        LocalTime horaLlegada,
        @NotEmpty(message = "Debe registrar al menos un ocupante") @Valid DatosOcupante[] ocupantes,
        String observaciones) {
}
