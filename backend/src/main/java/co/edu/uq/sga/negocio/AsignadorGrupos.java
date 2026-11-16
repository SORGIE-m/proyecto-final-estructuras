package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.estructuras.ListaDoble;
import org.springframework.stereotype.Component;

/**
 * Busca, con recursividad y backtracking, la combinación de apartamentos disponibles que aloje
 * a todo un grupo con el MENOR número de apartamentos; ante empate, la de menor capacidad
 * sobrante (menos camas vacías).
 */
@Component
public class AsignadorGrupos {

    private static final class Busqueda {
        final Apartamento[] candidatos;
        final int[] capacidadRestante; // suma de capacidades desde i hasta el final
        final boolean[] elegido;
        boolean[] mejor;
        int mejorCantidad = Integer.MAX_VALUE;
        int mejorSobrante = Integer.MAX_VALUE;

        Busqueda(Apartamento[] candidatos) {
            this.candidatos = candidatos;
            this.elegido = new boolean[candidatos.length];
            this.capacidadRestante = new int[candidatos.length + 1];
            for (int i = candidatos.length - 1; i >= 0; i--) {
                capacidadRestante[i] = capacidadRestante[i + 1] + candidatos[i].getCapacidad();
            }
        }
    }

    public ListaDoble<Apartamento> asignar(ListaDoble<Apartamento> disponibles, int personas) {
        ListaDoble<Apartamento> ordenados = ListaDoble.desde(disponibles);
        // mayor capacidad primero: encuentra pronto soluciones con pocos apartamentos (mejor poda)
        ordenados.ordenar((a, b) -> Integer.compare(b.getCapacidad(), a.getCapacidad()));
        Apartamento[] arr = new Apartamento[ordenados.tamanio()];
        int i = 0;
        for (Apartamento a : ordenados) {
            arr[i++] = a;
        }
        Busqueda b = new Busqueda(arr);
        backtrack(b, 0, personas, 0, 0);
        ListaDoble<Apartamento> resultado = new ListaDoble<>();
        if (b.mejor != null) {
            for (int k = 0; k < arr.length; k++) {
                if (b.mejor[k]) {
                    resultado.agregar(arr[k]);
                }
            }
        }
        return resultado;
    }

    private void backtrack(Busqueda b, int indice, int faltan, int cantidad, int capacidadElegida) {
        if (faltan <= 0) {
            int sobrante = -faltan;
            if (cantidad < b.mejorCantidad || (cantidad == b.mejorCantidad && sobrante < b.mejorSobrante)) {
                b.mejorCantidad = cantidad;
                b.mejorSobrante = sobrante;
                b.mejor = b.elegido.clone();
            }
            return;
        }
        if (indice == b.candidatos.length) {
            return;
        }
        // poda 1: aunque tomara todo lo que queda, no alcanza
        if (b.capacidadRestante[indice] < faltan) {
            return;
        }
        // poda 2: añadir uno más ya no mejora la cantidad de la mejor solución
        if (cantidad + 1 > b.mejorCantidad) {
            return;
        }
        // decisión: incluir el candidato
        b.elegido[indice] = true;
        backtrack(b, indice + 1, faltan - b.candidatos[indice].getCapacidad(), cantidad + 1,
                capacidadElegida + b.candidatos[indice].getCapacidad());
        // deshacer la decisión (backtracking) y probar sin él
        b.elegido[indice] = false;
        backtrack(b, indice + 1, faltan, cantidad, capacidadElegida);
    }
}
