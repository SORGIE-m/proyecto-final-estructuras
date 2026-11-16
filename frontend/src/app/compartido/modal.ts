import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { Icono } from './icono';

/** Ventana modal con animación de entrada y cierre con Escape o clic fuera. */
@Component({
  selector: 'app-modal',
  imports: [Icono],
  template: `
    <div class="velo" (click)="cerrar.emit()"></div>
    <section class="ventana" [style.max-width.px]="ancho()" role="dialog" aria-modal="true" [attr.aria-label]="titulo()">
      <header>
        <div>
          @if (sobretitulo()) { <div class="sobretitulo">{{ sobretitulo() }}</div> }
          <h2>{{ titulo() }}</h2>
        </div>
        <button class="btn fantasma icono" type="button" (click)="cerrar.emit()" aria-label="Cerrar"><app-icono nombre="cerrar" /></button>
      </header>
      <div class="contenido"><ng-content /></div>
      <footer><ng-content select="[pie]" /></footer>
    </section>`,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; padding: 20px; }
    .velo { position: absolute; inset: 0; background: rgba(53, 44, 37, .38); backdrop-filter: blur(4px); animation: desvanecer .25s ease both; }
    .ventana { position: relative; width: 100%; max-height: calc(100vh - 40px); display: flex; flex-direction: column;
      background: var(--superficie); border-radius: var(--radio-lg); box-shadow: var(--sombra-lg); border: 1px solid var(--borde);
      animation: aparecer-escala .35s cubic-bezier(.2,.7,.2,1) both; overflow: hidden; }
    header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 24px 26px 8px; }
    h2 { font-family: var(--fuente-display); font-style: italic; font-size: 28px; font-weight: 500; color: var(--tierra-800); }
    .contenido { padding: 12px 26px 20px; overflow-y: auto; }
    footer { display: flex; justify-content: flex-end; gap: 10px; padding: 0 26px 22px; flex-wrap: wrap; }
    footer:empty { display: none; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Modal {
  readonly titulo = input('');
  readonly sobretitulo = input('');
  readonly ancho = input(640);
  readonly cerrar = output<void>();

  @HostListener('document:keydown.escape')
  alEscape(): void {
    this.cerrar.emit();
  }
}
