package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Reserva registrada por recepción (teléfono o presencial). */
public record SolicitudReservaDirecta(
        @NotNull(message = "Los datos del titular son obligatorios") @Valid DatosTitular titular,
        @NotBlank(message = "Debe elegir un apartamento") String apartamentoCodigo,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate entrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate salida,
        LocalTime horaLlegada,
        @NotEmpty(message = "Debe registrar al menos un ocupante") @Valid DatosOcupante[] ocupantes,
        String observaciones) {
}
