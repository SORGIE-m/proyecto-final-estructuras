package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Grupo grande que se reparte entre varios apartamentos. */
public record SolicitudGrupo(
        @NotNull(message = "Los datos del titular son obligatorios") @Valid DatosTitular titular,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate entrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate salida,
        LocalTime horaLlegada,
        @NotEmpty(message = "Debe registrar los ocupantes del grupo") @Valid DatosOcupante[] ocupantes) {
}
