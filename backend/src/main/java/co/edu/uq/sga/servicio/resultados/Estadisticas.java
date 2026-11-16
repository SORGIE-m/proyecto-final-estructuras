package co.edu.uq.sga.servicio.resultados;

import java.time.LocalDate;

/** Reporte avanzado calculado recorriendo las estructuras propias. */
public record Estadisticas(LocalDate desde, LocalDate hasta, Resumen resumen,
                           OcupacionApartamento[] ocupacionPorApartamento,
                           OcupacionTemporada[] ocupacionPorTemporada,
                           IngresoMes[] ingresosPorMes,
                           ReservasCanal[] reservasPorCanal,
                           Conflictos conflictos,
                           Preparacion preparacion,
                           EstructurasInfo estructuras) {

    public record Resumen(int reservasPeriodo, long ingresosRecaudados, long ingresosFacturados,
                          double ocupacionPromedio, long tarifaPromedioNoche, int huespedes, int apartamentos) {
    }

    public record OcupacionApartamento(String codigo, String nombre, int nochesOcupadas, int nochesPeriodo,
                                       double porcentaje, long ingresos) {
    }

    public record OcupacionTemporada(String temporada, int nochesDisponibles, int nochesOcupadas, double porcentaje) {
    }

    public record IngresoMes(String mes, long recaudado, long facturado) {
    }

    public record ReservasCanal(String canal, int total, int pendientes, int confirmadas, int enCurso,
                                int finalizadas, int canceladas, int noShow) {
    }

    public record ConflictoItem(String solicitud, String plataforma, String apartamento, String reservaEnConflicto,
                                String motivo, String fecha) {
    }

    public record Conflictos(int procesadas, int aceptadas, int conflictos, int rechazadas, ConflictoItem[] recientes) {
    }

    public record Preparacion(int registros, double promedioMinutos, double promedioEsperaMinutos,
                              long minimoMinutos, long maximoMinutos) {
    }

    /** Métricas de las estructuras (útil para la sustentación). */
    public record EstructurasInfo(int apartamentosArbol, int alturaArbolApartamentos, int huespedesArbol,
                                  int alturaArbolHuespedes, int nodosGrafo, int pasillosGrafo,
                                  int solicitudesEnCola, int reservasIndexadas) {
    }
}
