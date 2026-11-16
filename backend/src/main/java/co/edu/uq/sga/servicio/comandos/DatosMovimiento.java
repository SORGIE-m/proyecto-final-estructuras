package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DatosMovimiento(
        @NotBlank(message = "El concepto es obligatorio") @Size(max = 120) String concepto,
        @Positive(message = "El monto debe ser mayor que cero") long monto) {
}
