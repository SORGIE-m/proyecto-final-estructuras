package co.edu.uq.sga.dominio;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Registro de lo que ocurrió con cada solicitud externa (CONFLICTO = conflicto de canal). */
public record ProcesamientoExterno(SolicitudExterna solicitud, ResultadoSolicitud resultado, String reservaCodigo,
                                   String reservaEnConflicto, String motivo, LocalDateTime procesadaEn)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
