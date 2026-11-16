import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { FechaPipe, MonedaPipe } from '../../../core/formatos';
import { Apartamento, Empleado, Huesped, Temporada } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { SesionService } from '../../../core/sesion.service';
import { ConfirmacionService } from '../../../compartido/confirmacion.service';
import { Icono } from '../../../compartido/icono';
import { ImagenApartamento } from '../../../compartido/imagen-apartamento';
import { Modal } from '../../../compartido/modal';

type Seccion = 'apartamentos' | 'huespedes' | 'empleados' | 'temporadas' | 'datos';

interface FormApto {
  codigo: string; nombre: string; bloque: string; piso: number; dormitorios: number; capacidad: number;
  caracteristicas: string; tarifaBaja: number; tarifaMedia: number; tarifaAlta: number; descripcion: string; imagenUrl: string;
}

@Component({
  selector: 'app-administracion',
  imports: [FormsModule, Icono, Modal, ImagenApartamento, MonedaPipe, FechaPipe],
  templateUrl: './administracion.html',
  styleUrl: './administracion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Administracion implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  private readonly confirmacion = inject(ConfirmacionService);
  readonly sesion = inject(SesionService);

  readonly seccion = signal<Seccion>('apartamentos');
  readonly apartamentos = signal<Apartamento[]>([]);
  readonly huespedes = signal<Huesped[]>([]);
  readonly empleados = signal<Empleado[]>([]);
  readonly temporadas = signal<Temporada[]>([]);
  readonly ocupado = signal(false);
  textoHuesped = '';
  ordenAptos = 'codigo';

  // formularios (modal)
  readonly editandoApto = signal<{ nuevo: boolean; datos: FormApto } | null>(null);
  readonly editandoHuesped = signal<{ nuevo: boolean; datos: Record<string, string> } | null>(null);
  readonly editandoEmpleado = signal<{ nuevo: boolean; datos: Record<string, string | boolean> } | null>(null);
  readonly editandoTemporada = signal<{ id: string | null; datos: Record<string, string> } | null>(null);
  readonly fueraServicio = signal<Apartamento | null>(null);
  motivo = '';
  readonly resultadoCarga = signal<string>('');

  ngOnInit(): void {
    this.cargarTodo();
  }

  cargarTodo(): void {
    this.cargarAptos();
    this.cargarHuespedes();
    this.api.empleados().subscribe(e => this.empleados.set(e));
    this.api.temporadas().subscribe(t => this.temporadas.set(t));
  }

  cargarAptos(): void {
    this.api.apartamentos(this.ordenAptos).subscribe(a => this.apartamentos.set(a));
  }

  cargarHuespedes(): void {
    this.api.huespedes(this.textoHuesped).subscribe(h => this.huespedes.set(h));
  }

  private ejecutar<T>(op: Observable<T>, mensaje: string, despues: () => void): void {
    this.ocupado.set(true);
    op.subscribe({
      next: () => { this.avisos.exito(mensaje); this.ocupado.set(false); despues(); },
      error: () => this.ocupado.set(false)
    });
  }

  // ---------------------------------------------------------------- apartamentos

  nuevoApto(): void {
    this.editandoApto.set({ nuevo: true, datos: { codigo: '', nombre: '', bloque: 'A', piso: 1, dormitorios: 1, capacidad: 2, caracteristicas: 'Wi-Fi 24 h, Cocina equipada',
      tarifaBaja: 100000, tarifaMedia: 125000, tarifaAlta: 160000, descripcion: '', imagenUrl: '' } });
  }

  editarApto(a: Apartamento): void {
    this.editandoApto.set({ nuevo: false, datos: { codigo: a.codigo, nombre: a.nombre, bloque: a.bloque, piso: a.piso, dormitorios: a.dormitorios,
      capacidad: a.capacidad, caracteristicas: a.caracteristicas.join(', '), tarifaBaja: a.tarifaBaja, tarifaMedia: a.tarifaMedia,
      tarifaAlta: a.tarifaAlta, descripcion: a.descripcion ?? '', imagenUrl: a.imagenUrl ?? '' } });
  }

  guardarApto(): void {
    const e = this.editandoApto();
    if (!e) return;
    const cuerpo = { ...e.datos, caracteristicas: e.datos.caracteristicas.split(',').map(s => s.trim()).filter(Boolean) };
    const op = e.nuevo ? this.api.crearApartamento(cuerpo) : this.api.actualizarApartamento(e.datos.codigo, cuerpo);
    this.ejecutar(op, e.nuevo ? 'Apartamento creado' : 'Apartamento actualizado', () => { this.editandoApto.set(null); this.cargarAptos(); });
  }

  async eliminarApto(a: Apartamento): Promise<void> {
    if (await this.confirmacion.confirmar('Eliminar apartamento', `¿Eliminar ${a.codigo} · ${a.nombre}? También se retirará del mapa.`, 'Eliminar', true)) {
      this.ejecutar(this.api.eliminarApartamento(a.codigo), 'Apartamento eliminado', () => this.cargarAptos());
    }
  }

  marcarFueraServicio(): void {
    const a = this.fueraServicio();
    if (!a) return;
    this.ejecutar(this.api.cambiarEstadoApartamento(a.codigo, 'FUERA_SERVICIO', this.motivo), 'Apartamento fuera de servicio',
      () => { this.fueraServicio.set(null); this.motivo = ''; this.cargarAptos(); });
  }

  devolverAlServicio(a: Apartamento): void {
    this.ejecutar(this.api.cambiarEstadoApartamento(a.codigo, 'PENDIENTE_PREPARACION'), 'Apartamento devuelto al servicio (pendiente de preparación)',
      () => this.cargarAptos());
  }

  // ---------------------------------------------------------------- huéspedes

  nuevoHuesped(): void {
    this.editandoHuesped.set({ nuevo: true, datos: { documento: '', nombre: '', fechaNacimiento: '', telefono: '', correo: '', clave: '' } });
  }

  editarHuesped(h: Huesped): void {
    this.editandoHuesped.set({ nuevo: false, datos: { documento: h.documento, nombre: h.nombre, fechaNacimiento: h.fechaNacimiento, telefono: h.telefono, correo: h.correo, clave: '' } });
  }

  guardarHuesped(): void {
    const e = this.editandoHuesped();
    if (!e) return;
    const cuerpo = { ...e.datos, clave: e.datos['clave'] || null };
    const op = e.nuevo ? this.api.crearHuesped(cuerpo) : this.api.actualizarHuesped(e.datos['documento'], cuerpo);
    this.ejecutar(op, e.nuevo ? 'Huésped registrado' : 'Huésped actualizado', () => { this.editandoHuesped.set(null); this.cargarHuespedes(); });
  }

  async eliminarHuesped(h: Huesped): Promise<void> {
    if (await this.confirmacion.confirmar('Eliminar huésped', `¿Eliminar el perfil de ${h.nombre}?`, 'Eliminar', true)) {
      this.ejecutar(this.api.eliminarHuesped(h.documento), 'Huésped eliminado', () => this.cargarHuespedes());
    }
  }

  // ---------------------------------------------------------------- empleados

  nuevoEmpleado(): void {
    this.editandoEmpleado.set({ nuevo: true, datos: { documento: '', nombre: '', rol: 'RECEPCIONISTA', usuario: '', correo: '', telefono: '', clave: '', activo: true } });
  }

  editarEmpleado(e: Empleado): void {
    this.editandoEmpleado.set({ nuevo: false, datos: { documento: e.documento, nombre: e.nombre, rol: e.rol, usuario: e.usuario, correo: e.correo, telefono: e.telefono, clave: '', activo: e.activo } });
  }

  guardarEmpleado(): void {
    const e = this.editandoEmpleado();
    if (!e) return;
    const cuerpo = { ...e.datos, clave: e.datos['clave'] || null };
    const op = e.nuevo ? this.api.crearEmpleado(cuerpo) : this.api.actualizarEmpleado(String(e.datos['documento']), cuerpo);
    this.ejecutar(op, e.nuevo ? 'Empleado creado' : 'Empleado actualizado', () => { this.editandoEmpleado.set(null); this.api.empleados().subscribe(l => this.empleados.set(l)); });
  }

  async eliminarEmpleado(e: Empleado): Promise<void> {
    if (await this.confirmacion.confirmar('Eliminar empleado', `¿Eliminar a ${e.nombre}? Saldrá de la rotación de turnos.`, 'Eliminar', true)) {
      this.ejecutar(this.api.eliminarEmpleado(e.documento), 'Empleado eliminado', () => this.api.empleados().subscribe(l => this.empleados.set(l)));
    }
  }

  // ---------------------------------------------------------------- temporadas

  nuevaTemporada(): void {
    this.editandoTemporada.set({ id: null, datos: { nombre: '', tipo: 'MEDIA', inicio: '', fin: '' } });
  }

  editarTemporada(t: Temporada): void {
    this.editandoTemporada.set({ id: t.id, datos: { nombre: t.nombre, tipo: t.tipo, inicio: t.inicio, fin: t.fin } });
  }

  guardarTemporada(): void {
    const e = this.editandoTemporada();
    if (!e) return;
    const op = e.id ? this.api.actualizarTemporada(e.id, e.datos) : this.api.crearTemporada(e.datos);
    this.ejecutar(op, 'Temporada guardada', () => { this.editandoTemporada.set(null); this.api.temporadas().subscribe(l => this.temporadas.set(l)); });
  }

  async eliminarTemporada(t: Temporada): Promise<void> {
    if (await this.confirmacion.confirmar('Eliminar temporada', `¿Eliminar "${t.nombre}"? Esas fechas pasarán a temporada baja.`, 'Eliminar', true)) {
      this.ejecutar(this.api.eliminarTemporada(t.id), 'Temporada eliminada', () => this.api.temporadas().subscribe(l => this.temporadas.set(l)));
    }
  }

  // ---------------------------------------------------------------- datos

  async cargarEscenario(): Promise<void> {
    if (!(await this.confirmacion.confirmar('Cargar escenario de prueba',
      'Se reemplazarán TODOS los datos actuales por el escenario inicial (apartamentos, pasillos, huéspedes, empleados y reservas).', 'Cargar escenario', true))) return;
    this.ocupado.set(true);
    this.api.cargarEscenario().subscribe({
      next: r => this.trasCarga(r.mensaje),
      error: () => this.ocupado.set(false)
    });
  }

  subirEscenario(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    (evento.target as HTMLInputElement).value = '';
    if (!archivo) return;
    this.ocupado.set(true);
    this.api.cargarEscenarioArchivo(archivo).subscribe({ next: r => this.trasCarga(r.mensaje), error: () => this.ocupado.set(false) });
  }

  private trasCarga(mensaje: string): void {
    this.ocupado.set(false);
    this.resultadoCarga.set(mensaje);
    this.avisos.exito('Datos cargados', mensaje);
    this.cargarTodo();
  }
}
