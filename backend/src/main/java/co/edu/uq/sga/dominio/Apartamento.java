package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Apartamento completo que se entrega en exclusiva a un solo grupo.
 * Orden natural por código (permite guardarlo en el árbol binario de búsqueda).
 * Sus reservas se guardan en un árbol ordenado por fecha de entrada.
 */
public class Apartamento implements Comparable<Apartamento>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String codigo;
    private String nombre;
    private String bloque;
    private int piso;
    private int dormitorios;
    private int capacidad;
    private ListaDoble<String> caracteristicas = new ListaDoble<>();
    private Tarifas tarifas;
    private String descripcion;
    private String imagenUrl;
    private EstadoApartamento estado = EstadoApartamento.PREPARADO;
    private String motivoFueraServicio;
    private LocalDateTime pendienteDesde;
    private LocalDateTime enPreparacionDesde;
    private final ArbolBinarioBusqueda<Reserva> reservas = new ArbolBinarioBusqueda<>();

    public Apartamento(String codigo, String nombre, String bloque, int piso, int dormitorios,
                       int capacidad, Tarifas tarifas) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("El código del apartamento es obligatorio");
        }
        this.codigo = codigo.trim().toUpperCase();
        actualizarDatos(nombre, bloque, piso, dormitorios, capacidad, tarifas);
    }

    public final void actualizarDatos(String nombre, String bloque, int piso, int dormitorios,
                                      int capacidad, Tarifas tarifas) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del apartamento es obligatorio");
        }
        if (bloque == null || bloque.isBlank()) {
            throw new ReglaNegocioException("El bloque es obligatorio");
        }
        if (piso < 1) {
            throw new ReglaNegocioException("El piso debe ser 1 o mayor");
        }
        if (dormitorios < 1) {
            throw new ReglaNegocioException("Debe tener al menos un dormitorio");
        }
        if (capacidad < 1) {
            throw new ReglaNegocioException("La capacidad debe ser al menos 1");
        }
        if (tarifas == null) {
            throw new ReglaNegocioException("Las tarifas son obligatorias");
        }
        this.nombre = nombre.trim();
        this.bloque = bloque.trim().toUpperCase();
        this.piso = piso;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.tarifas = tarifas;
    }

    /** Aplica la máquina de estados; el motivo es obligatorio al pasar a Fuera de servicio. */
    public void cambiarEstado(EstadoApartamento destino, String motivo, LocalDateTime ahora) {
        if (destino == estado) {
            return;
        }
        if (!estado.puedePasarA(destino)) {
            throw new TransicionEstadoInvalidaException("El apartamento " + codigo + " no puede pasar de "
                    + estado.etiqueta() + " a " + destino.etiqueta());
        }
        if (destino == EstadoApartamento.FUERA_SERVICIO) {
            if (motivo == null || motivo.isBlank()) {
                throw new ReglaNegocioException("Debe registrar el motivo para dejar el apartamento fuera de servicio");
            }
            motivoFueraServicio = motivo.trim();
        } else {
            motivoFueraServicio = null;
        }
        if (destino == EstadoApartamento.PENDIENTE_PREPARACION) {
            pendienteDesde = ahora;
            enPreparacionDesde = null;
        } else if (destino == EstadoApartamento.EN_PREPARACION) {
            enPreparacionDesde = ahora;
        } else {
            pendienteDesde = null;
            enPreparacionDesde = null;
        }
        estado = destino;
    }

    /** Restaura el estado sin validar transiciones; uso exclusivo de la carga de escenarios. */
    public void restaurarEstado(EstadoApartamento estado, String motivo, LocalDateTime pendienteDesde) {
        this.estado = estado;
        this.motivoFueraServicio = motivo;
        this.pendienteDesde = pendienteDesde;
    }

    public boolean admiteReservas() {
        return estado != EstadoApartamento.FUERA_SERVICIO;
    }

    public long tarifaPara(TipoTemporada temporada) {
        return tarifas.para(temporada);
    }

    @Override
    public int compareTo(Apartamento o) {
        return codigo.compareTo(o.codigo);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Apartamento a && codigo.equals(a.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getBloque() { return bloque; }
    public int getPiso() { return piso; }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public ListaDoble<String> getCaracteristicas() { return caracteristicas; }
    public void setCaracteristicas(ListaDoble<String> caracteristicas) {
        this.caracteristicas = caracteristicas == null ? new ListaDoble<>() : caracteristicas;
    }
    public Tarifas getTarifas() { return tarifas; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public EstadoApartamento getEstado() { return estado; }
    public String getMotivoFueraServicio() { return motivoFueraServicio; }
    public LocalDateTime getPendienteDesde() { return pendienteDesde; }
    public LocalDateTime getEnPreparacionDesde() { return enPreparacionDesde; }
    public ArbolBinarioBusqueda<Reserva> getReservas() { return reservas; }
}
