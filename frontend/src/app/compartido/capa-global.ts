import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificacionesService } from '../core/notificaciones.service';
import { ConfirmacionService } from './confirmacion.service';
import { Icono } from './icono';
import { Modal } from './modal';

/** Avisos flotantes y diálogo de confirmación globales. */
@Component({
  selector: 'app-capa-global',
  imports: [Icono, Modal],
  template: `
    <div class="avisos" aria-live="polite">
      @for (a of avisos.avisos(); track a.id) {
        <div class="toast" [class]="a.tipo">
          <span class="marca"><app-icono [nombre]="icono(a.tipo)" [tamano]="16" /></span>
          <div class="crece">
            <strong>{{ a.titulo }}</strong>
            @if (a.mensaje) { <p>{{ a.mensaje }}</p> }
          </div>
          <button type="button" (click)="avisos.cerrar(a.id)" aria-label="Cerrar aviso"><app-icono nombre="cerrar" [tamano]="14" /></button>
        </div>
      }
    </div>
    @if (confirmacion.actual(); as p) {
      <app-modal [titulo]="p.titulo" [ancho]="460" (cerrar)="confirmacion.responder(false)">
        <p class="suave">{{ p.mensaje }}</p>
        <ng-container pie>
          <button class="btn secundario" type="button" (click)="confirmacion.responder(false)">Volver</button>
          <button class="btn" [class.peligro]="p.peligro" type="button" (click)="confirmacion.responder(true)">{{ p.aceptar }}</button>
        </ng-container>
      </app-modal>
    }`,
  styles: [`
    .avisos { position: fixed; right: 20px; bottom: 20px; z-index: 200; display: flex; flex-direction: column; gap: 10px; width: min(380px, calc(100vw - 40px)); }
    .toast { display: flex; gap: 12px; align-items: flex-start; padding: 14px 14px 14px 16px; border-radius: 14px; background: var(--superficie);
      border: 1px solid var(--borde); box-shadow: var(--sombra); animation: entrar .4s cubic-bezier(.2,.7,.2,1) both; }
    .toast strong { font-weight: 500; font-size: 14px; color: var(--tierra-800); }
    .toast p { margin: 2px 0 0; font-size: 13px; color: var(--texto-suave); }
    .marca { width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0; color: #fff; }
    .exito .marca { background: var(--exito); } .error .marca { background: var(--peligro); }
    .info .marca { background: var(--info); } .toast.aviso .marca { background: var(--aviso); }
    .toast button { border: none; background: transparent; color: var(--texto-tenue); cursor: pointer; padding: 4px; border-radius: 6px; }
    .toast button:hover { background: var(--arena-100); }
    @keyframes entrar { from { opacity: 0; transform: translateX(30px); } to { opacity: 1; transform: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CapaGlobal {
  readonly avisos = inject(NotificacionesService);
  readonly confirmacion = inject(ConfirmacionService);

  icono(tipo: string): string {
    return tipo === 'exito' ? 'check' : tipo === 'error' ? 'cerrar' : tipo === 'aviso' ? 'alerta' : 'info';
  }
}
