import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { FechaPipe, HoraPipe } from '../../../core/formatos';
import { Apartamento, ItemPreparacion, Mapa, Ruta } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { Icono } from '../../../compartido/icono';
import { MapaEdificio } from '../../../compartido/mapa-edificio';

@Component({
  selector: 'app-preparacion',
  imports: [Icono, MapaEdificio, FechaPipe, HoraPipe],
  template: `
    <div class="pagina">
      <div class="encabezado-pagina">
        <div>
          <span class="sobretitulo">Cola de prioridad</span>
          <h1 class="titulo-display">Preparación de <span class="cursiva">apartamentos</span></h1>
          <p>Primero los apartamentos con entrada programada para hoy (por hora de llegada), luego el resto por antigüedad.</p>
        </div>
        <button class="btn" type="button" [disabled]="!cola().length || ocupado()" (click)="despachar()">
          <app-icono nombre="escoba" [tamano]="16" /> Despachar siguiente
        </button>
      </div>

      <div class="rejilla distribucion">
        <div class="columna">
          <section class="tarjeta">
            <h3>Pendientes <span class="chip sin-punto">{{ cola().length }}</span></h3>
            <div class="cola escalonado">
              @for (i of cola(); track i.apartamentoCodigo; let pos = $index) {
                <div class="item" [class.urgente]="i.prioridad === 1">
                  <span class="pos">{{ pos + 1 }}</span>
                  <div class="crece">
                    <div class="fila"><strong>{{ i.apartamentoCodigo }}</strong><span class="cursiva">{{ i.apartamentoNombre }}</span></div>
                    @if (i.entradaHoy) {
                      <span class="detalle"><app-icono nombre="reloj" [tamano]="13" /> Llega {{ i.huespedEntrante }} a las {{ i.horaLlegada | hora }}</span>
                    } @else {
                      <span class="detalle tenue">Sin entrada hoy · pendiente desde {{ i.pendienteDesde | fecha: 'hora' }}</span>
                    }
                  </div>
                  <span class="prioridad">P{{ i.prioridad }}</span>
                  <button class="btn secundario pequeno" type="button" (click)="iniciar(i.apartamentoCodigo)">Iniciar</button>
                </div>
              } @empty {
                <div class="vacio"><span class="cursiva">Todo en orden</span>No hay apartamentos pendientes de preparación.</div>
              }
            </div>
          </section>

          <section class="tarjeta">
            <h3>En preparación <span class="chip sin-punto">{{ enCurso().length }}</span></h3>
            @for (a of enCurso(); track a.codigo) {
              <div class="item en-curso">
                <span class="pulso"></span>
                <div class="crece"><strong>{{ a.codigo }}</strong> <span class="cursiva">{{ a.nombre }}</span>
                  <span class="detalle tenue">Inició {{ a.enPreparacionDesde | fecha: 'hora' }}</span></div>
                <button class="btn exito pequeno" type="button" (click)="finalizar(a.codigo)"><app-icono nombre="check" [tamano]="14" /> Preparado</button>
              </div>
            } @empty { <div class="vacio">Nadie está preparando apartamentos en este momento.</div> }
          </section>
        </div>

        <section class="tarjeta">
          <div class="fila-entre mb">
            <h3 class="tarjeta-titulo" style="margin:0">Ruta sugerida</h3>
            <div class="pestanas">
              <button type="button" [class.activa]="algoritmo() === 'DIJKSTRA'" (click)="cambiarAlgoritmo('DIJKSTRA')">Dijkstra</button>
              <button type="button" [class.activa]="algoritmo() === 'BFS'" (click)="cambiarAlgoritmo('BFS')">BFS</button>
            </div>
          </div>
          <app-mapa-edificio [mapa]="mapa()" [ruta]="ruta()?.camino ?? []" [paradas]="ruta()?.paradas ?? []" [inalcanzables]="ruta()?.inalcanzables ?? []" />
          @if (ruta(); as r) {
            <div class="resumen-ruta">
              <div><small>Recorrido</small><strong>{{ r.metros }} m</strong></div>
              <div><small>Paradas</small><strong>{{ r.paradas.length }}</strong></div>
              <div><small>Tramos</small><strong>{{ r.camino.length > 0 ? r.camino.length - 1 : 0 }}</strong></div>
            </div>
            @if (r.paradas.length) {
              <div class="pasos">
                <span class="chip sin-punto">Recepción</span>
                @for (p of r.paradas; track p) { <app-icono nombre="adelante" [tamano]="14" /><span class="chip PENDIENTE_PREPARACION">{{ p }}</span> }
              </div>
            }
            @if (r.inalcanzables.length) { <p class="alerta-ruta">Inalcanzables por pasillos cerrados: {{ r.inalcanzables.join(', ') }}</p> }
          }
        </section>
      </div>
    </div>`,
  styles: [`
    .distribucion { grid-template-columns: minmax(340px, 1fr) 1.35fr; align-items: start; }
    .cola { display: flex; flex-direction: column; gap: 8px; }
    .item { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 14px; background: var(--superficie-2); border: 1px solid var(--borde);
      transition: transform .2s ease, box-shadow .2s ease; &:hover { transform: translateX(3px); box-shadow: var(--sombra-sm); }
      strong { font-weight: 500; letter-spacing: .06em; } .cursiva { font-size: 17px; }
      .detalle { display: flex; gap: 6px; align-items: center; font-size: 12.5px; color: var(--texto-suave); margin-top: 2px; } }
    .item.urgente { background: var(--estado-pendiente-bg); border-color: #efd9a8; }
    .pos { width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; background: var(--tierra-700); color: #fff; font-size: 13px; flex-shrink: 0; }
    .urgente .pos { background: #b8861f; animation: pulso 2s infinite; }
    .prioridad { font-size: 11px; letter-spacing: .14em; color: var(--texto-tenue); }
    .en-curso { background: var(--estado-pendiente-bg); }
    .pulso { width: 12px; height: 12px; border-radius: 50%; background: var(--estado-pendiente); animation: pulso 1.6s infinite; flex-shrink: 0; }
    .resumen-ruta { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 14px;
      div { padding: 12px; border-radius: 12px; background: var(--superficie-2); border: 1px solid var(--borde); text-align: center; }
      small { display: block; font-size: 10.5px; text-transform: uppercase; letter-spacing: .16em; color: var(--texto-tenue); }
      strong { font-family: var(--fuente-display); font-size: 26px; font-weight: 500; } }
    .pasos { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-top: 14px; color: var(--texto-tenue); }
    .alerta-ruta { color: var(--peligro); font-size: 13px; }
    @media (max-width: 1100px) { .distribucion { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Preparacion implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  readonly cola = signal<ItemPreparacion[]>([]);
  readonly enCurso = signal<Apartamento[]>([]);
  readonly mapa = signal<Mapa | null>(null);
  readonly ruta = signal<Ruta | null>(null);
  readonly algoritmo = signal<'DIJKSTRA' | 'BFS'>('DIJKSTRA');
  readonly ocupado = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    forkJoin({ c: this.api.colaPreparacion(), e: this.api.enPreparacion(), m: this.api.mapa(), r: this.api.rutaPreparacion(this.algoritmo()) })
      .subscribe(x => { this.cola.set(x.c); this.enCurso.set(x.e); this.mapa.set(x.m); this.ruta.set(x.r); this.ocupado.set(false); });
  }

  cambiarAlgoritmo(a: 'DIJKSTRA' | 'BFS'): void {
    this.algoritmo.set(a);
    this.api.rutaPreparacion(a).subscribe(r => this.ruta.set(r));
  }

  despachar(): void {
    this.ocupado.set(true);
    this.api.despachar().subscribe({
      next: i => { this.avisos.exito('Preparación iniciada', `${i.apartamentoCodigo} · ${i.apartamentoNombre}`); this.cargar(); },
      error: () => this.ocupado.set(false)
    });
  }

  iniciar(codigo: string): void {
    this.api.iniciarPreparacion(codigo).subscribe(() => { this.avisos.info('Preparación iniciada', codigo); this.cargar(); });
  }

  finalizar(codigo: string): void {
    this.api.finalizarPreparacion(codigo).subscribe(() => { this.avisos.exito('Apartamento preparado', `${codigo} listo para recibir huéspedes`); this.cargar(); });
  }
}
