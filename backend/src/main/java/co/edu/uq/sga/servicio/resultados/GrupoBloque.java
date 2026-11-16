package co.edu.uq.sga.servicio.resultados;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.estructuras.ListaDoble;

/** Componente conexa de un bloque obtenida con DFS. */
public record GrupoBloque(String bloque, ListaDoble<NodoEdificio> nodos) {
}
