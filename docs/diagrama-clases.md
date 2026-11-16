# Diagrama de clases

Arquitectura por capas (dependencias hacia adentro): **web → servicio → negocio / repositorio → dominio → estructuras**.

```mermaid
classDiagram
    direction LR

    %% ---------------- dominio
    class Apartamento {
        -String codigo
        -String nombre, bloque
        -int piso, dormitorios, capacidad
        -ListaDoble~String~ caracteristicas
        -Tarifas tarifas
        -EstadoApartamento estado
        -String motivoFueraServicio
        -ArbolBinarioBusqueda~Reserva~ reservas
        +cambiarEstado(destino, motivo, ahora)
        +compareTo(Apartamento) «por código»
    }
    class Huesped {
        -String documento
        -String nombre, telefono, correo, foto
        -LocalDate fechaNacimiento
        -Conjunto~String~ favoritos
        -ListaDoble~String~ historial
        +compareTo(Huesped) «por documento»
    }
    class Empleado {
        -String documento, nombre, usuario
        -Rol rol
        -boolean activo
    }
    class Reserva {
        -String codigo
        -Estancia estancia
        -ListaDoble~Ocupante~ ocupantes
        -Canal canal
        -EstadoReserva estado
        -ListaDoble~NocheLiquidada~ desglose
        -long total
        -Folio folio
        -LocalDateTime venceEn
        +transitar(EstadoReserva)
        +anticipoRequerido() long
        +compareTo(Reserva) «por fecha de entrada»
    }
    class Estancia {
        <<record, Iterable~LocalDate~>>
        +entrada, salida
        +noches() int
        +solapaCon(Estancia) boolean
    }
    class Ocupante {
        <<record>>
        +nombre, documento, fechaNacimiento
        +pagaTarifa(entrada) «≥ 6 años»
    }
    class Folio {
        -ListaDoble~Movimiento~ movimientos
        -Pila~Movimiento~ operaciones
        +registrar(tipo, concepto, monto, ...)
        +deshacerUltima() Movimiento
        +saldo() long
    }
    class Movimiento {
        <<record>>
        +numero, tipo, concepto, monto, fecha, numeroReferencia
    }
    class Tarifas { <<record>> +baja, media, alta }
    class Temporada { <<record>> +nombre, tipo, inicio, fin }
    class NodoEdificio { +id, nombre, tipo, bloque, piso, x, y }
    class SolicitudExterna { <<record>> }
    class EstadoApartamento { <<enum>> PREPARADO OCUPADO PENDIENTE_PREPARACION EN_PREPARACION FUERA_SERVICIO }
    class EstadoReserva { <<enum>> PENDIENTE CONFIRMADA EN_CURSO FINALIZADA CANCELADA NO_SHOW }
    class Canal { <<enum>> PORTAL DIRECTA EXTERNA }

    Apartamento *-- Tarifas
    Apartamento --> EstadoApartamento
    Apartamento o-- Reserva : árbol por entrada
    Reserva *-- Estancia
    Reserva *-- Folio
    Reserva o-- Ocupante
    Reserva --> EstadoReserva
    Reserva --> Canal
    Folio o-- Movimiento
    Huesped ..> Reserva : historial (códigos)

    %% ---------------- negocio
    class LiquidadorEstancia { +liquidar(apto, estancia, ocupantes) Liquidacion «recursivo» }
    class DetectorSolapamientos { +primerConflicto(arbol, estancia) «recursivo» }
    class AsignadorGrupos { +asignar(libres, personas) «backtracking» }
    class PoliticaCancelacion { <<interface>> +calcular(reserva, fecha) Reembolso }
    class PoliticaCancelacionEstandar
    class ProveedorTemporadas { <<interface>> +tipoPara(fecha) }
    class CodificadorClaves { <<interface>> }
    PoliticaCancelacion <|.. PoliticaCancelacionEstandar
    LiquidadorEstancia --> ProveedorTemporadas

    %% ---------------- persistencia
    class UnidadDeTrabajo { <<interface>> +leer() +escribir() +exclusivo() }
    class PersistenciaEstado { <<interface>> +cargar() +guardar() }
    class PersistenciaArchivoSerializado
    class EstadoSistema
    class RepositorioApartamentos { <<interface>> }
    class RepositorioReservas { <<interface>> }
    PersistenciaEstado <|.. PersistenciaArchivoSerializado
    UnidadDeTrabajo --> PersistenciaEstado
    PersistenciaArchivoSerializado ..> EstadoSistema

    %% ---------------- servicios
    class ServicioReservas { <<interface>> }
    class ServicioReservasImpl
    class ServicioFolio { <<interface>> }
    class ServicioPreparacion { <<interface>> }
    class ServicioCanalExterno { <<interface>> }
    class ServicioDisponibilidad { <<interface>> }
    class ServicioMapa { <<interface>> }
    class ServicioReportes { <<interface>> }
    ServicioReservas <|.. ServicioReservasImpl
    ServicioReservasImpl --> RepositorioReservas
    ServicioReservasImpl --> RepositorioApartamentos
    ServicioReservasImpl --> DetectorSolapamientos
    ServicioReservasImpl --> LiquidadorEstancia
    ServicioReservasImpl --> PoliticaCancelacion
    ServicioReservasImpl --> ServicioPreparacion
    ServicioReservasImpl --> UnidadDeTrabajo
    ServicioCanalExterno ..> ServicioReservas
    ServicioDisponibilidad ..> AsignadorGrupos

    %% ---------------- web
    class ReservaControlador
    class InterceptorAutenticacion
    class ManejadorErrores
    ReservaControlador --> ServicioReservas
```

## Principios SOLID aplicados

| Principio | Cómo se cumple |
|---|---|
| **S** — Responsabilidad única | Cada clase hace una cosa: `LiquidadorEstancia` solo liquida, `DetectorSolapamientos` solo detecta choques, `PoliticaCancelacion` solo calcula reembolsos, los controladores solo traducen HTTP, el `Mapeador` solo convierte a DTO, `ManejadorErrores` solo traduce excepciones. |
| **O** — Abierto/cerrado | Nuevas políticas de cancelación, nuevos órdenes (`Comparator`) o un nuevo medio de persistencia se agregan implementando una interfaz, sin modificar los servicios. |
| **L** — Sustitución de Liskov | Todas las estructuras son intercambiables como `Coleccion<T>`; cualquier `PersistenciaEstado` o `Reloj` (el real o el de pruebas) funciona igual. |
| **I** — Segregación de interfaces | Repositorios pequeños y específicos (`RepositorioApartamentos`, `RepositorioTurnos`, …) y servicios por caso de uso en vez de un servicio gigante. |
| **D** — Inversión de dependencias | Los servicios dependen de abstracciones (`UnidadDeTrabajo`, repositorios, `Reloj`, `ProveedorTemporadas`, `CodificadorClaves`); Spring inyecta las implementaciones por constructor. |
