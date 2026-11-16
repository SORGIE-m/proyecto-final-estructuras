package co.edu.uq.sga.servicio.comandos;

import co.edu.uq.sga.dominio.TipoTemporada;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DatosTemporada(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El tipo es obligatorio") TipoTemporada tipo,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate inicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fin) {
}
