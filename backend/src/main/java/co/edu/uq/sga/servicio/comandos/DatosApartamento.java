package co.edu.uq.sga.servicio.comandos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DatosApartamento(
        @NotBlank(message = "El código es obligatorio") @Size(max = 12) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @NotBlank(message = "El bloque es obligatorio") String bloque,
        @Min(value = 1, message = "El piso debe ser 1 o mayor") int piso,
        @Min(value = 1, message = "Debe tener al menos un dormitorio") int dormitorios,
        @Min(value = 1, message = "La capacidad debe ser al menos 1") int capacidad,
        String[] caracteristicas,
        @Positive(message = "La tarifa baja debe ser positiva") long tarifaBaja,
        @Positive(message = "La tarifa media debe ser positiva") long tarifaMedia,
        @Positive(message = "La tarifa alta debe ser positiva") long tarifaAlta,
        @Size(max = 600) String descripcion,
        String imagenUrl) {
}
