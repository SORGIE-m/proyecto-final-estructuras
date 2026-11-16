package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.comparadores.ComparadoresReserva;
import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.DatoDuplicadoException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.negocio.CodificadorClaves;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.comandos.DatosHuesped;
import co.edu.uq.sga.servicio.comandos.DatosTitular;
import org.springframework.stereotype.Service;

@Service
public class ServicioHuespedesImpl implements ServicioHuespedes {

    private static final int MAX_FOTO = 1_500_000;

    private final RepositorioHuespedes huespedes;
    private final RepositorioApartamentos apartamentos;
    private final RepositorioReservas reservas;
    private final CodificadorClaves codificador;
    private final UnidadDeTrabajo uow;

    public ServicioHuespedesImpl(RepositorioHuespedes huespedes, RepositorioApartamentos apartamentos,
                                 RepositorioReservas reservas, CodificadorClaves codificador, UnidadDeTrabajo uow) {
        this.huespedes = huespedes;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.codificador = codificador;
        this.uow = uow;
    }

    @Override
    public Huesped registrar(DatosHuesped d) {
        return uow.escribir(() -> {
            if (d.clave() == null || d.clave().length() < 8) {
                throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres");
            }
            Huesped existente = huespedes.buscar(d.documento());
            if (existente != null) {
                // un huésped creado por recepción o por el canal externo puede activar su cuenta
                if (existente.getClaveHash() == null && existente.getCorreo().equalsIgnoreCase(d.correo().trim())) {
                    existente.actualizar(d.nombre(), d.fechaNacimiento(), d.telefono(), d.correo());
                    existente.setClaveHash(codificador.codificar(d.clave()));
                    existente.setFoto(validarFoto(d.foto()));
                    return existente;
                }
                throw new DatoDuplicadoException("Ya existe un huésped con el documento " + d.documento().trim());
            }
            validarCorreoUnico(d.correo(), null);
            Huesped h = new Huesped(d.documento(), d.nombre(), d.fechaNacimiento(), d.telefono(), d.correo());
            h.setClaveHash(codificador.codificar(d.clave()));
            h.setFoto(validarFoto(d.foto()));
            huespedes.guardar(h);
            return h;
        });
    }

    @Override
    public Huesped actualizar(String documento, DatosHuesped d) {
        return uow.escribir(() -> {
            Huesped h = requerir(documento);
            validarCorreoUnico(d.correo(), h.getDocumento());
            h.actualizar(d.nombre(), d.fechaNacimiento(), d.telefono(), d.correo());
            if (d.clave() != null && !d.clave().isBlank()) {
                h.setClaveHash(codificador.codificar(d.clave()));
            }
            if (d.foto() != null) {
                h.setFoto(d.foto().isBlank() ? null : validarFoto(d.foto()));
            }
            return h;
        });
    }

    private String validarFoto(String foto) {
        if (foto == null || foto.isBlank()) {
            return null;
        }
        if (foto.length() > MAX_FOTO) {
            throw new ReglaNegocioException("La fotografía es demasiado grande (máximo 1 MB)");
        }
        if (!foto.startsWith("data:image/") && !foto.startsWith("http")) {
            throw new ReglaNegocioException("La fotografía debe ser una imagen válida");
        }
        return foto;
    }

    private void validarCorreoUnico(String correo, String documentoPropio) {
        Huesped otro = huespedes.buscarPorCorreo(correo);
        if (otro != null && !otro.getDocumento().equals(documentoPropio)) {
            throw new DatoDuplicadoException("El correo " + correo + " ya está registrado");
        }
    }

    @Override
    public void eliminar(String documento) {
        uow.ejecutar(() -> {
            Huesped h = requerir(documento);
            for (Reserva r : reservas.todas()) {
                if (r.getTitularDocumento().equals(h.getDocumento()) && r.esActiva()) {
                    throw new ReglaNegocioException("No se puede eliminar: el huésped tiene la reserva activa " + r.getCodigo());
                }
            }
            huespedes.eliminar(h.getDocumento());
        });
    }

    @Override
    public Huesped obtener(String documento) {
        return uow.leer(() -> requerir(documento));
    }

    private Huesped requerir(String documento) {
        Huesped h = huespedes.buscar(documento);
        if (h == null) {
            throw new RecursoNoEncontradoException("No existe un huésped con documento " + documento);
        }
        return h;
    }

    @Override
    public ListaDoble<Huesped> listar(String texto) {
        return uow.leer(() -> {
            ListaDoble<Huesped> todos = huespedes.arbol().enInorden();
            if (texto == null || texto.isBlank()) {
                return todos;
            }
            String t = texto.trim().toLowerCase();
            return todos.filtrar(h -> h.getNombre().toLowerCase().contains(t)
                    || h.getDocumento().contains(t) || h.getCorreo().contains(t));
        });
    }

    @Override
    public Huesped obtenerOCrear(DatosTitular t) {
        return uow.escribir(() -> {
            Huesped h = huespedes.buscar(t.documento());
            if (h != null) {
                return h;
            }
            validarCorreoUnico(t.correo(), null);
            h = new Huesped(t.documento(), t.nombre(), t.fechaNacimiento(), t.telefono(), t.correo());
            huespedes.guardar(h);
            return h;
        });
    }

    @Override
    public boolean agregarFavorito(String documento, String codigo) {
        return uow.escribir(() -> {
            Huesped h = requerir(documento);
            Apartamento a = apartamentos.buscar(codigo);
            if (a == null) {
                throw new RecursoNoEncontradoException("No existe el apartamento " + codigo);
            }
            return h.getFavoritos().agregar(a.getCodigo());
        });
    }

    @Override
    public boolean quitarFavorito(String documento, String codigo) {
        return uow.escribir(() -> requerir(documento).getFavoritos().eliminar(codigo.trim().toUpperCase()));
    }

    @Override
    public ListaDoble<Apartamento> favoritos(String documento) {
        return uow.leer(() -> {
            ListaDoble<Apartamento> r = new ListaDoble<>();
            for (String codigo : requerir(documento).getFavoritos()) {
                Apartamento a = apartamentos.buscar(codigo);
                if (a != null) {
                    r.agregar(a);
                }
            }
            return r;
        });
    }

    @Override
    public ListaDoble<Reserva> historial(String documento) {
        return uow.leer(() -> {
            ListaDoble<Reserva> r = new ListaDoble<>();
            for (String codigo : requerir(documento).getHistorial()) {
                Reserva reserva = reservas.buscar(codigo);
                if (reserva != null) {
                    r.agregar(reserva);
                }
            }
            return r;
        });
    }

    @Override
    public ListaDoble<Reserva> reservas(String documento) {
        return uow.leer(() -> {
            Huesped h = requerir(documento);
            ListaDoble<Reserva> r = reservas.todas().flujo()
                    .filter(x -> x.getTitularDocumento().equals(h.getDocumento()))
                    .collect(ListaDoble::new, ListaDoble::agregar, ListaDoble::agregarTodos);
            r.ordenar(ComparadoresReserva.porCreacionDescendente());
            return r;
        });
    }
}
