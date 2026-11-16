package co.edu.uq.sga.repositorio;

import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.ListaDoble;

import java.util.function.Supplier;

public interface RepositorioPreparacion {
    ColaPrioridad<ItemPreparacion> cola(Supplier<ColaPrioridad<ItemPreparacion>> constructor);

    ListaDoble<RegistroPreparacion> registros();
}
