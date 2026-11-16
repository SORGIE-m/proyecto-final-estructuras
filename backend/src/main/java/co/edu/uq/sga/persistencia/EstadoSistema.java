package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.estructuras.Cola;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.Diccionario;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaCircular;
import co.edu.uq.sga.estructuras.ListaDoble;

import java.io.Serial;
import java.io.Serializable;

/**
 * Raíz de todos los datos del sistema. Se persiste completa en un archivo serializado.
 * Las colas de prioridad son índices derivados (transient) y se reconstruyen al cargar.
 */
public class EstadoSistema implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    final ArbolBinarioBusqueda<Apartamento> apartamentos = new ArbolBinarioBusqueda<>();
    final ArbolBinarioBusqueda<Huesped> huespedes = new ArbolBinarioBusqueda<>();
    final ArbolBinarioBusqueda<Empleado> empleados = new ArbolBinarioBusqueda<>();
    final Diccionario<String, Reserva> reservas = new Diccionario<>();
    final ListaDoble<Temporada> temporadas = new ListaDoble<>();
    final Grafo<NodoEdificio> mapa = new Grafo<>();
    final Cola<SolicitudExterna> colaExterna = new Cola<>();
    final ListaDoble<ProcesamientoExterno> procesamientosExternos = new ListaDoble<>();
    final ListaDoble<RegistroPreparacion> registrosPreparacion = new ListaDoble<>();
    final ListaCircular<String> turnosRecepcion = new ListaCircular<>();
    long secuenciaReservas;
    long secuenciaSolicitudes;
    long secuenciaTemporadas;

    transient ColaPrioridad<ItemPreparacion> colaPreparacion;
    transient ColaPrioridad<Reserva> colaVencimientos;
}
