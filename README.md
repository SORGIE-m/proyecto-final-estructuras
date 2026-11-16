# SGA · Sistema de Gestión de Alojamiento — Aparta-Suites UQ

Aplicación completa para el alojamiento **Aparta-Suites UQ (Salento, Quindío)**: reservas por tres canales sin
ventas dobles, folio con deshacer, cola inteligente de preparación, mapa del edificio como grafo con rutas
Dijkstra/BFS, calendario de ocupación, estadísticas y administración. Todas las estructuras de datos son propias,
genéricas e iterables (sin colecciones de `java.util` en el núcleo).

- **Backend:** Java 21 · Spring Boot 3.5 · persistencia en archivo serializado · 71 pruebas automatizadas.
- **Frontend:** Angular 20 (componentes standalone, signals) · diseño cálido arena, animaciones fluidas.

## Ejecutar

Requisitos: JDK 21, Maven 3.9+, Node 20+.

```bash
cd backend && mvn spring-boot:run
```

```bash
cd frontend && npm install && npm start
```

Abrir http://localhost:4200 (el frontend redirige `/api` al backend en el puerto 8080).

La primera vez el backend carga el **escenario inicial** y lo guarda en `backend/datos/sga.dat`; desde ahí todos los
cambios persisten entre reinicios. Para volver a empezar: botón *Administración → Carga de datos* o borrar ese archivo.

### Pruebas

```bash
cd backend && mvn test
```

## Usuarios iniciales

| Rol | Usuario | Contraseña |
|---|---|---|
| Administradora (Ana Lucía Marín) | `admin` | `Admin123*` |
| Recepcionista (Pedro Castaño) | `recepcion` | `Recep123*` |
| Recepcionista (Paula Henao) | `phenao` | `Recep123*` |
| Recepcionista (Diego Arias) | `darias` | `Recep123*` |
| Huésped (Laura Gómez) | `laura.gomez@correo.com` o `1094000001` | `Huesped123*` |
| Huéspedes 1094000002 … 1094000008 | correo o documento | `Huesped123*` |

La pantalla de ingreso tiene accesos directos para los tres roles.

## Qué incluye el escenario

15 apartamentos en 3 bloques · grafo de 27 puntos y 29 pasillos · 7 temporadas por año (baja, media, alta) ·
30 reservas en todos los estados (finalizadas, en curso, llegadas hoy, pendientes, canceladas, no-show) ·
4 apartamentos en la cola de preparación (2 con entrada hoy) · 1 apartamento fuera de servicio ·
5 solicitudes externas en cola (2 generan conflicto de canal y 1 excede la capacidad) · historial de preparación.
Las fechas son relativas a "hoy", por lo que la demostración siempre está vigente.

## Estructura

```
backend/src/main/java/co/edu/uq/sga
├── estructuras/   ListaDoble, ListaCircular, Cola, Pila, ColaPrioridad, ArbolBinarioBusqueda, Diccionario, Conjunto, Grafo
├── dominio/       Apartamento, Huesped, Empleado, Reserva, Estancia (Iterable), Folio, Movimiento, Temporada, ...
├── comparadores/  Órdenes alternativos (precio, capacidad, dormitorios, reservas, prioridad de preparación)
├── excepciones/   ReservaSolapada, CapacidadExcedida, FechasInvalidas, TransicionEstadoInvalida, EstructuraVacia, ...
├── negocio/       Liquidador recursivo, detector de solapamientos, backtracking de grupos, política de cancelación
├── repositorio/   Interfaces de repositorios (DIP)
├── persistencia/  Estado del sistema, archivo serializado atómico, unidad de trabajo con reversión
├── servicio/      Casos de uso (interfaces + impl), comandos validados y resultados
├── escenario/     Carga del escenario desde archivo plano JSON
└── web/           Controladores REST, DTO, seguridad por roles, manejo de errores
frontend/src/app
├── core/          API, sesión, interceptor, guardas, Pila de navegación, formatos
├── compartido/    Íconos, modal, avisos, mapa SVG del edificio, imagen con respaldo
├── layout/        Sitio público y panel de gestión
└── paginas/       Inicio, reservar, ingresar, mi cuenta y panel (tablero, reservas, calendario,
                   preparación, mapa, canal externo, estadísticas, administración)
docs/              Diagrama de clases y diagrama de estructuras propias (Mermaid)
```

## API principal

| Método y ruta | Rol | Descripción |
|---|---|---|
| `POST /api/auth/login` · `/registro` | público | Sesión con token Bearer |
| `GET /api/apartamentos?orden=` | público | Catálogo (código, precio, capacidad, dormitorios, reservas) |
| `GET /api/disponibilidad` · `POST /cotizacion` | público | Disponibilidad ordenada y desglose noche por noche |
| `POST /api/reservas/portal` · `/directa` · `/grupo` | huésped / recepción | Crear reservas |
| `POST /api/reservas/{c}/check-in` · `/check-out` · `/no-show` · `/cancelar` | recepción | Ciclo de vida |
| `POST /api/reservas/{c}/folio/cargos` · `/pagos` · `/anticipo` · `/deshacer` | recepción / huésped | Folio |
| `GET /api/preparacion/cola` · `POST /despachar` · `GET /ruta` | recepción | Cola de prioridad y ruta |
| `GET /api/mapa` · `/ruta` · `/simular-cierre` · `/bloques` | recepción / admin | Grafo |
| `POST /api/canal-externo/simular` · `/procesar-siguiente` · `/procesar-todas` | recepción | Cola FIFO |
| `GET /api/reportes/tablero` · `/calendario` · `/estadisticas` | recepción / admin | Reportes |
| `POST /api/admin/escenario` · `/escenario/archivo` | admin | Carga de datos |

Los errores responden con JSON uniforme `{estado, error, mensaje, detalles}` (por ejemplo `409 RESERVA_SOLAPADA`,
`422 CAPACIDAD_EXCEDIDA`, `400 FECHAS_INVALIDAS`, `409 TRANSICION_INVALIDA`, `409 ESTRUCTURA_VACIA`).

## Historial de commits

Proyecto desarrollado de forma individual por **SORGIE-m**, con apoyo de **Claude** (asistente de IA de Anthropic).

El proyecto ya estaba completo y se publicó originalmente en un **único commit** (`7ecb636`). Para cumplir el
requisito del enunciado de **mínimo 10 commits por integrante con *conventional commits***, Claude ayudó a
reorganizar ese mismo contenido en commits separados por componente (estructuras, dominio, persistencia, negocio,
servicios, API, pruebas, frontend y documentación). El código final es idéntico al del commit original; solo
cambió la forma en que está dividido el historial, y se agregó esta nota.
