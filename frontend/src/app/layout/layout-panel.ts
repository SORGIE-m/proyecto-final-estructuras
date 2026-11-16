import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Turno } from '../core/modelos';
import { NavegacionService } from '../core/navegacion.service';
import { SesionService } from '../core/sesion.service';
import { Icono } from '../compartido/icono';
import { Marca } from './marca';

interface Enlace { ruta: string; texto: string; icono: string; soloAdmin?: boolean; exacto?: boolean; }

@Component({
  selector: 'app-layout-panel',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, Marca],
  template: `
    <aside class="lateral" [class.abierta]="menuAbierto()">
      <app-marca destino="/panel" [clara]="true" />
      <div class="rol">{{ sesion.esAdmin() ? 'Administración' : 'Recepción' }}</div>
      <nav>
        @for (e of enlaces(); track e.ruta) {
          <a [routerLink]="e.ruta" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: !!e.exacto }" (click)="menuAbierto.set(false)">
            <app-icono [nombre]="e.icono" [tamano]="18" />
            <span>{{ e.texto }}</span>
          </a>
        }
      </nav>
      @if (turno(); as t) {
        <div class="turno">
          <div class="sobretitulo">Turno de recepción</div>
          <strong>{{ t.actual.nombre }}</strong>
          <span>{{ t.franja }}</span>
          <button type="button" (click)="rotar()" title="Pasar el turno al siguiente recepcionista">
            <app-icono nombre="recargar" [tamano]="14" /> Rotar turno
          </button>
          @if (t.siguientes.length) {
            <small>Sigue: {{ t.siguientes[0].nombre }}</small>
          }
        </div>
      }
      <a class="sitio" routerLink="/"><app-icono nombre="puerta" [tamano]="16" /> Ver sitio de huéspedes</a>
    </aside>
    <div class="cuerpo">
      <header class="superior">
        <div class="fila">
          <button class="btn fantasma icono menu" type="button" (click)="menuAbierto.set(!menuAbierto())" aria-label="Menú">
            <app-icono nombre="menu" />
          </button>
          <button class="btn secundario pequeno" type="button" [disabled]="!navegacion.puedeVolver()" (click)="navegacion.volver()">
            <app-icono nombre="atras" [tamano]="15" /> Atrás
          </button>
        </div>
        <div class="fila">
          <div class="usuario">
            <span class="avatar">{{ iniciales() }}</span>
            <span><strong>{{ sesion.sesion()?.nombre }}</strong><small>{{ sesion.esAdmin() ? 'Administrador' : 'Recepcionista' }}</small></span>
          </div>
          <button class="btn fantasma pequeno" type="button" (click)="sesion.cerrar()"><app-icono nombre="salir" [tamano]="15" /> Salir</button>
        </div>
      </header>
      <main><router-outlet /></main>
    </div>
    @if (menuAbierto()) { <div class="velo" (click)="menuAbierto.set(false)"></div> }`,
  styles: [`
    :host { display: flex; min-height: 100vh; }
    .lateral { position: sticky; top: 0; height: 100vh; width: 260px; flex-shrink: 0; display: flex; flex-direction: column; gap: 18px;
      padding: 26px 18px; color: #f2e8db; overflow-y: auto;
      background: linear-gradient(180deg, #5d4f43 0%, #4a3f36 55%, #3f352d 100%); }
    .rol { font-size: 10.5px; text-transform: uppercase; letter-spacing: .3em; color: #c9b498; padding-left: 4px; }
    nav { display: flex; flex-direction: column; gap: 2px; }
    nav a { display: flex; align-items: center; gap: 12px; padding: 11px 14px; border-radius: 12px; color: #e3d5c3; font-size: 14px;
      transition: background .2s ease, color .2s ease, transform .2s ease; }
    nav a:hover { background: rgba(255,255,255,.07); color: #fff; transform: translateX(2px); }
    nav a.activo { background: rgba(251, 246, 239, .14); color: #fff; box-shadow: inset 3px 0 0 #d8bb93; }
    .turno { margin-top: auto; padding: 16px; border-radius: 14px; background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.08);
      display: flex; flex-direction: column; gap: 3px; }
    .turno .sobretitulo { color: #bfa98c; font-size: 10px; }
    .turno strong { font-weight: 500; color: #fff; }
    .turno span, .turno small { font-size: 12.5px; color: #cdbba4; }
    .turno button { margin-top: 8px; align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; border: 1px solid rgba(255,255,255,.2);
      background: transparent; color: #f2e8db; font-family: var(--fuente); font-size: 11px; letter-spacing: .12em; text-transform: uppercase;
      padding: 6px 12px; border-radius: 999px; cursor: pointer; transition: background .2s ease; }
    .turno button:hover { background: rgba(255,255,255,.1); }
    .sitio { display: flex; align-items: center; gap: 8px; color: #cdbba4; font-size: 13px; padding: 4px; }
    .sitio:hover { color: #fff; }
    .cuerpo { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .superior { position: sticky; top: 0; z-index: 40; display: flex; align-items: center; justify-content: space-between; gap: 12px;
      padding: 14px clamp(16px, 3vw, 40px); background: rgba(243, 235, 224, .78); backdrop-filter: blur(12px); border-bottom: 1px solid var(--borde); }
    .usuario { display: flex; align-items: center; gap: 10px; }
    .usuario strong { display: block; font-weight: 500; font-size: 14px; line-height: 1.2; }
    .usuario small { color: var(--texto-tenue); font-size: 12px; }
    .avatar { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; background: var(--acento-claro);
      color: var(--tierra-800); font-weight: 500; font-size: 13px; letter-spacing: .05em; }
    .menu { display: none; }
    .velo { display: none; }
    @media (max-width: 980px) {
      .lateral { position: fixed; left: 0; z-index: 60; transform: translateX(-100%); transition: transform .3s cubic-bezier(.2,.7,.2,1); }
      .lateral.abierta { transform: none; box-shadow: var(--sombra-lg); }
      .menu { display: inline-flex; }
      .velo { display: block; position: fixed; inset: 0; z-index: 55; background: rgba(53,44,37,.3); }
      .usuario span:last-child { display: none; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LayoutPanel implements OnInit {
  readonly sesion = inject(SesionService);
  readonly navegacion = inject(NavegacionService);
  private readonly api = inject(ApiService);
  readonly menuAbierto = signal(false);
  readonly turno = signal<Turno | null>(null);

  private readonly todos: Enlace[] = [
    { ruta: '/panel', texto: 'Inicio', icono: 'inicio', exacto: true },
    { ruta: '/panel/reservas', texto: 'Reservas', icono: 'llave' },
    { ruta: '/panel/calendario', texto: 'Calendario', icono: 'calendario' },
    { ruta: '/panel/preparacion', texto: 'Preparación', icono: 'escoba' },
    { ruta: '/panel/mapa', texto: 'Mapa del edificio', icono: 'mapa' },
    { ruta: '/panel/canal-externo', texto: 'Canal externo', icono: 'bandeja' },
    { ruta: '/panel/estadisticas', texto: 'Estadísticas', icono: 'grafico', soloAdmin: true },
    { ruta: '/panel/administracion', texto: 'Administración', icono: 'ajustes', soloAdmin: true }
  ];
  readonly enlaces = computed(() => this.todos.filter(e => !e.soloAdmin || this.sesion.esAdmin()));

  ngOnInit(): void {
    this.api.turno().subscribe({ next: t => this.turno.set(t), error: () => this.turno.set(null) });
  }

  rotar(): void {
    this.api.rotarTurno().subscribe(t => this.turno.set(t));
  }

  iniciales(): string {
    return (this.sesion.sesion()?.nombre ?? '?').split(' ').slice(0, 2).map(p => p.charAt(0)).join('').toUpperCase();
  }
}
