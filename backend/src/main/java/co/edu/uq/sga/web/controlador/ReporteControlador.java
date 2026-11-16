package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioReportes;
import co.edu.uq.sga.servicio.resultados.Calendario;
import co.edu.uq.sga.servicio.resultados.Estadisticas;
import co.edu.uq.sga.servicio.resultados.Tablero;
import co.edu.uq.sga.soporte.Reloj;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/reportes")
@RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
public class ReporteControlador {

    private final ServicioReportes servicio;
    private final Reloj reloj;

    public ReporteControlador(ServicioReportes servicio, Reloj reloj) {
        this.servicio = servicio;
        this.reloj = reloj;
    }

    @GetMapping("/tablero")
    public Tablero tablero() {
        return servicio.tablero();
    }

    @GetMapping("/calendario")
    public Calendario calendario(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return servicio.calendario(mes == null ? YearMonth.from(reloj.hoy()) : mes);
    }

    @GetMapping("/estadisticas")
    @RequiereRol(Rol.ADMINISTRADOR)
    public Estadisticas estadisticas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.estadisticas(desde, hasta);
    }

    @GetMapping("/hoy")
    public LocalDate hoy() {
        return reloj.hoy();
    }
}
