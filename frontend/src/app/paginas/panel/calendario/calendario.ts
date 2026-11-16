import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/api.service';
import { aFecha, isoLocal } from '../../../core/formatos';
import { Calendario as DatosCalendario } from '../../../core/modelos';
import { Icono } from '../../../compartido/icono';

@Component({
  selector: 'app-calendario',
  imports: [Icono],
  template: `
    <div class="pagina">
      <div class="encabezado-pagina">
        <div>
          <span class="sobretitulo">Ocupación</span>
          <h1 class="titulo-display">Calendario de <span class="cursiva">{{ nombreMes() }}</span></h1>
          <p>Noches ocupadas, pendientes de anticipo, bloqueadas y libres por apartamento. La noche de salida no se ocupa.</p>
        </div>
        <div class="fila">
          <button class="btn secundario icono" type="button" (click)="mover(-1)" aria-label="Mes anterior"><app-icono nombre="atras" /></button>
          <button class="btn secundario pequeno" type="button" (click)="irHoy()">Hoy</button>
          <button class="btn secundario icono" type="button" (click)="mover(1)" aria-label="Mes siguiente"><app-icono nombre="adelante" /></button>
        </div>
      </div>

      <div class="leyenda">
        <span><i class="OCUPADA"></i> Ocupada</span><span><i class="PENDIENTE"></i> Pendiente de anticipo</span>
        <span><i class="BLOQUEADA"></i> Bloqueada (fuera de servicio)</span><span><i class="LIBRE"></i> Libre</span>
      </div>

      <div class="tarjeta rejilla-cal" [class.cargando]="cargando()">
        @if (datos(); as d) {
          <table>
            <thead>
              <tr>
                <th class="fija">Apartamento</th>
                @for (n of d.noches; track n) {
                  <th [class.hoy]="n === hoy" [class.finde]="esFinde(n)"><small>{{ diaSemana(n) }}</small>{{ dia(n) }}</th>
                }
                <th>Noches</th>
              </tr>
            </thead>
            <tbody>
              @for (f of d.filas; track f.apartamento) {
                <tr>
                  <td class="fija"><strong>{{ f.apartamento }}</strong><span>{{ f.nombre }}</span></td>
                  @for (c of f.celdas; track c.fecha; let i = $index) {
                    <td [class]="c.estado" [class.hoy]="c.fecha === hoy"
                        [class.inicio]="c.reserva && (i === 0 || f.celdas[i - 1].reserva !== c.reserva)"
                        [class.fin]="c.reserva && (i === f.celdas.length - 1 || f.celdas[i + 1].reserva !== c.reserva)"
                        [title]="c.reserva ? c.reserva + ' · ' + c.huesped : (c.estado === 'BLOQUEADA' ? 'Fuera de servicio' : 'Libre')">
                      <span></span>
                    </td>
                  }
                  <td class="total">{{ f.nochesOcupadas }}</td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <div class="esqueleto" style="height: 480px"></div>
        }
      </div>
      <p class="tenue mt">Ocupación del mes: {{ ocupacion() }} % de {{ totalNoches() }} noches-apartamento.</p>
    </div>`,
  styles: [`
    .leyenda { display: flex; gap: 18px; flex-wrap: wrap; margin-bottom: 16px; font-size: 13px; color: var(--texto-suave);
      span { display: inline-flex; gap: 8px; align-items: center; } i { width: 16px; height: 12px; border-radius: 4px; }
      i.OCUPADA { background: var(--estado-ocupado); } i.PENDIENTE { background: var(--estado-pendiente); }
      i.BLOQUEADA { background: repeating-linear-gradient(45deg, var(--estado-fuera), var(--estado-fuera) 3px, var(--estado-fuera-bg) 3px, var(--estado-fuera-bg) 6px); }
      i.LIBRE { background: var(--arena-100); border: 1px solid var(--borde); } }
    .rejilla-cal { padding: 0; overflow-x: auto; transition: opacity .2s ease; &.cargando { opacity: .6; } }
    table { border-collapse: separate; border-spacing: 0; width: 100%; font-size: 12px; }
    th { position: sticky; top: 0; background: var(--superficie-2); font-weight: 500; color: var(--texto-suave); padding: 10px 2px; min-width: 30px;
      text-align: center; border-bottom: 1px solid var(--borde); small { display: block; font-size: 9.5px; color: var(--texto-tenue); text-transform: uppercase; } }
    th.finde { color: var(--tierra-600); } th.hoy { color: #fff; background: var(--tierra-700); border-radius: 10px 10px 0 0; small { color: var(--acento-claro); } }
    .fija { position: sticky; left: 0; z-index: 2; background: var(--superficie); text-align: left; padding: 8px 16px; min-width: 190px;
      border-right: 1px solid var(--borde); strong { font-weight: 500; letter-spacing: .06em; margin-right: 8px; } span { color: var(--texto-tenue); font-family: var(--fuente-display); font-style: italic; font-size: 15px; } }
    th.fija { z-index: 3; background: var(--superficie-2); }
    td { padding: 6px 0; border-bottom: 1px solid var(--arena-200); text-align: center; }
    td.hoy { background: rgba(107, 91, 77, .06); }
    td > span { display: block; height: 22px; margin: 0 -1px; background: transparent; transition: transform .2s ease; }
    td.LIBRE > span { margin: 0 3px; border-radius: 6px; background: var(--arena-50); }
    td.OCUPADA > span { background: var(--estado-ocupado); }
    td.PENDIENTE > span { background: var(--estado-pendiente); }
    td.BLOQUEADA > span { margin: 0 2px; border-radius: 4px; background: repeating-linear-gradient(45deg, var(--estado-fuera), var(--estado-fuera) 3px, var(--estado-fuera-bg) 3px, var(--estado-fuera-bg) 6px); }
    td.inicio > span { border-top-left-radius: 11px; border-bottom-left-radius: 11px; margin-left: 3px; }
    td.fin > span { border-top-right-radius: 11px; border-bottom-right-radius: 11px; margin-right: 3px; }
    td:hover > span { transform: scaleY(1.2); }
    td.total { font-weight: 500; color: var(--tierra-700); padding: 0 12px; }
    tbody tr { animation: aparecer .4s both; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Calendario implements OnInit {
  private readonly api = inject(ApiService);
  readonly hoy = isoLocal(new Date());
  readonly mes = signal(this.hoy.substring(0, 7));
  readonly datos = signal<DatosCalendario | null>(null);
  readonly cargando = signal(false);

  readonly nombreMes = computed(() => {
    const [a, m] = this.mes().split('-').map(Number);
    return new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' }).format(new Date(a, m - 1, 1));
  });
  readonly totalNoches = computed(() => (this.datos()?.filas.length ?? 0) * (this.datos()?.noches.length ?? 0));
  readonly ocupacion = computed(() => {
    const total = this.totalNoches();
    const ocupadas = (this.datos()?.filas ?? []).reduce((s, f) => s + f.nochesOcupadas, 0);
    return total ? Math.round((ocupadas * 1000) / total) / 10 : 0;
  });

  ngOnInit(): void {
    this.api.hoy().subscribe(h => { this.mes.set(h.substring(0, 7)); this.cargar(); });
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.calendario(this.mes()).subscribe({ next: d => { this.datos.set(d); this.cargando.set(false); }, error: () => this.cargando.set(false) });
  }

  mover(delta: number): void {
    const [a, m] = this.mes().split('-').map(Number);
    const f = new Date(a, m - 1 + delta, 1);
    this.mes.set(`${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}`);
    this.cargar();
  }

  irHoy(): void {
    this.mes.set(this.hoy.substring(0, 7));
    this.cargar();
  }

  dia(iso: string): number { return aFecha(iso).getDate(); }
  diaSemana(iso: string): string { return ['D', 'L', 'M', 'M', 'J', 'V', 'S'][aFecha(iso).getDay()]; }
  esFinde(iso: string): boolean { const d = aFecha(iso).getDay(); return d === 0 || d === 6; }
}
