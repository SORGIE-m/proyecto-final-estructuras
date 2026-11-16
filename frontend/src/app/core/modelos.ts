/** Contratos de la API del SGA (reflejan los DTO del backend). */

export type Rol = 'HUESPED' | 'RECEPCIONISTA' | 'ADMINISTRADOR';
export type EstadoApartamento = 'PREPARADO' | 'OCUPADO' | 'PENDIENTE_PREPARACION' | 'EN_PREPARACION' | 'FUERA_SERVICIO';
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_SHOW';
export type Canal = 'PORTAL' | 'DIRECTA' | 'EXTERNA';
export type TipoNodo = 'APARTAMENTO' | 'RECEPCION' | 'ESCALERA' | 'CUARTO_ASEO' | 'PASILLO';

export interface Sesion {
  token: string;
  rol: Rol;
  identificador: string;
  nombre: string;
  usuario: string;
  expira: string;
}

export interface Apartamento {
  codigo: string;
  nombre: string;
  bloque: string;
  piso: number;
  dormitorios: number;
  capacidad: number;
  caracteristicas: string[];
  tarifaBaja: number;
  tarifaMedia: number;
  tarifaAlta: number;
  descripcion: string | null;
  imagenUrl: string | null;
  estado: EstadoApartamento;
  estadoEtiqueta: string;
  motivoFueraServicio: string | null;
  pendienteDesde: string | null;
  enPreparacionDesde: string | null;
  totalReservas: number;
  reservasActivas: number;
}

export interface Huesped {
  documento: string;
  nombre: string;
  fechaNacimiento: string;
  telefono: string;
  correo: string;
  foto: string | null;
  favoritos: string[];
  estancias: number;
  cuentaActiva: boolean;
}

export interface Empleado {
  documento: string;
  nombre: string;
  rol: Rol;
  usuario: string;
  correo: string;
  telefono: string;
  activo: boolean;
}

export interface Ocupante {
  nombre: string;
  documento?: string;
  fechaNacimiento: string;
  edad?: number;
  paga?: boolean;
}

export interface Noche {
  fecha: string;
  temporada: string;
  tarifaPorPersona: number;
  personasQuePagan: number;
  personasExentas: number;
  subtotal: number;
}

export interface Movimiento {
  numero: number;
  tipo: 'CARGO' | 'PAGO' | 'REVERSO_CARGO' | 'REVERSO_PAGO';
  tipoEtiqueta: string;
  concepto: string;
  monto: number;
  efecto: number;
  fecha: string;
  numeroReferencia: number | null;
  usuario: string;
}

export interface Folio {
  reserva: string;
  movimientos: Movimiento[];
  totalCargos: number;
  totalPagado: number;
  saldo: number;
  ultimaOperacion: Movimiento | null;
  deshacibles: number;
}

export interface Reserva {
  codigo: string;
  apartamentoCodigo: string;
  apartamentoNombre: string;
  imagenUrl: string | null;
  titularDocumento: string;
  titularNombre: string;
  ocupantes: Ocupante[];
  entrada: string;
  salida: string;
  noches: number;
  canal: Canal;
  estado: EstadoReserva;
  estadoEtiqueta: string;
  creadaEn: string;
  venceEn: string | null;
  horaLlegada: string;
  total: number;
  anticipoRequerido: number;
  anticipoPendiente: number;
  pagado: number;
  saldo: number;
  checkInEn: string | null;
  checkOutEn: string | null;
  motivoCierre: string | null;
  referenciaExterna: string | null;
  observaciones: string | null;
  desglose: Noche[];
}

export interface Cotizacion {
  apartamentoCodigo: string;
  entrada: string;
  salida: string;
  noches: number;
  personasQuePagan: number;
  personasExentas: number;
  total: number;
  promedioNoche: number;
  anticipo: number;
  desglose: Noche[];
}

export interface Opcion {
  apartamento: Apartamento;
  total: number;
  noches: number;
  promedioNoche: number;
}

export interface Reembolso {
  pagado: number;
  reembolso: number;
  retenido: number;
  porcentaje: number;
  regla: string;
}

export interface ItemPreparacion {
  apartamentoCodigo: string;
  apartamentoNombre: string;
  bloque: string;
  piso: number;
  prioridad: number;
  entradaHoy: boolean;
  horaLlegada: string | null;
  reservaEntrante: string | null;
  huespedEntrante: string | null;
  pendienteDesde: string | null;
}

export interface NodoMapa {
  id: string;
  nombre: string;
  tipo: TipoNodo;
  bloque: string;
  piso: number;
  x: number;
  y: number;
  estadoApartamento: EstadoApartamento | null;
}

export interface Pasillo {
  origen: string;
  destino: string;
  metros: number;
  habilitado: boolean;
}

export interface Mapa {
  nodos: NodoMapa[];
  pasillos: Pasillo[];
}

export interface Ruta {
  algoritmo: string;
  camino: string[];
  metros: number;
  paradas: string[];
  inalcanzables: string[];
}

export interface GrupoBloque {
  bloque: string;
  nodos: string[];
  apartamentos: string[];
}

export interface SolicitudExterna {
  id: string;
  plataforma: string;
  referencia: string;
  apartamento: string;
  titularNombre: string;
  titularDocumento: string;
  entrada: string;
  salida: string;
  personas: number;
  recibidaEn: string;
}

export interface Procesamiento {
  solicitud: SolicitudExterna;
  resultado: 'ACEPTADA' | 'CONFLICTO' | 'RECHAZADA';
  reservaCodigo: string | null;
  reservaEnConflicto: string | null;
  motivo: string;
  procesadaEn: string;
}

export interface Turno {
  actual: Empleado;
  franja: string;
  siguientes: Empleado[];
}

export interface PropuestaGrupo {
  personas: number;
  apartamentos: Apartamento[];
  capacidadTotal: number;
  totalEstimado: number;
  candidatosEvaluados: number;
}

export interface Temporada {
  id: string;
  nombre: string;
  tipo: 'BAJA' | 'MEDIA' | 'ALTA';
  inicio: string;
  fin: string;
}

export interface Tablero {
  apartamentos: number;
  preparados: number;
  ocupados: number;
  pendientes: number;
  enPreparacion: number;
  fueraServicio: number;
  llegadasHoy: number;
  salidasHoy: number;
  reservasPendientes: number;
  solicitudesExternas: number;
  ocupacionHoy: number;
  ingresosMes: number;
}

export type EstadoNoche = 'LIBRE' | 'OCUPADA' | 'PENDIENTE' | 'BLOQUEADA';

export interface Calendario {
  mes: string;
  noches: string[];
  filas: {
    apartamento: string;
    nombre: string;
    bloque: string;
    estadoApartamento: EstadoApartamento;
    nochesOcupadas: number;
    celdas: { fecha: string; estado: EstadoNoche; reserva: string | null; huesped: string | null }[];
  }[];
}

export interface Estadisticas {
  desde: string;
  hasta: string;
  resumen: {
    reservasPeriodo: number;
    ingresosRecaudados: number;
    ingresosFacturados: number;
    ocupacionPromedio: number;
    tarifaPromedioNoche: number;
    huespedes: number;
    apartamentos: number;
  };
  ocupacionPorApartamento: { codigo: string; nombre: string; nochesOcupadas: number; nochesPeriodo: number; porcentaje: number; ingresos: number }[];
  ocupacionPorTemporada: { temporada: string; nochesDisponibles: number; nochesOcupadas: number; porcentaje: number }[];
  ingresosPorMes: { mes: string; recaudado: number; facturado: number }[];
  reservasPorCanal: { canal: string; total: number; pendientes: number; confirmadas: number; enCurso: number; finalizadas: number; canceladas: number; noShow: number }[];
  conflictos: {
    procesadas: number;
    aceptadas: number;
    conflictos: number;
    rechazadas: number;
    recientes: { solicitud: string; plataforma: string; apartamento: string; reservaEnConflicto: string | null; motivo: string; fecha: string }[];
  };
  preparacion: { registros: number; promedioMinutos: number; promedioEsperaMinutos: number; minimoMinutos: number; maximoMinutos: number };
  estructuras: {
    apartamentosArbol: number;
    alturaArbolApartamentos: number;
    huespedesArbol: number;
    alturaArbolHuespedes: number;
    nodosGrafo: number;
    pasillosGrafo: number;
    solicitudesEnCola: number;
    reservasIndexadas: number;
  };
}

export interface ErrorApi {
  estado: number;
  error: string;
  mensaje: string;
  detalles: string[];
}
