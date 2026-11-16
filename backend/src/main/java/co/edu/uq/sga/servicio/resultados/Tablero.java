package co.edu.uq.sga.servicio.resultados;

/** Resumen de la pantalla de inicio. */
public record Tablero(int apartamentos, int preparados, int ocupados, int pendientes, int enPreparacion,
                      int fueraServicio, int llegadasHoy, int salidasHoy, int reservasPendientes,
                      int solicitudesExternas, double ocupacionHoy, long ingresosMes) {
}
