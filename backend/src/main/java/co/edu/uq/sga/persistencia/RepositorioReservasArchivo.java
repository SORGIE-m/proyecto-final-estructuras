package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.Diccionario;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import org.springframework.stereotype.Repository;

import java.util.function.Supplier;

@Repository
public class RepositorioReservasArchivo implements RepositorioReservas {

    private final AlmacenEstado almacen;

    public RepositorioReservasArchivo(AlmacenEstado almacen) {
        this.almacen = almacen;
    }

    @Override
    public Reserva buscar(String codigo) {
        return codigo == null ? null : todas().obtener(codigo.trim().toUpperCase());
    }

    @Override
    public void guardar(Reserva reserva) {
        todas().poner(reserva.getCodigo(), reserva);
    }

    @Override
    public Diccionario<String, Reserva> todas() {
        return almacen.actual().reservas;
    }

    @Override
    public String siguienteCodigo() {
        EstadoSistema e = almacen.actual();
        e.secuenciaReservas++;
        return String.format("R-%05d", e.secuenciaReservas);
    }

    @Override
    public ColaPrioridad<Reserva> colaVencimientos(Supplier<ColaPrioridad<Reserva>> constructor) {
        EstadoSistema e = almacen.actual();
        if (e.colaVencimientos == null) {
            e.colaVencimientos = constructor.get();
        }
        return e.colaVencimientos;
    }
}
