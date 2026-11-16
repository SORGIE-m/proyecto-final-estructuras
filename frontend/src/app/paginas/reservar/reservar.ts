import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { FechaPipe, MonedaPipe, isoLocal, sumarDias } from '../../core/formatos';
import { Cotizacion, Ocupante, Opcion, Reserva } from '../../core/modelos';
import { NotificacionesService } from '../../core/notificaciones.service';
import { SesionService } from '../../core/sesion.service';
import { Icono } from '../../compartido/icono';
import { ImagenApartamento } from '../../compartido/imagen-apartamento';
import { Modal } from '../../compartido/modal';

@Component({
  selector: 'app-reservar',
  imports: [FormsModule, Icono, ImagenApartamento, Modal, MonedaPipe, FechaPipe],
  templateUrl: './reservar.html',
  styleUrl: './reservar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Reservar implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionesService);
  readonly sesion = inject(SesionService);

  readonly hoy = isoLocal(new Date());
  entrada = sumarDias(this.hoy, 7);
  salida = sumarDias(this.hoy, 10);
  personas = 2;
  orden = 'precio';

  readonly opciones = signal<Opcion[]>([]);
  readonly buscando = signal(false);
  readonly buscado = signal(false);
  readonly favoritos = signal<string[]>([]);

  readonly seleccion = signal<Opcion | null>(null);
  readonly ocupantes = signal<Ocupante[]>([]);
  readonly cotizacion = signal<Cotizacion | null>(null);
  readonly cotizando = signal(false);
  readonly reservando = signal(false);
  readonly creada = signal<Reserva | null>(null);
  horaLlegada = '15:00';
  observaciones = '';

  readonly noches = computed(() => this.cotizacion()?.noches ?? 0);

  ngOnInit(): void {
    const q = this.ruta.snapshot.queryParamMap;
    this.entrada = q.get('entrada') ?? this.entrada;
    this.salida = q.get('salida') ?? this.salida;
    this.personas = Number(q.get('personas') ?? this.personas);
    if (this.sesion.esHuesped()) {
      this.api.favoritos(this.sesion.sesion()!.identificador).subscribe(f => this.favoritos.set(f.map(a => a.codigo)));
    }
    this.buscar();
  }

  buscar(): void {
    if (!this.entrada || !this.salida) return;
    this.buscando.set(true);
    this.api.disponibilidad(this.entrada, this.salida, this.personas, this.orden).subscribe({
      next: l => { this.opciones.set(l); this.buscando.set(false); this.buscado.set(true); },
      error: () => { this.opciones.set([]); this.buscando.set(false); this.buscado.set(true); }
    });
  }

  esFavorito(codigo: string): boolean {
    return this.favoritos().includes(codigo);
  }

  alternarFavorito(codigo: string, evento: Event): void {
    evento.stopPropagation();
    if (!this.sesion.esHuesped()) {
      this.avisos.info('Inicia sesión', 'Ingresa como huésped para guardar favoritos.');
      return;
    }
    const doc = this.sesion.sesion()!.identificador;
    const op = this.esFavorito(codigo) ? this.api.quitarFavorito(doc, codigo) : this.api.agregarFavorito(doc, codigo);
    op.subscribe(l => this.favoritos.set(l.map(a => a.codigo)));
  }

  abrir(o: Opcion): void {
    this.seleccion.set(o);
    this.creada.set(null);
    this.cotizacion.set(null);
    const base: Ocupante[] = [];
    for (let i = 0; i < this.personas; i++) base.push({ nombre: '', fechaNacimiento: '' });
    this.ocupantes.set(base);
    if (this.sesion.esHuesped()) {
      this.api.perfil().subscribe(p => {
        const l = [...this.ocupantes()];
        l[0] = { nombre: p.nombre, documento: p.documento, fechaNacimiento: p.fechaNacimiento };
        this.ocupantes.set(l);
        this.recotizar();
      });
    }
    this.recotizar();
  }

  cerrar(): void {
    this.seleccion.set(null);
  }

  agregarOcupante(): void {
    const s = this.seleccion();
    if (s && this.ocupantes().length < s.apartamento.capacidad) {
      this.ocupantes.update(l => [...l, { nombre: '', fechaNacimiento: '' }]);
    }
  }

  quitarOcupante(i: number): void {
    if (this.ocupantes().length > 1) {
      this.ocupantes.update(l => l.filter((_, k) => k !== i));
      this.recotizar();
    }
  }

  actualizar(i: number, campo: 'nombre' | 'fechaNacimiento', valor: string): void {
    this.ocupantes.update(l => l.map((o, k) => (k === i ? { ...o, [campo]: valor } : o)));
    if (campo === 'fechaNacimiento') this.recotizar();
  }

  /** Cotiza con las fechas de nacimiento conocidas (los menores de 6 años no pagan). */
  recotizar(): void {
    const s = this.seleccion();
    if (!s) return;
    const ocupantes = this.ocupantes().map(o => ({ nombre: o.nombre || 'Ocupante', fechaNacimiento: o.fechaNacimiento || '1990-01-01' }));
    this.cotizando.set(true);
    this.api.cotizar(s.apartamento.codigo, this.entrada, this.salida, ocupantes).subscribe({
      next: c => { this.cotizacion.set(c); this.cotizando.set(false); },
      error: () => this.cotizando.set(false)
    });
  }

  reservar(): void {
    const s = this.seleccion();
    if (!s) return;
    if (!this.sesion.esHuesped()) {
      this.avisos.info('Inicia sesión para reservar', 'Tu búsqueda se conservará.');
      this.router.navigate(['/ingresar'], { queryParams: { volver: this.router.url } });
      return;
    }
    if (this.ocupantes().some(o => !o.nombre.trim() || !o.fechaNacimiento)) {
      this.avisos.aviso('Faltan datos de ocupantes', 'Registra el nombre y la fecha de nacimiento de cada persona.');
      return;
    }
    this.reservando.set(true);
    this.api.reservarPortal({
      apartamentoCodigo: s.apartamento.codigo, entrada: this.entrada, salida: this.salida,
      horaLlegada: this.horaLlegada, ocupantes: this.ocupantes(), observaciones: this.observaciones
    }).subscribe({
      next: r => { this.creada.set(r); this.reservando.set(false); this.avisos.exito('Reserva registrada', `Código ${r.codigo}`); this.buscar(); },
      error: () => this.reservando.set(false)
    });
  }

  pagarAnticipo(): void {
    const r = this.creada();
    if (!r) return;
    this.reservando.set(true);
    this.api.anticipo(r.codigo).subscribe({
      next: () => {
        this.avisos.exito('Anticipo pagado', 'Tu reserva quedó confirmada.');
        this.reservando.set(false);
        this.router.navigateByUrl('/mi-cuenta');
      },
      error: () => this.reservando.set(false)
    });
  }
}
