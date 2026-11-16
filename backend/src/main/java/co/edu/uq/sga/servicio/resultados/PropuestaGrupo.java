package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.estructuras.ListaDoble;

/** Combinación mínima de apartamentos para alojar a un grupo grande. */
public record PropuestaGrupo(int personas, ListaDoble<Apartamento> apartamentos, int capacidadTotal,
                             long totalEstimado, int candidatosEvaluados) {
}
