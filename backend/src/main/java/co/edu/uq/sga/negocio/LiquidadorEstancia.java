package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.NocheLiquidada;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.TipoTemporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Iterator;

/**
 * Liquida una estancia noche por noche, de forma recursiva, recorriendo la estancia con su
 * Iterator. Cada noche toma la tarifa de su temporada; los menores de 6 años no pagan.
 */
@Component
public class LiquidadorEstancia {

    private final ProveedorTemporadas temporadas;

    public LiquidadorEstancia(ProveedorTemporadas temporadas) {
        this.temporadas = temporadas;
    }

    public Liquidacion liquidar(Apartamento apartamento, Estancia estancia, ListaDoble<Ocupante> ocupantes) {
        int pagan = 0;
        for (Ocupante o : ocupantes) {
            if (o.pagaTarifa(estancia.entrada())) {
                pagan++;
            }
        }
        return liquidar(apartamento, estancia, pagan, ocupantes.tamanio() - pagan);
    }

    public Liquidacion liquidar(Apartamento apartamento, Estancia estancia, int personasQuePagan, int exentas) {
        ListaDoble<NocheLiquidada> desglose = new ListaDoble<>();
        long total = liquidarRecursivo(estancia.iterator(), apartamento, personasQuePagan, exentas, desglose);
        return new Liquidacion(desglose, total, personasQuePagan, exentas);
    }

    /** Caso base: no quedan noches. Caso recursivo: valor de esta noche + valor del resto. */
    private long liquidarRecursivo(Iterator<LocalDate> noches, Apartamento apto, int pagan, int exentas,
                                   ListaDoble<NocheLiquidada> desglose) {
        if (!noches.hasNext()) {
            return 0;
        }
        LocalDate noche = noches.next();
        TipoTemporada temporada = temporadas.tipoPara(noche);
        long tarifa = apto.tarifaPara(temporada);
        long subtotal = tarifa * pagan;
        desglose.agregar(new NocheLiquidada(noche, temporada, tarifa, pagan, exentas, subtotal));
        return subtotal + liquidarRecursivo(noches, apto, pagan, exentas, desglose);
    }
}
