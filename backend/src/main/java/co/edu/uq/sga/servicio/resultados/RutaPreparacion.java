package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.estructuras.ListaDoble;

/** Recorrido sugerido para el personal de preparación. */
public record RutaPreparacion(String algoritmo, NodoEdificio origen, ListaDoble<NodoEdificio> camino,
                              double metros, ListaDoble<String> paradas, ListaDoble<String> inalcanzables) {
}
