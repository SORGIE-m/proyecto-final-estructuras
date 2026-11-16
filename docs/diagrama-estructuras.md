# Diagrama de estructuras propias

Todas las estructuras son genéricas, implementan `Iterable<T>` con su propio `Iterator<T>` (a través de
`Coleccion<T>`) y **no usan colecciones de `java.util`**. Paquete: `co.edu.uq.sga.estructuras`.

```mermaid
classDiagram
    direction TB
    class Iterable~T~ {
        <<interface>>
        +iterator() Iterator~T~
    }
    class Coleccion~T~ {
        <<interface>>
        +tamanio() int
        +estaVacia() boolean
        +flujo() Stream~T~
    }
    Iterable <|-- Coleccion

    class ListaDoble~T~ {
        -Nodo cabeza, cola
        +agregar(T) / agregarAlInicio(T)
        +obtener(i) / eliminarEn(i) / eliminar(T)
        +buscar(Predicate) / filtrar(Predicate)
        +ordenar(Comparator)  «Merge Sort recursivo»
        +enReversa() Iterable~T~
    }
    class ListaCircular~T~ {
        -Nodo ultimo, actual
        +agregar(T) / eliminar(T)
        +actual() T
        +avanzar() T
        +verSiguiente(n) T
    }
    class Cola~T~ {
        -Nodo frente, fin
        +encolar(T)
        +desencolar() T  «EstructuraVaciaException»
        +frente() T
    }
    class Pila~T~ {
        -Nodo cima
        +apilar(T)
        +desapilar() T
        +cima() T
    }
    class ColaPrioridad~T~ {
        -Object[] monticulo
        -Comparator comparador
        +insertar(T)  O(log n)
        +extraer() T  O(log n)
        +eliminarSi(Predicate)
        +reordenar()  heapify O(n)
    }
    class ArbolBinarioBusqueda~T extends Comparable~ {
        -Nodo raiz
        +insertar(T)  «recursivo»
        +buscar(T) / buscarPor(clave)  «recursivo»
        +eliminar(T)  «recursivo, sucesor inorden»
        +altura()  «recursivo»
        +inorden / preorden / postorden  «recursivos»
        +minimo() / maximo()
    }
    class Diccionario~K extends Comparable, V~ {
        -ArbolBinarioBusqueda~Entrada~ arbol
        +poner(K, V) / obtener(K) / eliminar(K)
    }
    class Conjunto~T~ {
        -ListaDoble~T~ elementos
        +agregar(T) boolean  «sin repetidos»
        +union / interseccion / diferencia
    }
    class Grafo~T~ {
        -ListaDoble~Vertice~ vertices
        +agregarVertice / eliminarVertice
        +conectar(a, b, peso) / desconectar
        +cambiarEstadoConexion(a, b, habilitada)
        +bfs(origen) / dfs(origen)  «DFS recursivo»
        +dijkstra(a, b) Ruta  «usa ColaPrioridad»
        +rutaBfs(a, b) Ruta  «usa Cola»
        +componentesConexas(filtro)
        +inalcanzablesDesde(origen, filtro)
    }
    class Ruta~T~ {
        +camino() ListaDoble~T~
        +costo() double
        +concatenar(Ruta)
    }

    Coleccion <|.. ListaDoble
    Coleccion <|.. ListaCircular
    Coleccion <|.. Cola
    Coleccion <|.. Pila
    Coleccion <|.. ColaPrioridad
    Coleccion <|.. ArbolBinarioBusqueda
    Coleccion <|.. Diccionario
    Coleccion <|.. Conjunto
    Coleccion <|.. Grafo
    Diccionario *-- ArbolBinarioBusqueda
    Conjunto *-- ListaDoble
    Grafo *-- ListaDoble
    Grafo ..> ColaPrioridad : Dijkstra
    Grafo ..> Cola : BFS
    Grafo ..> Ruta
    ArbolBinarioBusqueda ..> Pila : iterador inorden
```

## Uso de cada estructura en el SGA

| Estructura | Dónde se usa |
|---|---|
| `Grafo<NodoEdificio>` * | Mapa del edificio (apartamentos, recepción, escaleras, cuartos de aseo, puente). Dijkstra/BFS para la ruta de preparación, DFS para agrupar por bloque, BFS para inalcanzables al cerrar un pasillo. |
| `ColaPrioridad<ItemPreparacion>` * | Cola de preparación: prioridad 1 = entrada hoy (por hora de llegada), prioridad 2 = resto (`ComparadoresPreparacion` con `thenComparing`). |
| `ColaPrioridad<Reserva>` * | Reservas Pendientes ordenadas por hora de vencimiento (24 h). |
| `Cola<SolicitudExterna>` * | Solicitudes del canal externo en orden de llegada (FIFO). |
| `Pila<Movimiento>` * | Deshacer la última operación del folio (registra el movimiento contrario). En Angular, `Pila<string>` para el botón "Atrás". |
| `ListaDoble<T>` * | Historial de estancias del huésped, ocupantes de una reserva, desglose de noches liquidadas, movimientos del folio. |
| `ListaCircular<String>` | Rotación de turnos de los recepcionistas. |
| `ArbolBinarioBusqueda<T>` * | Catálogo de apartamentos por código, huéspedes y empleados por documento, **reservas de cada apartamento por fecha de entrada**. |
| `Diccionario<K,V>` | Índice global de reservas por código y sesiones activas (sobre el ABB). |
| `Conjunto<String>` | Apartamentos favoritos de cada huésped (sin repetidos). |

## Algoritmos recursivos

- **ABB**: insertar, buscar, eliminar, altura, inorden/preorden/postorden.
- **Detección de solapamientos** (`DetectorSolapamientos`): recorre recursivamente el árbol de reservas de **un**
  apartamento; si una reserva entra en o después de la salida pedida, poda su subárbol derecho.
- **DFS** sobre el grafo (recorrido y componentes conexas).
- **Liquidación noche por noche** (`LiquidadorEstancia`): valor de esta noche + valor del resto, recorriendo la
  `Estancia` con su `Iterator`.
- **Merge Sort** de `ListaDoble`.
- **Asignación de grupos grandes con backtracking** (`AsignadorGrupos`): incluye/excluye cada apartamento libre,
  poda cuando la capacidad restante no alcanza o cuando ya no puede mejorar el mínimo de apartamentos.
