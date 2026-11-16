package co.edu.uq.sga.servicio;

import java.io.InputStream;

/** Carga de un escenario de prueba completo (reemplaza todos los datos). */
public interface ServicioEscenario {
    /** Carga el escenario incluido en la aplicación. */
    String cargarPredeterminado();

    /** Carga un escenario desde un archivo plano JSON; devuelve un resumen. */
    String cargar(InputStream json);
}
