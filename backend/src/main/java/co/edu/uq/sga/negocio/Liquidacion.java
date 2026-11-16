package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.NocheLiquidada;
import co.edu.uq.sga.estructuras.ListaDoble;

/** Resultado de liquidar una estancia: desglose noche por noche y total. */
public record Liquidacion(ListaDoble<NocheLiquidada> desglose, long total, int personasQuePagan, int personasExentas) {

    public int noches() {
        return desglose.tamanio();
    }
}
