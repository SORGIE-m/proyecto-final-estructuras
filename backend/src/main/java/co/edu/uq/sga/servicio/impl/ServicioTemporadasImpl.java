package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.dominio.TipoTemporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioTemporadas;
import co.edu.uq.sga.servicio.ServicioTemporadas;
import co.edu.uq.sga.servicio.comandos.DatosTemporada;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;

@Service
public class ServicioTemporadasImpl implements ServicioTemporadas {

    private final RepositorioTemporadas repositorio;
    private final UnidadDeTrabajo uow;

    public ServicioTemporadasImpl(RepositorioTemporadas repositorio, UnidadDeTrabajo uow) {
        this.repositorio = repositorio;
        this.uow = uow;
    }

    /** Sin bloqueo propio: se invoca siempre dentro de otra operación ya delimitada. */
    @Override
    public TipoTemporada tipoPara(LocalDate fecha) {
        for (Temporada t : repositorio.todas()) {
            if (t.incluye(fecha)) {
                return t.tipo();
            }
        }
        return TipoTemporada.BAJA;
    }

    @Override
    public ListaDoble<Temporada> listar() {
        return uow.leer(() -> {
            ListaDoble<Temporada> l = ListaDoble.desde(repositorio.todas());
            l.ordenar(Comparator.comparing(Temporada::inicio));
            return l;
        });
    }

    @Override
    public Temporada crear(DatosTemporada d) {
        return uow.escribir(() -> {
            Temporada nueva = new Temporada(repositorio.siguienteId(), d.nombre().trim(), d.tipo(), d.inicio(), d.fin());
            validarSinSolape(nueva);
            repositorio.todas().agregar(nueva);
            return nueva;
        });
    }

    @Override
    public Temporada actualizar(String id, DatosTemporada d) {
        return uow.escribir(() -> {
            Temporada actual = requerir(id);
            Temporada nueva = new Temporada(actual.id(), d.nombre().trim(), d.tipo(), d.inicio(), d.fin());
            validarSinSolape(nueva);
            ListaDoble<Temporada> lista = repositorio.todas();
            lista.reemplazar(lista.indiceDe(actual), nueva);
            return nueva;
        });
    }

    @Override
    public void eliminar(String id) {
        uow.ejecutar(() -> repositorio.todas().eliminar(requerir(id)));
    }

    private Temporada requerir(String id) {
        Temporada t = repositorio.todas().buscar(x -> x.id().equals(id));
        if (t == null) {
            throw new RecursoNoEncontradoException("No existe la temporada " + id);
        }
        return t;
    }

    private void validarSinSolape(Temporada nueva) {
        for (Temporada t : repositorio.todas()) {
            if (!t.id().equals(nueva.id()) && t.solapaCon(nueva)) {
                throw new FechasInvalidasException("Las fechas se cruzan con la temporada \"" + t.nombre() + "\" ("
                        + t.inicio() + " a " + t.fin() + ")");
            }
        }
    }
}
