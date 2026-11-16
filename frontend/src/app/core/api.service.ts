import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Apartamento, Calendario, Cotizacion, Empleado, Estadisticas, Folio, GrupoBloque, Huesped, ItemPreparacion,
  Mapa, Ocupante, Opcion, Procesamiento, PropuestaGrupo, Reembolso, Reserva, Ruta, Sesion, SolicitudExterna,
  Tablero, Temporada, Turno
} from './modelos';

type Params = Record<string, string | number | boolean | null | undefined>;

/** Único punto de acceso HTTP a la API del SGA. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  private params(p: Params = {}): HttpParams {
    let hp = new HttpParams();
    for (const [k, v] of Object.entries(p)) {
      if (v !== null && v !== undefined && v !== '') hp = hp.set(k, String(v));
    }
    return hp;
  }

  // ---- autenticación
  login(usuario: string, clave: string) { return this.http.post<Sesion>(`${this.base}/auth/login`, { usuario, clave }); }
  registro(datos: unknown) { return this.http.post<Sesion>(`${this.base}/auth/registro`, datos); }
  logout() { return this.http.post(`${this.base}/auth/logout`, {}); }
  perfil() { return this.http.get<Huesped>(`${this.base}/auth/perfil`); }

  // ---- apartamentos
  apartamentos(orden?: string) { return this.http.get<Apartamento[]>(`${this.base}/apartamentos`, { params: this.params({ orden }) }); }
  apartamento(codigo: string) { return this.http.get<Apartamento>(`${this.base}/apartamentos/${codigo}`); }
  crearApartamento(d: unknown) { return this.http.post<Apartamento>(`${this.base}/apartamentos`, d); }
  actualizarApartamento(codigo: string, d: unknown) { return this.http.put<Apartamento>(`${this.base}/apartamentos/${codigo}`, d); }
  eliminarApartamento(codigo: string) { return this.http.delete(`${this.base}/apartamentos/${codigo}`); }
  cambiarEstadoApartamento(codigo: string, estado: string, motivo?: string) {
    return this.http.patch<Apartamento>(`${this.base}/apartamentos/${codigo}/estado`, { estado, motivo });
  }

  // ---- disponibilidad
  disponibilidad(entrada: string, salida: string, personas: number, orden: string) {
    return this.http.get<Opcion[]>(`${this.base}/disponibilidad`, { params: this.params({ entrada, salida, personas, orden }) });
  }
  cotizar(apartamento: string, entrada: string, salida: string, ocupantes: Ocupante[]) {
    return this.http.post<Cotizacion>(`${this.base}/disponibilidad/cotizacion`, { apartamento, entrada, salida, ocupantes });
  }
  cotizarRapido(apartamento: string, entrada: string, salida: string, adultos: number, ninos: number) {
    return this.http.get<Cotizacion>(`${this.base}/disponibilidad/cotizacion`, { params: this.params({ apartamento, entrada, salida, adultos, ninos }) });
  }
  proponerGrupo(entrada: string, salida: string, personas: number) {
    return this.http.get<PropuestaGrupo>(`${this.base}/disponibilidad/grupos`, { params: this.params({ entrada, salida, personas }) });
  }

  // ---- reservas
  reservarPortal(d: unknown) { return this.http.post<Reserva>(`${this.base}/reservas/portal`, d); }
  reservarDirecta(d: unknown) { return this.http.post<Reserva>(`${this.base}/reservas/directa`, d); }
  reservarGrupo(d: unknown) { return this.http.post<Reserva[]>(`${this.base}/reservas/grupo`, d); }
  reservas(filtros: Params = {}) { return this.http.get<Reserva[]>(`${this.base}/reservas`, { params: this.params(filtros) }); }
  reserva(codigo: string) { return this.http.get<Reserva>(`${this.base}/reservas/${codigo}`); }
  llegadas(fecha?: string) { return this.http.get<Reserva[]>(`${this.base}/reservas/llegadas`, { params: this.params({ fecha }) }); }
  salidas(fecha?: string) { return this.http.get<Reserva[]>(`${this.base}/reservas/salidas`, { params: this.params({ fecha }) }); }
  porVencer() { return this.http.get<Reserva[]>(`${this.base}/reservas/por-vencer`); }
  confirmar(codigo: string) { return this.http.post<Reserva>(`${this.base}/reservas/${codigo}/confirmar`, {}); }
  previsualizarCancelacion(codigo: string) { return this.http.get<Reembolso>(`${this.base}/reservas/${codigo}/cancelacion`); }
  cancelar(codigo: string, motivo?: string) { return this.http.post<Reserva>(`${this.base}/reservas/${codigo}/cancelar`, { motivo }); }
  checkIn(codigo: string) { return this.http.post<Reserva>(`${this.base}/reservas/${codigo}/check-in`, {}); }
  checkOut(codigo: string) { return this.http.post<Reserva>(`${this.base}/reservas/${codigo}/check-out`, {}); }
  noShow(codigo: string) { return this.http.post<Reserva>(`${this.base}/reservas/${codigo}/no-show`, {}); }

  // ---- folio
  folio(codigo: string) { return this.http.get<Folio>(`${this.base}/reservas/${codigo}/folio`); }
  cargo(codigo: string, concepto: string, monto: number) { return this.http.post<Folio>(`${this.base}/reservas/${codigo}/folio/cargos`, { concepto, monto }); }
  pago(codigo: string, concepto: string, monto: number) { return this.http.post<Folio>(`${this.base}/reservas/${codigo}/folio/pagos`, { concepto, monto }); }
  anticipo(codigo: string) { return this.http.post<Folio>(`${this.base}/reservas/${codigo}/folio/anticipo`, {}); }
  deshacer(codigo: string) { return this.http.post<Folio>(`${this.base}/reservas/${codigo}/folio/deshacer`, {}); }

  // ---- huéspedes
  huespedes(texto?: string) { return this.http.get<Huesped[]>(`${this.base}/huespedes`, { params: this.params({ texto }) }); }
  huesped(doc: string) { return this.http.get<Huesped>(`${this.base}/huespedes/${doc}`); }
  crearHuesped(d: unknown) { return this.http.post<Huesped>(`${this.base}/huespedes`, d); }
  actualizarHuesped(doc: string, d: unknown) { return this.http.put<Huesped>(`${this.base}/huespedes/${doc}`, d); }
  eliminarHuesped(doc: string) { return this.http.delete(`${this.base}/huespedes/${doc}`); }
  favoritos(doc: string) { return this.http.get<Apartamento[]>(`${this.base}/huespedes/${doc}/favoritos`); }
  agregarFavorito(doc: string, codigo: string) { return this.http.post<Apartamento[]>(`${this.base}/huespedes/${doc}/favoritos/${codigo}`, {}); }
  quitarFavorito(doc: string, codigo: string) { return this.http.delete<Apartamento[]>(`${this.base}/huespedes/${doc}/favoritos/${codigo}`); }
  historial(doc: string) { return this.http.get<Reserva[]>(`${this.base}/huespedes/${doc}/historial`); }
  reservasDe(doc: string) { return this.http.get<Reserva[]>(`${this.base}/huespedes/${doc}/reservas`); }

  // ---- empleados y turnos
  empleados() { return this.http.get<Empleado[]>(`${this.base}/empleados`); }
  crearEmpleado(d: unknown) { return this.http.post<Empleado>(`${this.base}/empleados`, d); }
  actualizarEmpleado(doc: string, d: unknown) { return this.http.put<Empleado>(`${this.base}/empleados/${doc}`, d); }
  eliminarEmpleado(doc: string) { return this.http.delete(`${this.base}/empleados/${doc}`); }
  turno() { return this.http.get<Turno>(`${this.base}/empleados/turno`); }
  rotarTurno() { return this.http.post<Turno>(`${this.base}/empleados/turno/rotar`, {}); }

  // ---- temporadas
  temporadas() { return this.http.get<Temporada[]>(`${this.base}/temporadas`); }
  crearTemporada(d: unknown) { return this.http.post<Temporada>(`${this.base}/temporadas`, d); }
  actualizarTemporada(id: string, d: unknown) { return this.http.put<Temporada>(`${this.base}/temporadas/${id}`, d); }
  eliminarTemporada(id: string) { return this.http.delete(`${this.base}/temporadas/${id}`); }

  // ---- preparación
  colaPreparacion() { return this.http.get<ItemPreparacion[]>(`${this.base}/preparacion/cola`); }
  enPreparacion() { return this.http.get<Apartamento[]>(`${this.base}/preparacion/en-curso`); }
  despachar() { return this.http.post<ItemPreparacion>(`${this.base}/preparacion/despachar`, {}); }
  iniciarPreparacion(codigo: string) { return this.http.post<Apartamento>(`${this.base}/preparacion/${codigo}/iniciar`, {}); }
  finalizarPreparacion(codigo: string) { return this.http.post<Apartamento>(`${this.base}/preparacion/${codigo}/finalizar`, {}); }
  rutaPreparacion(algoritmo: string, origen?: string) {
    return this.http.get<Ruta>(`${this.base}/preparacion/ruta`, { params: this.params({ algoritmo, origen }) });
  }

  // ---- mapa
  mapa() { return this.http.get<Mapa>(`${this.base}/mapa`); }
  ruta(origen: string, destino: string, algoritmo: string) {
    return this.http.get<Ruta>(`${this.base}/mapa/ruta`, { params: this.params({ origen, destino, algoritmo }) });
  }
  recorrido(origen: string, tipo: string) { return this.http.get<string[]>(`${this.base}/mapa/recorrido`, { params: this.params({ origen, tipo }) }); }
  simularCierre(a: string, b: string, origen = 'RECEPCION') {
    return this.http.get<string[]>(`${this.base}/mapa/simular-cierre`, { params: this.params({ a, b, origen }) });
  }
  inalcanzables(origen = 'RECEPCION') { return this.http.get<string[]>(`${this.base}/mapa/inalcanzables`, { params: this.params({ origen }) }); }
  bloques() { return this.http.get<GrupoBloque[]>(`${this.base}/mapa/bloques`); }
  cambiarPasillo(origen: string, destino: string, habilitado: boolean) {
    return this.http.patch<Mapa>(`${this.base}/mapa/pasillos/estado`, { origen, destino, habilitado });
  }
  crearPasillo(origen: string, destino: string, metros: number) { return this.http.post<Mapa>(`${this.base}/mapa/pasillos`, { origen, destino, metros }); }
  eliminarPasillo(origen: string, destino: string) {
    return this.http.delete<Mapa>(`${this.base}/mapa/pasillos`, { params: this.params({ origen, destino }) });
  }

  // ---- canal externo
  externasPendientes() { return this.http.get<SolicitudExterna[]>(`${this.base}/canal-externo/pendientes`); }
  simularExternas(cantidad: number) { return this.http.post<SolicitudExterna[]>(`${this.base}/canal-externo/simular`, {}, { params: this.params({ cantidad }) }); }
  procesarSiguiente() { return this.http.post<Procesamiento>(`${this.base}/canal-externo/procesar-siguiente`, {}); }
  procesarTodas() { return this.http.post<Procesamiento[]>(`${this.base}/canal-externo/procesar-todas`, {}); }
  historialExterno() { return this.http.get<Procesamiento[]>(`${this.base}/canal-externo/historial`); }

  // ---- reportes
  tablero() { return this.http.get<Tablero>(`${this.base}/reportes/tablero`); }
  calendario(mes: string) { return this.http.get<Calendario>(`${this.base}/reportes/calendario`, { params: this.params({ mes }) }); }
  estadisticas(desde?: string, hasta?: string) {
    return this.http.get<Estadisticas>(`${this.base}/reportes/estadisticas`, { params: this.params({ desde, hasta }) });
  }
  hoy(): Observable<string> { return this.http.get<string>(`${this.base}/reportes/hoy`); }

  // ---- administración
  cargarEscenario() { return this.http.post<{ mensaje: string }>(`${this.base}/admin/escenario`, {}); }
  cargarEscenarioArchivo(archivo: File) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<{ mensaje: string }>(`${this.base}/admin/escenario/archivo`, datos);
  }
}
