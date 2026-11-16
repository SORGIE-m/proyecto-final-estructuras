import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { FechaPipe, HoraPipe, MonedaPipe } from '../../../core/formatos';
import { Apartamento, Reserva, Tablero as DatosTablero } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { SesionService } from '../../../core/sesion.service';
import { Icono } from '../../../compartido/icono';

@Component({
  selector: 'app-tablero',
  imports: [RouterLink, Icono, MonedaPipe, FechaPipe, HoraPipe],
  templateUrl: './tablero.html',
  styleUrl: './tablero.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Tablero implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  readonly sesion = inject(SesionService);

  readonly datos = signal<DatosTablero | null>(null);
  readonly apartamentos = signal<Apartamento[]>([]);
  readonly llegadas = signal<Reserva[]>([]);
  readonly salidas = signal<Reserva[]>([]);
  readonly filtro = signal<string>('TODOS');
  readonly hoy = signal<string>('');
  private intervalo?: ReturnType<typeof setInterval>;

  readonly bloques = computed(() => {
    const f = this.filtro();
    const lista = this.apartamentos().filter(a => f === 'TODOS' || a.estado === f
      || (f === 'PENDIENTE_PREPARACION' && a.estado === 'EN_PREPARACION'));
    const grupos = new Map<string, Apartamento[]>();
    for (const a of lista) grupos.set(a.bloque, [...(grupos.get(a.bloque) ?? []), a]);
    return [...grupos.entries()].map(([bloque, aptos]) => ({ bloque, aptos }));
  });

  readonly leyenda = [
    { estado: 'PREPARADO', texto: 'Preparado' },
    { estado: 'OCUPADO', texto: 'Ocupado' },
    { estado: 'PENDIENTE_PREPARACION', texto: 'Pendiente / En preparación' },
    { estado: 'FUERA_SERVICIO', texto: 'Fuera de servicio' }
  ];

  ngOnInit(): void {
    this.cargar();
    // actualización periódica: el tablero refleja el estado en tiempo (casi) real
    this.intervalo = setInterval(() => this.cargar(), 30000);
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalo);
  }

  cargar(): void {
    forkJoin({ t: this.api.tablero(), a: this.api.apartamentos(), l: this.api.llegadas(), s: this.api.salidas(), h: this.api.hoy() })
      .subscribe(r => {
        this.datos.set(r.t);
        this.apartamentos.set(r.a);
        this.llegadas.set(r.l);
        this.salidas.set(r.s);
        this.hoy.set(r.h);
      });
  }

  checkIn(r: Reserva): void {
    this.api.checkIn(r.codigo).subscribe(() => { this.avisos.exito('Check-in registrado', `${r.titularNombre} · ${r.apartamentoCodigo}`); this.cargar(); });
  }

  checkOut(r: Reserva): void {
    this.api.checkOut(r.codigo).subscribe(() => {
      this.avisos.exito('Check-out registrado', `${r.apartamentoCodigo} pasó a la cola de preparación`);
      this.cargar();
    });
  }

  contar(estado: string): number {
    const d = this.datos();
    if (!d) return 0;
    return ({ PREPARADO: d.preparados, OCUPADO: d.ocupados, PENDIENTE_PREPARACION: d.pendientes + d.enPreparacion, FUERA_SERVICIO: d.fueraServicio } as Record<string, number>)[estado] ?? 0;
  }
}
