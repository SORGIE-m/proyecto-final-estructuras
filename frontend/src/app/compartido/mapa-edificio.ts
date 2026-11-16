import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Mapa, NodoMapa, Pasillo } from '../core/modelos';

interface Segmento { a: NodoMapa; b: NodoMapa; pasillo: Pasillo; enRuta: boolean; }
interface Bloque { nombre: string; x: number; y: number; ancho: number; alto: number; }

/**
 * Representación gráfica del grafo del edificio: nodos = apartamentos y puntos de servicio,
 * aristas = pasillos. Colorea cada apartamento según su estado y anima la ruta calculada.
 */
@Component({
  selector: 'app-mapa-edificio',
  template: `
    <svg viewBox="0 0 1000 650" class="lienzo" role="img" aria-label="Mapa del edificio">
      <defs>
        <pattern id="puntos" width="22" height="22" patternUnits="userSpaceOnUse"><circle cx="1.5" cy="1.5" r="1.1" fill="#e2d4c2"/></pattern>
        <filter id="sombra" x="-20%" y="-20%" width="140%" height="160%"><feDropShadow dx="0" dy="3" stdDeviation="4" flood-color="#4e4238" flood-opacity=".18"/></filter>
      </defs>
      <rect width="1000" height="650" fill="url(#puntos)" />

      @for (b of bloques(); track b.nombre) {
        <g class="bloque">
          <rect [attr.x]="b.x" [attr.y]="b.y" [attr.width]="b.ancho" [attr.height]="b.alto" rx="22" />
          <text [attr.x]="b.x + 18" [attr.y]="b.y + 28">Bloque {{ b.nombre }}</text>
        </g>
      }
      @for (p of pisos(); track p.piso) {
        <text class="piso" x="14" [attr.y]="p.y + 4">P{{ p.piso }}</text>
      }

      @for (s of segmentos(); track s.a.id + s.b.id) {
        <g class="pasillo" [class.cerrado]="!s.pasillo.habilitado" [class.ruta]="s.enRuta" [class.editable]="editable()"
           (click)="pasilloClic.emit(s.pasillo)">
          <line class="toque" [attr.x1]="s.a.x" [attr.y1]="s.a.y" [attr.x2]="s.b.x" [attr.y2]="s.b.y" />
          <line class="trazo" [attr.x1]="s.a.x" [attr.y1]="s.a.y" [attr.x2]="s.b.x" [attr.y2]="s.b.y" />
          @if (mostrarMetros()) {
            <text class="metros" [attr.x]="(s.a.x + s.b.x) / 2" [attr.y]="(s.a.y + s.b.y) / 2 - 6">{{ s.pasillo.metros }} m</text>
          }
          <title>{{ s.a.nombre }} ↔ {{ s.b.nombre }} · {{ s.pasillo.metros }} m{{ s.pasillo.habilitado ? '' : ' (cerrado)' }}</title>
        </g>
      }

      @if (trayecto(); as t) {
        <path class="trayecto" [attr.d]="t" />
        <circle r="8" class="viajero">
          <animateMotion [attr.dur]="duracion()" repeatCount="indefinite" [attr.path]="t" rotate="auto" />
        </circle>
      }

      @for (n of mapa()?.nodos ?? []; track n.id) {
        <g class="nodo" [attr.transform]="'translate(' + n.x + ',' + n.y + ')'" [class]="claseNodo(n)" (click)="nodoClic.emit(n.id)">
          @switch (n.tipo) {
            @case ('APARTAMENTO') {
              <rect x="-36" y="-22" width="72" height="44" rx="12" filter="url(#sombra)" />
              <text class="codigo" y="5">{{ n.id }}</text>
            }
            @case ('RECEPCION') {
              <rect x="-62" y="-20" width="124" height="40" rx="20" filter="url(#sombra)" />
              <text class="codigo claro" y="5">RECEPCIÓN</text>
            }
            @case ('CUARTO_ASEO') {
              <rect x="-30" y="-17" width="60" height="34" rx="9" filter="url(#sombra)" />
              <text class="mini" y="4">ASEO</text>
            }
            @case ('ESCALERA') {
              <circle r="15" filter="url(#sombra)" />
              <path d="M-7 6h4v-4h4v-4h4v-4h3" class="escalon" />
            }
            @default {
              <circle r="8" />
            }
          }
          @if (paradas().includes(n.id)) {
            <g class="orden" transform="translate(30,-24)"><circle r="11" /><text y="4">{{ paradas().indexOf(n.id) + 1 }}</text></g>
          }
          <title>{{ n.nombre }}{{ n.estadoApartamento ? ' · ' + etiqueta(n.estadoApartamento) : '' }}</title>
        </g>
      }
    </svg>`,
  styles: [`
    :host { display: block; }
    .lienzo { width: 100%; height: auto; display: block; border-radius: var(--radio); background: #fbf7f1; }
    .bloque rect { fill: rgba(236, 224, 208, .45); stroke: #e0cfb9; stroke-dasharray: 4 6; }
    .bloque text { font-size: 12px; letter-spacing: .24em; text-transform: uppercase; fill: #a3927f; font-family: var(--fuente); }
    .piso { font-size: 11px; fill: #b8a690; letter-spacing: .1em; font-family: var(--fuente); }
    .pasillo .trazo { stroke: #c9b397; stroke-width: 3.5; stroke-linecap: round; transition: stroke .3s ease, stroke-width .3s ease; }
    .pasillo .toque { stroke: transparent; stroke-width: 16; }
    .pasillo.editable { cursor: pointer; }
    .pasillo.editable:hover .trazo { stroke: #8a705a; stroke-width: 5; }
    .pasillo.cerrado .trazo { stroke: #b65b4b; stroke-dasharray: 3 7; opacity: .8; }
    .pasillo.ruta .trazo { stroke: #6b5b4d; stroke-width: 5; }
    .metros { font-size: 10px; fill: #a3927f; text-anchor: middle; font-family: var(--fuente); paint-order: stroke; stroke: #fbf7f1; stroke-width: 3; }
    .trayecto { fill: none; stroke: #d8a24a; stroke-width: 4; stroke-linecap: round; stroke-linejoin: round; stroke-dasharray: 10 12;
      animation: avanzar 1.2s linear infinite; opacity: .95; }
    .viajero { fill: #fff; stroke: #b8862f; stroke-width: 4; }
    .nodo { cursor: pointer; transition: transform .2s ease; }
    .nodo rect, .nodo circle { fill: #fdfaf6; stroke: #cdb89d; stroke-width: 1.5; transition: fill .3s ease, stroke .3s ease; }
    .nodo .codigo { font-size: 13px; font-weight: 500; text-anchor: middle; letter-spacing: .08em; fill: #fff; font-family: var(--fuente); pointer-events: none; }
    .nodo .mini { font-size: 9.5px; text-anchor: middle; letter-spacing: .16em; fill: #7d6d5f; font-family: var(--fuente); }
    .nodo .escalon { fill: none; stroke: #8a705a; stroke-width: 1.8; }
    .nodo:hover rect, .nodo:hover circle { stroke: #6b5b4d; stroke-width: 2.5; }
    .RECEPCION rect { fill: #6b5b4d; stroke: #4e4238; }
    .PREPARADO rect { fill: #6f9a6a; stroke: #5a8256; }
    .OCUPADO rect { fill: #5f86a6; stroke: #4c7090; }
    .PENDIENTE_PREPARACION rect, .EN_PREPARACION rect { fill: #d1a03f; stroke: #b4862c; }
    .EN_PREPARACION rect { animation: latido 1.6s ease-in-out infinite; }
    .FUERA_SERVICIO rect { fill: #b65b4b; stroke: #9a4a3c; }
    .seleccionado rect, .seleccionado circle { stroke: #352c25 !important; stroke-width: 3.5 !important; }
    .inalcanzable { opacity: .35; }
    .inalcanzable rect { stroke: #b65b4b !important; stroke-dasharray: 4 3; }
    .en-ruta circle { stroke: #b8862f; stroke-width: 2.5; }
    .orden circle { fill: #352c25; stroke: #fdfaf6; stroke-width: 2; }
    .orden text { fill: #fff; font-size: 11px; text-anchor: middle; font-weight: 600; font-family: var(--fuente); }
    @keyframes avanzar { to { stroke-dashoffset: -44; } }
    @keyframes latido { 0%, 100% { opacity: 1; } 50% { opacity: .65; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MapaEdificio {
  readonly mapa = input<Mapa | null>(null);
  readonly ruta = input<string[]>([]);
  readonly paradas = input<string[]>([]);
  readonly inalcanzables = input<string[]>([]);
  readonly seleccionados = input<string[]>([]);
  readonly editable = input(false);
  readonly mostrarMetros = input(false);
  readonly nodoClic = output<string>();
  readonly pasilloClic = output<Pasillo>();

  private readonly porId = computed(() => new Map((this.mapa()?.nodos ?? []).map(n => [n.id, n])));

  readonly segmentos = computed<Segmento[]>(() => {
    const ids = this.porId();
    const ruta = this.ruta();
    const enRuta = new Set<string>();
    for (let i = 1; i < ruta.length; i++) {
      enRuta.add(ruta[i - 1] + '|' + ruta[i]);
      enRuta.add(ruta[i] + '|' + ruta[i - 1]);
    }
    return (this.mapa()?.pasillos ?? [])
      .filter(p => ids.has(p.origen) && ids.has(p.destino))
      .map(p => ({ a: ids.get(p.origen)!, b: ids.get(p.destino)!, pasillo: p, enRuta: enRuta.has(p.origen + '|' + p.destino) }));
  });

  readonly bloques = computed<Bloque[]>(() => {
    const grupos = new Map<string, NodoMapa[]>();
    for (const n of this.mapa()?.nodos ?? []) {
      if (n.bloque && n.tipo !== 'CUARTO_ASEO') {
        grupos.set(n.bloque, [...(grupos.get(n.bloque) ?? []), n]);
      }
    }
    return [...grupos.entries()].map(([nombre, ns]) => {
      const xs = ns.map(n => n.x);
      const ys = ns.map(n => n.y);
      const x = Math.min(...xs) - 56;
      const y = Math.min(...ys) - 70;
      return { nombre, x, y, ancho: Math.max(...xs) + 56 - x, alto: Math.max(...ys) + 40 - y };
    });
  });

  readonly pisos = computed(() => {
    const vistos = new Map<number, number>();
    for (const n of this.mapa()?.nodos ?? []) {
      if (n.tipo === 'ESCALERA' && !vistos.has(n.piso)) vistos.set(n.piso, n.y);
    }
    return [...vistos.entries()].map(([piso, y]) => ({ piso, y }));
  });

  readonly trayecto = computed(() => {
    const ids = this.porId();
    const puntos = this.ruta().map(id => ids.get(id)).filter((n): n is NodoMapa => !!n);
    if (puntos.length < 2) return null;
    return puntos.map((n, i) => `${i === 0 ? 'M' : 'L'} ${n.x} ${n.y}`).join(' ');
  });

  readonly duracion = computed(() => `${Math.max(3, this.ruta().length * 0.7).toFixed(1)}s`);

  claseNodo(n: NodoMapa): string {
    const clases = ['nodo', n.tipo, n.estadoApartamento ?? ''];
    if (this.seleccionados().includes(n.id)) clases.push('seleccionado');
    if (this.inalcanzables().includes(n.id)) clases.push('inalcanzable');
    if (this.ruta().includes(n.id)) clases.push('en-ruta');
    return clases.join(' ');
  }

  etiqueta(e: string): string {
    return ({ PREPARADO: 'Preparado', OCUPADO: 'Ocupado', PENDIENTE_PREPARACION: 'Pendiente de preparación',
      EN_PREPARACION: 'En preparación', FUERA_SERVICIO: 'Fuera de servicio' } as Record<string, string>)[e] ?? e;
  }
}
