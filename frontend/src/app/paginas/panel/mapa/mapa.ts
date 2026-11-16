import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/api.service';
import { GrupoBloque, Mapa as DatosMapa, Pasillo, Ruta } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { SesionService } from '../../../core/sesion.service';
import { Icono } from '../../../compartido/icono';
import { MapaEdificio } from '../../../compartido/mapa-edificio';

type Modo = 'ruta' | 'simular' | 'editar';

@Component({
  selector: 'app-mapa',
  imports: [Icono, MapaEdificio],
  template: `
    <div class="pagina">
      <div class="encabezado-pagina">
        <div>
          <span class="sobretitulo">Grafo del edificio</span>
          <h1 class="titulo-display">Mapa <span class="cursiva">interactivo</span></h1>
          <p>{{ ayuda() }}</p>
        </div>
        <div class="pestanas">
          <button type="button" [class.activa]="modo() === 'ruta'" (click)="cambiarModo('ruta')">Rutas</button>
          <button type="button" [class.activa]="modo() === 'simular'" (click)="cambiarModo('simular')">Simular cierre</button>
          @if (sesion.esAdmin()) { <button type="button" [class.activa]="modo() === 'editar'" (click)="cambiarModo('editar')">Abrir / cerrar pasillos</button> }
        </div>
      </div>

      <div class="rejilla distribucion">
        <section class="tarjeta lienzo">
          <app-mapa-edificio [mapa]="mapa()" [ruta]="caminoVisible()" [inalcanzables]="inalcanzables()" [seleccionados]="nodosResaltados()"
            [editable]="modo() !== 'ruta'" [mostrarMetros]="metros()" (nodoClic)="alClicNodo($event)" (pasilloClic)="alClicPasillo($event)" />
          <div class="fila-entre mt">
            <div class="leyenda">
              <span><i class="p"></i>Preparado</span><span><i class="o"></i>Ocupado</span><span><i class="e"></i>Pendiente / en preparación</span>
              <span><i class="f"></i>Fuera de servicio</span><span><i class="c"></i>Pasillo cerrado</span>
            </div>
            <label class="interruptor"><input type="checkbox" [checked]="metros()" (change)="metros.set(!metros())" /> Mostrar distancias</label>
          </div>
        </section>

        <div class="columna">
          <section class="tarjeta">
            <h3>Ruta más corta</h3>
            <div class="pestanas mb">
              <button type="button" [class.activa]="algoritmo() === 'DIJKSTRA'" (click)="algoritmo.set('DIJKSTRA'); calcular()">Dijkstra (metros)</button>
              <button type="button" [class.activa]="algoritmo() === 'BFS'" (click)="algoritmo.set('BFS'); calcular()">BFS (tramos)</button>
            </div>
            <div class="seleccion">
              <div><small>Origen</small><strong>{{ seleccion().at(0) ?? '—' }}</strong></div>
              <app-icono nombre="adelante" />
              <div><small>Destino</small><strong>{{ seleccion().at(1) ?? '—' }}</strong></div>
            </div>
            @if (ruta(); as r) {
              <div class="resultado">
                <strong class="metros">{{ r.metros }} m</strong>
                <span class="tenue">{{ r.camino.length - 1 }} tramo(s)</span>
                <div class="camino">@for (p of r.camino; track $index) { <span>{{ p }}</span> }</div>
              </div>
            }
            <button class="btn fantasma pequeno mt" type="button" (click)="limpiar()">Limpiar selección</button>
          </section>

          <section class="tarjeta">
            <h3>Recorridos</h3>
            <p class="tenue">Desde {{ seleccion().at(0) ?? 'RECEPCION' }}: orden de visita del grafo.</p>
            <div class="fila">
              <button class="btn secundario pequeno" type="button" (click)="recorrer('BFS')">Anchura (BFS)</button>
              <button class="btn secundario pequeno" type="button" (click)="recorrer('DFS')">Profundidad (DFS)</button>
            </div>
            @if (recorrido().length) {
              <div class="orden-recorrido">
                @for (n of recorrido(); track n; let i = $index) {
                  <span [class.visitado]="i < paso()">{{ i + 1 }}. {{ n }}</span>
                }
              </div>
            }
          </section>

          <section class="tarjeta">
            <h3>Conectividad <button class="btn fantasma pequeno" type="button" (click)="analizar()"><app-icono nombre="recargar" [tamano]="14" /></button></h3>
            @if (inalcanzablesActuales().length) {
              <p class="alerta">Inalcanzables desde recepción: {{ inalcanzablesActuales().join(', ') }}</p>
            } @else {
              <p class="ok"><app-icono nombre="check" [tamano]="15" /> Todos los puntos son alcanzables desde recepción.</p>
            }
            <span class="sobretitulo">Agrupación por bloque (DFS)</span>
            @for (g of bloques(); track g.bloque) {
              <div class="bloque"><strong>Bloque {{ g.bloque }}</strong><span class="tenue">{{ g.apartamentos.length }} apartamentos · {{ g.nodos.length }} puntos</span>
                <div class="aptos">@for (a of g.apartamentos; track a) { <span class="chip sin-punto">{{ a }}</span> }</div></div>
            }
          </section>
        </div>
      </div>
    </div>`,
  styles: [`
    .distribucion { grid-template-columns: 1.6fr minmax(320px, 1fr); align-items: start; }
    .lienzo { padding: 14px; }
    .leyenda { display: flex; gap: 14px; flex-wrap: wrap; font-size: 12.5px; color: var(--texto-suave);
      span { display: inline-flex; align-items: center; gap: 6px; } i { width: 14px; height: 10px; border-radius: 3px; }
      .p { background: var(--estado-preparado); } .o { background: var(--estado-ocupado); } .e { background: var(--estado-pendiente); }
      .f { background: var(--estado-fuera); } .c { background: repeating-linear-gradient(90deg, var(--estado-fuera) 0 3px, transparent 3px 6px); height: 3px; } }
    .interruptor { display: inline-flex; gap: 8px; align-items: center; font-size: 13px; color: var(--texto-suave); cursor: pointer; }
    .seleccion { display: flex; align-items: center; gap: 12px; color: var(--texto-tenue);
      div { flex: 1; padding: 10px 12px; border-radius: 12px; background: var(--superficie-2); border: 1px solid var(--borde); }
      small { display: block; font-size: 10.5px; text-transform: uppercase; letter-spacing: .16em; } strong { color: var(--tierra-800); font-weight: 500; } }
    .resultado { margin-top: 14px; animation: aparecer .3s both; }
    .metros { font-family: var(--fuente-display); font-size: 34px; font-weight: 500; margin-right: 8px; }
    .camino { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px;
      span { font-size: 11.5px; padding: 3px 8px; border-radius: 6px; background: var(--arena-100); } span:not(:last-child)::after { content: ' →'; color: var(--texto-tenue); } }
    .orden-recorrido { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 12px;
      span { font-size: 11.5px; padding: 3px 8px; border-radius: 6px; background: var(--arena-50); color: var(--texto-tenue); border: 1px solid var(--borde); transition: all .25s ease; }
      span.visitado { background: var(--tierra-700); color: #fff; border-color: var(--tierra-700); } }
    .alerta { color: var(--peligro); font-size: 13.5px; }
    .ok { display: flex; gap: 6px; align-items: center; color: var(--exito); font-size: 13.5px; }
    .bloque { padding: 10px 0; border-bottom: 1px solid var(--arena-200); display: flex; flex-direction: column; gap: 4px; &:last-child { border: none; }
      strong { font-weight: 500; } .aptos { display: flex; gap: 4px; flex-wrap: wrap; } }
    @media (max-width: 1100px) { .distribucion { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Mapa implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  readonly sesion = inject(SesionService);

  readonly mapa = signal<DatosMapa | null>(null);
  readonly modo = signal<Modo>('ruta');
  readonly algoritmo = signal<'DIJKSTRA' | 'BFS'>('DIJKSTRA');
  readonly seleccion = signal<string[]>([]);
  readonly ruta = signal<Ruta | null>(null);
  readonly inalcanzables = signal<string[]>([]);
  readonly inalcanzablesActuales = signal<string[]>([]);
  readonly bloques = signal<GrupoBloque[]>([]);
  readonly recorrido = signal<string[]>([]);
  readonly paso = signal(0);
  readonly metros = signal(false);
  private animacion?: ReturnType<typeof setInterval>;

  readonly caminoVisible = computed(() => {
    if (this.recorrido().length && this.paso() < this.recorrido().length + 1) return [];
    return this.ruta()?.camino ?? [];
  });

  /** Durante un recorrido se resaltan los nodos ya visitados; si no, la selección. */
  readonly nodosResaltados = computed(() =>
    this.recorrido().length ? this.recorrido().slice(0, this.paso()) : this.seleccion());

  readonly ayuda = computed(() => ({
    ruta: 'Selecciona dos puntos del mapa para calcular la ruta más corta entre ellos.',
    simular: 'Haz clic en un pasillo para ver qué puntos quedarían inalcanzables desde recepción si se cerrara (sin modificar el mapa).',
    editar: 'Haz clic en un pasillo para cerrarlo o volver a abrirlo. Los cierres afectan las rutas de preparación.'
  })[this.modo()]);

  ngOnInit(): void {
    this.api.mapa().subscribe(m => this.mapa.set(m));
    this.analizar();
  }

  ngOnDestroy(): void {
    clearInterval(this.animacion);
  }

  analizar(): void {
    this.api.inalcanzables().subscribe(l => this.inalcanzablesActuales.set(l));
    this.api.bloques().subscribe(b => this.bloques.set(b));
  }

  cambiarModo(m: Modo): void {
    this.modo.set(m);
    this.inalcanzables.set([]);
  }

  alClicNodo(id: string): void {
    if (this.modo() !== 'ruta') return;
    const actual = this.seleccion();
    this.seleccion.set(actual.length >= 2 || actual.includes(id) ? [id] : [...actual, id]);
    this.recorrido.set([]);
    this.calcular();
  }

  calcular(): void {
    const [a, b] = this.seleccion();
    if (!a || !b) { this.ruta.set(null); return; }
    this.api.ruta(a, b, this.algoritmo()).subscribe({ next: r => this.ruta.set(r), error: () => this.ruta.set(null) });
  }

  alClicPasillo(p: Pasillo): void {
    if (this.modo() === 'simular') {
      this.api.simularCierre(p.origen, p.destino).subscribe(l => {
        this.inalcanzables.set(l);
        if (l.length) this.avisos.aviso(`Cerrar ${p.origen} ↔ ${p.destino}`, `Quedarían inalcanzables: ${l.join(', ')}`);
        else this.avisos.info(`Cerrar ${p.origen} ↔ ${p.destino}`, 'Existen rutas alternativas: nada queda aislado.');
      });
    } else if (this.modo() === 'editar') {
      this.api.cambiarPasillo(p.origen, p.destino, !p.habilitado).subscribe(m => {
        this.mapa.set(m);
        this.avisos.exito(p.habilitado ? 'Pasillo cerrado' : 'Pasillo abierto', `${p.origen} ↔ ${p.destino}`);
        this.analizar();
        this.calcular();
      });
    }
  }

  recorrer(tipo: 'BFS' | 'DFS'): void {
    clearInterval(this.animacion);
    this.api.recorrido(this.seleccion().at(0) ?? 'RECEPCION', tipo).subscribe(orden => {
      this.recorrido.set(orden);
      this.paso.set(0);
      this.animacion = setInterval(() => {
        this.paso.update(p => p + 1);
        if (this.paso() > orden.length) clearInterval(this.animacion);
      }, 220);
    });
  }

  limpiar(): void {
    this.seleccion.set([]);
    this.ruta.set(null);
    this.recorrido.set([]);
    this.inalcanzables.set([]);
  }
}
