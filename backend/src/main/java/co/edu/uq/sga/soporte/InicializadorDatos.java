package co.edu.uq.sga.soporte;

import co.edu.uq.sga.persistencia.AlmacenEstado;
import co.edu.uq.sga.persistencia.EstadoSistema;
import co.edu.uq.sga.persistencia.PersistenciaEstado;
import co.edu.uq.sga.servicio.ServicioEscenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * Al arrancar recupera los datos persistidos; si no existen (primera ejecución), carga el
 * escenario inicial con los primeros usuarios. Se ejecuta antes de que el servidor web acepte
 * peticiones, así nunca se atiende una solicitud con el sistema vacío.
 */
@Component
public class InicializadorDatos implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorDatos.class);

    private final PersistenciaEstado persistencia;
    private final AlmacenEstado almacen;
    private final ServicioEscenario escenario;
    private final boolean cargarSiVacio;

    public InicializadorDatos(PersistenciaEstado persistencia, AlmacenEstado almacen, ServicioEscenario escenario,
                              @Value("${sga.datos.escenario-si-vacio:true}") boolean cargarSiVacio) {
        this.persistencia = persistencia;
        this.almacen = almacen;
        this.escenario = escenario;
        this.cargarSiVacio = cargarSiVacio;
    }

    @Override
    public void afterSingletonsInstantiated() {
        EstadoSistema guardado = persistencia.cargar();
        if (guardado != null) {
            almacen.reemplazar(guardado);
            LOG.info("Datos recuperados del archivo de persistencia");
        } else if (cargarSiVacio) {
            LOG.info(escenario.cargarPredeterminado());
        }
    }
}
