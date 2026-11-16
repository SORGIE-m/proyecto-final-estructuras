package co.edu.uq.sga.servicio.comandos;

import co.edu.uq.sga.dominio.TipoNodo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DatosNodo(
        @NotBlank(message = "El identificador es obligatorio") String id,
        String nombre,
        @NotNull(message = "El tipo de punto es obligatorio") TipoNodo tipo,
        String bloque,
        int piso,
        double x,
        double y) {
}
