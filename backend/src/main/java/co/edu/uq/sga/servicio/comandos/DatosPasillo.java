package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DatosPasillo(
        @NotBlank(message = "El origen es obligatorio") String origen,
        @NotBlank(message = "El destino es obligatorio") String destino,
        @Positive(message = "La distancia debe ser positiva") double metros) {
}
