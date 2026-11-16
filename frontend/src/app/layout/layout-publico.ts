import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from '../core/sesion.service';
import { Icono } from '../compartido/icono';
import { Marca } from './marca';

@Component({
  selector: 'app-layout-publico',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, Marca],
  template: `
    <header class="barra" [class.compacta]="desplazado()">
      <app-marca />
      <nav>
        <a routerLink="/" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Inicio</a>
        <a routerLink="/reservar" routerLinkActive="activo">Apartamentos</a>
        @if (sesion.esHuesped()) {
          <a routerLink="/mi-cuenta" routerLinkActive="activo">Mi cuenta</a>
        }
        @if (sesion.esPersonal()) {
          <a routerLink="/panel">Panel de gestión</a>
        }
      </nav>
      <div class="acciones">
        @if (sesion.autenticado()) {
          <span class="saludo">Hola, <em>{{ primerNombre() }}</em></span>
          <button class="btn fantasma pequeno" type="button" (click)="sesion.cerrar()"><app-icono nombre="salir" [tamano]="15" /> Salir</button>
        } @else {
          <a class="btn fantasma pequeno" routerLink="/ingresar">Ingresar</a>
          <a class="btn pequeno" routerLink="/registro">Crear cuenta</a>
        }
      </div>
    </header>
    <main><router-outlet /></main>
    <footer class="pie">
      <div class="adorno-estrellas">★ ★ ★ ★ ★</div>
      <p class="cursiva">Aparta-Suites UQ · Salento, Quindío</p>
      <p class="tenue">Apartamentos completos para tu grupo, en el corazón del Paisaje Cultural Cafetero.</p>
    </footer>`,
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .barra { position: sticky; top: 0; z-index: 50; display: flex; align-items: center; justify-content: space-between; gap: 20px;
      padding: 18px clamp(16px, 4vw, 56px); transition: padding .3s ease, background .3s ease, box-shadow .3s ease;
      background: rgba(243, 235, 224, .55); backdrop-filter: blur(14px); }
    .barra.compacta { padding-top: 10px; padding-bottom: 10px; background: rgba(251, 248, 243, .88); box-shadow: 0 8px 30px -18px rgba(53,44,37,.4); }
    nav { display: flex; gap: 6px; }
    nav a { padding: 8px 16px; border-radius: 999px; font-size: 12px; text-transform: uppercase; letter-spacing: .18em; color: var(--texto-suave); transition: all .2s ease; }
    nav a:hover { color: var(--tierra-800); background: rgba(255,255,255,.5); }
    nav a.activo { color: var(--tierra-800); background: var(--superficie); box-shadow: var(--sombra-sm); }
    .acciones { display: flex; align-items: center; gap: 8px; }
    .saludo { font-size: 14px; color: var(--texto-suave); margin-right: 6px; }
    .saludo em { font-family: var(--fuente-display); font-size: 18px; color: var(--tierra-700); }
    main { flex: 1; }
    .pie { text-align: center; padding: 48px 20px 40px; border-top: 1px solid var(--borde); margin-top: 40px; }
    .pie .cursiva { font-size: 22px; margin: 8px 0 2px; }
    @media (max-width: 860px) { nav { display: none; } .saludo { display: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LayoutPublico {
  readonly sesion = inject(SesionService);
  readonly desplazado = signal(false);

  primerNombre(): string {
    return (this.sesion.sesion()?.nombre ?? '').split(' ')[0];
  }

  @HostListener('window:scroll')
  alDesplazar(): void {
    this.desplazado.set(window.scrollY > 20);
  }
}
