package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.estructuras.Cola;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaCircular;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.repositorio.RepositorioCanalExterno;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.repositorio.RepositorioPreparacion;
import co.edu.uq.sga.repositorio.RepositorioTemporadas;
import co.edu.uq.sga.repositorio.RepositorioTurnos;
import org.springframework.stereotype.Repository;

import java.util.function.Supplier;

/** Implementaciones de los repositorios operativos sobre el estado serializado. */
public final class RepositoriosOperacionArchivo {

    private RepositoriosOperacionArchivo() {
    }

    @Repository
    public static class Temporadas implements RepositorioTemporadas {
        private final AlmacenEstado almacen;

        public Temporadas(AlmacenEstado almacen) {
            this.almacen = almacen;
        }

        @Override
        public ListaDoble<Temporada> todas() {
            return almacen.actual().temporadas;
        }

        @Override
        public String siguienteId() {
            EstadoSistema e = almacen.actual();
            e.secuenciaTemporadas++;
            return "T-" + e.secuenciaTemporadas;
        }
    }

    @Repository
    public static class Mapa implements RepositorioMapa {
        private final AlmacenEstado almacen;

        public Mapa(AlmacenEstado almacen) {
            this.almacen = almacen;
        }

        @Override
        public Grafo<NodoEdificio> grafo() {
            return almacen.actual().mapa;
        }
    }

    @Repository
    public static class CanalExterno implements RepositorioCanalExterno {
        private final AlmacenEstado almacen;

        public CanalExterno(AlmacenEstado almacen) {
            this.almacen = almacen;
        }

        @Override
        public Cola<SolicitudExterna> cola() {
            return almacen.actual().colaExterna;
        }

        @Override
        public ListaDoble<ProcesamientoExterno> procesamientos() {
            return almacen.actual().procesamientosExternos;
        }

        @Override
        public String siguienteId() {
            EstadoSistema e = almacen.actual();
            e.secuenciaSolicitudes++;
            return String.format("EXT-%04d", e.secuenciaSolicitudes);
        }
    }

    @Repository
    public static class Preparacion implements RepositorioPreparacion {
        private final AlmacenEstado almacen;

        public Preparacion(AlmacenEstado almacen) {
            this.almacen = almacen;
        }

        @Override
        public ColaPrioridad<ItemPreparacion> cola(Supplier<ColaPrioridad<ItemPreparacion>> constructor) {
            EstadoSistema e = almacen.actual();
            if (e.colaPreparacion == null) {
                e.colaPreparacion = constructor.get();
            }
            return e.colaPreparacion;
        }

        @Override
        public ListaDoble<RegistroPreparacion> registros() {
            return almacen.actual().registrosPreparacion;
        }
    }

    @Repository
    public static class Turnos implements RepositorioTurnos {
        private final AlmacenEstado almacen;

        public Turnos(AlmacenEstado almacen) {
            this.almacen = almacen;
        }

        @Override
        public ListaCircular<String> turnos() {
            return almacen.actual().turnosRecepcion;
        }
    }
}
