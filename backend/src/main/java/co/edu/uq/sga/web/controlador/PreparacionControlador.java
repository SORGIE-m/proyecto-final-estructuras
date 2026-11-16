package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.web.dto.Dtos.ApartamentoDto;
import co.edu.uq.sga.web.dto.Dtos.ItemPreparacionDto;
import co.edu.uq.sga.web.dto.Dtos.RutaDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preparacion")
@RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
public class PreparacionControlador {

    private static final String SESION = InterceptorAutenticacion.ATRIBUTO_SESION;

    private final ServicioPreparacion servicio;
    private final Mapeador mapeador;

    public PreparacionControlador(ServicioPreparacion servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping("/cola")
    public ItemPreparacionDto[] cola() {
        return Mapeador.arreglo(servicio.cola(), mapeador::item, ItemPreparacionDto[]::new);
    }

    @GetMapping("/en-curso")
    public ApartamentoDto[] enCurso() {
        return mapeador.apartamentos(servicio.enPreparacion());
    }

    @PostMapping("/despachar")
    public ItemPreparacionDto despachar(@RequestAttribute(SESION) Sesion sesion) {
        return mapeador.item(servicio.despacharSiguiente(sesion.nombre()));
    }

    @PostMapping("/{codigo}/iniciar")
    public ApartamentoDto iniciar(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        return mapeador.apartamento(servicio.iniciar(codigo, sesion.nombre()));
    }

    @PostMapping("/{codigo}/finalizar")
    public ApartamentoDto finalizar(@RequestAttribute(SESION) Sesion sesion, @PathVariable String codigo) {
        return mapeador.apartamento(servicio.finalizar(codigo, sesion.nombre()));
    }

    @GetMapping("/ruta")
    public RutaDto ruta(@RequestParam(defaultValue = "DIJKSTRA") String algoritmo,
                        @RequestParam(required = false) String origen) {
        return mapeador.rutaPreparacion(servicio.ruta(algoritmo, origen));
    }
}
