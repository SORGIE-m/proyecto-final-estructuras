package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.comparadores.ComparadoresPreparacion;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColaPrioridadTest {

    @Test
    void extraeSiempreElMenor() {
        ColaPrioridad<Integer> c = new ColaPrioridad<>(Comparator.naturalOrder());
        Random r = new Random(7);
        for (int i = 0; i < 1000; i++) {
            c.insertar(r.nextInt(10_000));
        }
        int anterior = Integer.MIN_VALUE;
        while (!c.estaVacia()) {
            int x = c.extraer();
            assertThat(x).isGreaterThanOrEqualTo(anterior);
            anterior = x;
        }
        assertThatThrownBy(c::extraer).isInstanceOf(EstructuraVaciaException.class);
    }

    @Test
    void iteraEnOrdenDePrioridadSinVaciarLaCola() {
        ColaPrioridad<Integer> c = new ColaPrioridad<>(Comparator.reverseOrder());
        c.insertar(3);
        c.insertar(9);
        c.insertar(1);
        assertThat(c).containsExactly(9, 3, 1);
        assertThat(c.tamanio()).isEqualTo(3);
        assertThat(c.eliminarSi(x -> x == 9)).isEqualTo(1);
        assertThat(c.verPrimero()).isEqualTo(3);
    }

    @Test
    void colaDePreparacionAtiendePrimeroEntradasDeHoyPorHoraDeLlegada() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 16, 8, 0);
        ColaPrioridad<ItemPreparacion> c = new ColaPrioridad<>(ComparadoresPreparacion.prioridad());
        c.insertar(new ItemPreparacion("B302", "Filandia", "B", 3, base.minusHours(5), false, null, null, null));
        c.insertar(new ItemPreparacion("A201", "Mirador", "A", 2, base, true, LocalTime.of(14, 0), "R-1", "Sofía"));
        c.insertar(new ItemPreparacion("C102", "Boquía", "C", 1, base.minusHours(1), false, null, null, null));
        c.insertar(new ItemPreparacion("B101", "Guadua", "B", 1, base, true, LocalTime.of(11, 30), "R-2", "María"));
        assertThat(c.flujo().map(ItemPreparacion::apartamentoCodigo))
                .containsExactly("B101", "A201", "B302", "C102");
    }
}
