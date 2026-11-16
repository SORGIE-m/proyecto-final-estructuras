package co.edu.uq.sga.servicio.comandos;

import co.edu.uq.sga.dominio.Ocupante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DatosOcupante(
        @NotBlank(message = "El nombre del ocupante es obligatorio") String nombre,
        String documento,
        @NotNull(message = "La fecha de nacimiento del ocupante es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") LocalDate fechaNacimiento) {

    public Ocupante aDominio() {
        return new Ocupante(nombre, documento, fechaNacimiento);
    }
}
