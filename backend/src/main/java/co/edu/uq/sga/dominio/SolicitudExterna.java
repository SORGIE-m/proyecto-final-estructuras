package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.ListaDoble;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Solicitud enviada por la plataforma de terceros (simulada). */
public record SolicitudExterna(String id, String plataforma, String referenciaExterna, String apartamentoCodigo,
                               String titularDocumento, String titularNombre, LocalDate titularNacimiento,
                               String titularTelefono, String titularCorreo, ListaDoble<Ocupante> ocupantes,
                               LocalDate entrada, LocalDate salida, LocalTime horaLlegada,
                               LocalDateTime recibidaEn) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
