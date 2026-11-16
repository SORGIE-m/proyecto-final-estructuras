package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.Cola;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.servicio.ServicioDisponibilidad;
import co.edu.uq.sga.servicio.ServicioGrupos;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.servicio.comandos.SolicitudGrupo;
import co.edu.uq.sga.servicio.resultados.PropuestaGrupo;
import org.springframework.stereotype.Service;

@Service
public class ServicioGruposImpl implements ServicioGrupos {

    private final ServicioDisponibilidad disponibilidad;
    private final ServicioReservas reservas;
    private final ServicioHuespedes huespedes;
    private final UnidadDeTrabajo uow;

    public ServicioGruposImpl(ServicioDisponibilidad disponibilidad, ServicioReservas reservas,
                              ServicioHuespedes huespedes, UnidadDeTrabajo uow) {
        this.disponibilidad = disponibilidad;
        this.reservas = reservas;
        this.huespedes = huespedes;
        this.uow = uow;
    }

    /**
     * Reparte el grupo: primero un adulto por apartamento (los menores de 6 no pueden quedar
     * solos), luego el resto en orden. Todo o nada: si una reserva falla, se revierte el grupo.
     */
    @Override
    public ListaDoble<Reserva> reservarGrupo(SolicitudGrupo s) {
        return uow.escribir(() -> {
            ListaDoble<Ocupante> todos = new ListaDoble<>();
            for (DatosOcupante d : s.ocupantes()) {
                todos.agregar(d.aDominio());
            }
            PropuestaGrupo propuesta = disponibilidad.proponerGrupo(s.entrada(), s.salida(), todos.tamanio());
            Cola<Ocupante> adultos = new Cola<>();
            Cola<Ocupante> resto = new Cola<>();
            for (Ocupante o : todos) {
                if (o.pagaTarifa(s.entrada()) && adultos.tamanio() < propuesta.apartamentos().tamanio()) {
                    adultos.encolar(o);
                } else {
                    resto.encolar(o);
                }
            }
            if (adultos.tamanio() < propuesta.apartamentos().tamanio()) {
                throw new ReglaNegocioException("Se necesita al menos un ocupante de 6 años o más por apartamento ("
                        + propuesta.apartamentos().tamanio() + ")");
            }
            Huesped titular = huespedes.obtenerOCrear(s.titular());
            ListaDoble<Reserva> creadas = new ListaDoble<>();
            for (Apartamento a : propuesta.apartamentos()) {
                ListaDoble<Ocupante> grupo = new ListaDoble<>();
                grupo.agregar(adultos.desencolar());
                while (grupo.tamanio() < a.getCapacidad() && !resto.estaVacia()) {
                    grupo.agregar(resto.desencolar());
                }
                creadas.agregar(reservas.registrar(titular, a.getCodigo(), s.entrada(), s.salida(), s.horaLlegada(),
                        grupo, Canal.DIRECTA, "Grupo de " + todos.tamanio() + " personas", null));
            }
            return creadas;
        });
    }
}
