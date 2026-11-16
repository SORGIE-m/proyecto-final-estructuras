import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { FechaPipe, HoraPipe, MonedaPipe } from '../../core/formatos';
import { Apartamento, Folio, Huesped, Reembolso, Reserva } from '../../core/modelos';
import { NotificacionesService } from '../../core/notificaciones.service';
import { SesionService } from '../../core/sesion.service';
import { Icono } from '../../compartido/icono';
import { ImagenApartamento } from '../../compartido/imagen-apartamento';
import { Modal } from '../../compartido/modal';

type Pestana = 'reservas' | 'favoritos' | 'historial' | 'perfil';

@Component({
  selector: 'app-mi-cuenta',
  imports: [FormsModule, RouterLink, Icono, ImagenApartamento, Modal, MonedaPipe, FechaPipe, HoraPipe],
  templateUrl: './mi-cuenta.html',
  styleUrl: './mi-cuenta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MiCuenta implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  readonly sesion = inject(SesionService);

  readonly pestana = signal<Pestana>('reservas');
  readonly perfil = signal<Huesped | null>(null);
  readonly reservas = signal<Reserva[]>([]);
  readonly favoritos = signal<Apartamento[]>([]);
  readonly historial = signal<Reserva[]>([]);
  readonly cargando = signal(true);

  readonly activas = computed(() => this.reservas().filter(r => ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'].includes(r.estado)));
  readonly cerradas = computed(() => this.reservas().filter(r => ['CANCELADA', 'NO_SHOW'].includes(r.estado)));
  readonly saldoTotal = computed(() => this.activas().reduce((s, r) => s + Math.max(0, r.saldo), 0));

  readonly folio = signal<Folio | null>(null);
  readonly cancelando = signal<{ reserva: Reserva; reembolso: Reembolso } | null>(null);
  readonly ocupado = signal(false);
  motivo = '';
  editable = { nombre: '', fechaNacimiento: '', telefono: '', correo: '', clave: '', foto: '' as string | null };

  private get doc(): string {
    return this.sesion.sesion()!.identificador;
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.perfil().subscribe(p => {
      this.perfil.set(p);
      this.editable = { nombre: p.nombre, fechaNacimiento: p.fechaNacimiento, telefono: p.telefono, correo: p.correo, clave: '', foto: p.foto };
    });
    this.api.reservasDe(this.doc).subscribe({ next: r => { this.reservas.set(r); this.cargando.set(false); }, error: () => this.cargando.set(false) });
    this.api.favoritos(this.doc).subscribe(f => this.favoritos.set(f));
    this.api.historial(this.doc).subscribe(h => this.historial.set(h));
  }

  pagarAnticipo(r: Reserva): void {
    this.ocupado.set(true);
    this.api.anticipo(r.codigo).subscribe({
      next: () => { this.avisos.exito('Anticipo pagado', `La reserva ${r.codigo} quedó confirmada.`); this.ocupado.set(false); this.cargar(); },
      error: () => this.ocupado.set(false)
    });
  }

  pagarSaldo(r: Reserva): void {
    this.ocupado.set(true);
    this.api.pago(r.codigo, 'Pago del saldo desde el portal', r.saldo).subscribe({
      next: f => { this.avisos.exito('Pago registrado', 'Tu folio quedó al día.'); this.folio.set(this.folio() ? f : null); this.ocupado.set(false); this.cargar(); },
      error: () => this.ocupado.set(false)
    });
  }

  verFolio(r: Reserva): void {
    this.api.folio(r.codigo).subscribe(f => this.folio.set(f));
  }

  prepararCancelacion(r: Reserva): void {
    this.motivo = '';
    this.api.previsualizarCancelacion(r.codigo).subscribe(re => this.cancelando.set({ reserva: r, reembolso: re }));
  }

  confirmarCancelacion(): void {
    const c = this.cancelando();
    if (!c) return;
    this.ocupado.set(true);
    this.api.cancelar(c.reserva.codigo, this.motivo).subscribe({
      next: () => { this.avisos.exito('Reserva cancelada', `Se te devolverán ${c.reembolso.reembolso.toLocaleString('es-CO')} COP.`); this.cancelando.set(null); this.ocupado.set(false); this.cargar(); },
      error: () => this.ocupado.set(false)
    });
  }

  quitarFavorito(a: Apartamento): void {
    this.api.quitarFavorito(this.doc, a.codigo).subscribe(f => this.favoritos.set(f));
  }

  elegirFoto(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    if (archivo.size > 900_000) {
      this.avisos.aviso('Imagen muy grande', 'Elige una fotografía de menos de 900 KB.');
      return;
    }
    const lector = new FileReader();
    lector.onload = () => { this.editable = { ...this.editable, foto: String(lector.result) }; this.guardarPerfil(); };
    lector.readAsDataURL(archivo);
  }

  guardarPerfil(): void {
    const p = this.perfil();
    if (!p) return;
    const e = this.editable;
    this.ocupado.set(true);
    this.api.actualizarHuesped(p.documento, {
      documento: p.documento, nombre: e.nombre, fechaNacimiento: e.fechaNacimiento, telefono: e.telefono, correo: e.correo,
      clave: e.clave || null, foto: e.foto ?? ''
    }).subscribe({
      next: h => { this.perfil.set(h); this.editable.clave = ''; this.ocupado.set(false); this.avisos.exito('Perfil actualizado'); },
      error: () => this.ocupado.set(false)
    });
  }

  quitarFoto(): void {
    this.editable = { ...this.editable, foto: '' };
    this.guardarPerfil();
  }
}
