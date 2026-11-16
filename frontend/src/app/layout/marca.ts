import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-marca',
  imports: [RouterLink],
  template: `
    <a [routerLink]="destino()" class="marca" [class.clara]="clara()">
      <svg viewBox="0 0 64 64" width="34" height="34" aria-hidden="true">
        <rect width="64" height="64" rx="18" fill="currentColor" opacity=".1"/>
        <path d="M14 44V27l18-12 18 12v17" fill="none" stroke="currentColor" stroke-width="3" stroke-linejoin="round"/>
        <path d="M26 44V33h12v11" fill="none" stroke="#b8956a" stroke-width="3" stroke-linejoin="round"/>
      </svg>
      <span>
        <strong>Aparta-Suites</strong>
        <em>UQ · Salento</em>
      </span>
    </a>`,
  styles: [`
    .marca { display: inline-flex; align-items: center; gap: 12px; color: var(--tierra-800); }
    .marca.clara { color: #fbf6ef; }
    span { display: flex; flex-direction: column; line-height: 1.05; }
    strong { font-weight: 400; text-transform: uppercase; letter-spacing: .26em; font-size: 13px; }
    em { font-family: var(--fuente-display); font-size: 16px; color: var(--tierra-600); letter-spacing: .02em; }
    .clara em { color: var(--acento-claro); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Marca {
  readonly destino = input('/');
  readonly clara = input(false);
}
