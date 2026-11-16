import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

/** Fotografía del apartamento con un respaldo ilustrado si la imagen no carga. */
@Component({
  selector: 'app-imagen-apto',
  template: `
    <div class="marco" [class.cargada]="cargada()">
      <img [src]="fuente()" [alt]="nombre()" loading="lazy" (load)="cargada.set(true)" (error)="fallo()" />
    </div>`,
  styles: [`
    :host { display: block; }
    .marco { position: relative; width: 100%; height: 100%; overflow: hidden; background: linear-gradient(120deg, #efe4d5, #e3d3bf); }
    .marco::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,.35), transparent);
      transform: translateX(-100%); animation: barrido 1.4s ease-in-out infinite; }
    .marco.cargada::after { display: none; }
    img { width: 100%; height: 100%; object-fit: cover; display: block; opacity: 0; transform: scale(1.04);
      transition: opacity .6s ease, transform 1.2s cubic-bezier(.2,.7,.2,1); }
    .cargada img { opacity: 1; transform: scale(1); }
    @keyframes barrido { to { transform: translateX(100%); } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImagenApartamento {
  readonly url = input<string | null>(null);
  readonly nombre = input('Apartamento');
  readonly cargada = signal(false);
  private readonly fallida = signal(false);

  readonly fuente = computed(() => (this.fallida() || !this.url()) ? this.respaldo() : this.url()!);

  fallo(): void {
    if (!this.fallida()) {
      this.fallida.set(true);
    }
  }

  /** Ilustración cálida generada en SVG (montañas y palmas de cera del Quindío). */
  private respaldo(): string {
    const inicial = (this.nombre() || 'A').trim().charAt(0).toUpperCase();
    const svg = `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 260'>
      <defs><linearGradient id='c' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='#f4e9db'/><stop offset='1' stop-color='#e2cfb7'/></linearGradient></defs>
      <rect width='400' height='260' fill='url(#c)'/>
      <circle cx='300' cy='70' r='26' fill='#f8efe2'/>
      <path d='M0 190 L90 110 L150 160 L230 80 L320 150 L400 100 L400 260 L0 260Z' fill='#cdb497' opacity='.75'/>
      <path d='M0 215 L120 150 L210 200 L300 140 L400 190 L400 260 L0 260Z' fill='#b39677' opacity='.8'/>
      <path d='M70 230 C72 190 74 160 80 120' stroke='#6b5b4d' stroke-width='3' fill='none'/>
      <path d='M80 120 c-14 -10 -30 -8 -40 2 M80 120 c10 -14 28 -16 40 -6 M80 120 c-4 -16 4 -28 16 -32' stroke='#6b5b4d' stroke-width='3' fill='none' stroke-linecap='round'/>
      <text x='200' y='145' text-anchor='middle' font-family='Georgia,serif' font-style='italic' font-size='64' fill='#6b5b4d' opacity='.55'>${inicial}</text>
    </svg>`;
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
  }
}
