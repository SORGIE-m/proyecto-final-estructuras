package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.comparadores.ComparadoresApartamento;
import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.Tarifas;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.DatoDuplicadoException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.servicio.ServicioApartamentos;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.comandos.DatosApartamento;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class ServicioApartamentosImpl implements ServicioApartamentos {

    private final RepositorioApartamentos apartamentos;
    private final RepositorioMapa mapa;
    private final ServicioPreparacion preparacion;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioApartamentosImpl(RepositorioApartamentos apartamentos, RepositorioMapa mapa,
                                    ServicioPreparacion preparacion, UnidadDeTrabajo uow, Reloj reloj) {
        this.apartamentos = apartamentos;
        this.mapa = mapa;
        this.preparacion = preparacion;
        this.uow = uow;
        this.reloj = reloj;
    }

    @Override
    public ListaDoble<Apartamento> listar(String orden) {
        return uow.leer(() -> {
            ListaDoble<Apartamento> lista = apartamentos.arbol().enInorden(); // ya ordenada por código
            Comparator<Apartamento> c = comparador(orden);
            if (c != null) {
                lista.ordenar(c);
            }
            return lista;
        });
    }

    static Comparator<Apartamento> comparador(String orden) {
        if (orden == null) {
            return null;
        }
        return switch (orden.toLowerCase()) {
            case "precio" -> ComparadoresApartamento.porPrecio();
            case "capacidad" -> ComparadoresApartamento.porCapacidad();
            case "dormitorios" -> ComparadoresApartamento.porDormitorios();
            case "reservas" -> ComparadoresApartamento.porNumeroReservas();
            default -> null;
        };
    }

    @Override
    public Apartamento obtener(String codigo) {
        return uow.leer(() -> requerir(codigo));
    }

    private Apartamento requerir(String codigo) {
        Apartamento a = apartamentos.buscar(codigo);
        if (a == null) {
            throw new RecursoNoEncontradoException("No existe el apartamento " + codigo);
        }
        return a;
    }

    @Override
    public Apartamento crear(DatosApartamento d) {
        return uow.escribir(() -> {
            if (apartamentos.buscar(d.codigo()) != null) {
                throw new DatoDuplicadoException("Ya existe un apartamento con el código " + d.codigo().trim().toUpperCase());
            }
            Apartamento a = new Apartamento(d.codigo(), d.nombre(), d.bloque(), d.piso(), d.dormitorios(),
                    d.capacidad(), new Tarifas(d.tarifaBaja(), d.tarifaMedia(), d.tarifaAlta()));
            aplicarOpcionales(a, d);
            apartamentos.guardar(a);
            return a;
        });
    }

    @Override
    public Apartamento actualizar(String codigo, DatosApartamento d) {
        return uow.escribir(() -> {
            Apartamento a = requerir(codigo);
            if (d.capacidad() < a.getCapacidad()) {
                for (Reserva r : a.getReservas()) {
                    if (r.esActiva() && r.personas() > d.capacidad()) {
                        throw new ReglaNegocioException("No se puede reducir la capacidad: la reserva "
                                + r.getCodigo() + " tiene " + r.personas() + " ocupantes");
                    }
                }
            }
            a.actualizarDatos(d.nombre(), d.bloque(), d.piso(), d.dormitorios(), d.capacidad(),
                    new Tarifas(d.tarifaBaja(), d.tarifaMedia(), d.tarifaAlta()));
            aplicarOpcionales(a, d);
            return a;
        });
    }

    private void aplicarOpcionales(Apartamento a, DatosApartamento d) {
        ListaDoble<String> caracteristicas = new ListaDoble<>();
        if (d.caracteristicas() != null) {
            for (String c : d.caracteristicas()) {
                if (c != null && !c.isBlank()) {
                    caracteristicas.agregar(c.trim());
                }
            }
        }
        a.setCaracteristicas(caracteristicas);
        a.setDescripcion(d.descripcion() == null ? "" : d.descripcion().trim());
        a.setImagenUrl(d.imagenUrl() == null ? "" : d.imagenUrl().trim());
    }

    @Override
    public void eliminar(String codigo) {
        uow.ejecutar(() -> {
            Apartamento a = requerir(codigo);
            for (Reserva r : a.getReservas()) {
                if (r.esActiva()) {
                    throw new ReglaNegocioException("No se puede eliminar: el apartamento tiene la reserva activa " + r.getCodigo());
                }
            }
            if (a.getEstado() == EstadoApartamento.OCUPADO) {
                throw new ReglaNegocioException("No se puede eliminar un apartamento ocupado");
            }
            apartamentos.eliminar(a.getCodigo());
            mapa.grafo().eliminarVertice(NodoEdificio.referencia(a.getCodigo()));
            preparacion.retirar(a.getCodigo());
        });
    }

    @Override
    public Apartamento cambiarEstado(String codigo, EstadoApartamento destino, String motivo) {
        return uow.escribir(() -> {
            Apartamento a = requerir(codigo);
            if (destino == EstadoApartamento.FUERA_SERVICIO) {
                a.cambiarEstado(destino, motivo, reloj.ahora());
                preparacion.retirar(a.getCodigo());
            } else if (destino == EstadoApartamento.PENDIENTE_PREPARACION) {
                a.cambiarEstado(destino, null, reloj.ahora());
                preparacion.encolar(a.getCodigo());
            } else {
                throw new ReglaNegocioException("Ese cambio se realiza desde check-in, check-out o el panel de preparación");
            }
            return a;
        });
    }
}
