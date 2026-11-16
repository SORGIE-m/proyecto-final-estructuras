package co.edu.uq.sga.web.controlador;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.servicio.ServicioMapa;
import co.edu.uq.sga.servicio.comandos.DatosNodo;
import co.edu.uq.sga.servicio.comandos.DatosPasillo;
import co.edu.uq.sga.web.dto.Dtos.EstadoPasilloDto;
import co.edu.uq.sga.web.dto.Dtos.GrupoBloqueDto;
import co.edu.uq.sga.web.dto.Dtos.MapaDto;
import co.edu.uq.sga.web.dto.Dtos.NodoDto;
import co.edu.uq.sga.web.dto.Dtos.PasilloDto;
import co.edu.uq.sga.web.dto.Dtos.RutaDto;
import co.edu.uq.sga.web.dto.Mapeador;
import co.edu.uq.sga.web.seguridad.RequiereRol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mapa")
@RequiereRol({Rol.RECEPCIONISTA, Rol.ADMINISTRADOR})
public class MapaControlador {

    private final ServicioMapa servicio;
    private final Mapeador mapeador;

    public MapaControlador(ServicioMapa servicio, Mapeador mapeador) {
        this.servicio = servicio;
        this.mapeador = mapeador;
    }

    @GetMapping
    public MapaDto mapa() {
        return new MapaDto(Mapeador.arreglo(servicio.nodos(), mapeador::nodo, NodoDto[]::new),
                Mapeador.arreglo(servicio.pasillos(), mapeador::pasillo, PasilloDto[]::new));
    }

    @PostMapping("/nodos")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol(Rol.ADMINISTRADOR)
    public NodoDto crearNodo(@Valid @RequestBody DatosNodo datos) {
        return mapeador.nodo(servicio.crearNodo(datos));
    }

    @PutMapping("/nodos/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public NodoDto actualizarNodo(@PathVariable String id, @Valid @RequestBody DatosNodo datos) {
        return mapeador.nodo(servicio.actualizarNodo(id, datos));
    }

    @DeleteMapping("/nodos/{id}")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MapaDto eliminarNodo(@PathVariable String id) {
        servicio.eliminarNodo(id);
        return mapa();
    }

    @PostMapping("/pasillos")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MapaDto conectar(@Valid @RequestBody DatosPasillo datos) {
        servicio.conectar(datos);
        return mapa();
    }

    @DeleteMapping("/pasillos")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MapaDto desconectar(@RequestParam String origen, @RequestParam String destino) {
        servicio.desconectar(origen, destino);
        return mapa();
    }

    @PatchMapping("/pasillos/estado")
    @RequiereRol(Rol.ADMINISTRADOR)
    public MapaDto estadoPasillo(@RequestBody EstadoPasilloDto cuerpo) {
        servicio.cambiarEstadoPasillo(cuerpo.origen(), cuerpo.destino(), cuerpo.habilitado());
        return mapa();
    }

    @GetMapping("/ruta")
    public RutaDto ruta(@RequestParam String origen, @RequestParam String destino,
                        @RequestParam(defaultValue = "DIJKSTRA") String algoritmo) {
        String alg = "BFS".equalsIgnoreCase(algoritmo) ? "BFS" : "DIJKSTRA";
        return mapeador.ruta(alg, servicio.ruta(origen, destino, alg));
    }

    @GetMapping("/recorrido")
    public String[] recorrido(@RequestParam(defaultValue = "RECEPCION") String origen,
                              @RequestParam(defaultValue = "BFS") String tipo) {
        return Mapeador.arreglo(servicio.recorrido(origen, tipo), NodoEdificio::id, String[]::new);
    }

    @GetMapping("/simular-cierre")
    public String[] simularCierre(@RequestParam(defaultValue = "RECEPCION") String origen, @RequestParam String a,
                                  @RequestParam String b) {
        return Mapeador.arreglo(servicio.simularCierre(origen, a, b), NodoEdificio::id, String[]::new);
    }

    @GetMapping("/inalcanzables")
    public String[] inalcanzables(@RequestParam(defaultValue = "RECEPCION") String origen) {
        return Mapeador.arreglo(servicio.inalcanzables(origen), NodoEdificio::id, String[]::new);
    }

    @GetMapping("/bloques")
    public GrupoBloqueDto[] bloques() {
        return Mapeador.arreglo(servicio.agruparPorBloque(), mapeador::grupo, GrupoBloqueDto[]::new);
    }

    @GetMapping("/componentes")
    public String[][] componentes() {
        return servicio.componentes().flujo()
                .map(c -> Mapeador.arreglo(c, NodoEdificio::id, String[]::new))
                .toArray(String[][]::new);
    }
}
