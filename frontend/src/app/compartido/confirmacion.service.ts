import { Injectable, signal } from '@angular/core';

export interface PeticionConfirmacion {
  titulo: string;
  mensaje: string;
  aceptar: string;
  peligro: boolean;
  resolver: (ok: boolean) => void;
}

/** Diálogo de confirmación con promesa (reemplaza window.confirm con el estilo de la app). */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  readonly actual = signal<PeticionConfirmacion | null>(null);

  confirmar(titulo: string, mensaje: string, aceptar = 'Confirmar', peligro = false): Promise<boolean> {
    return new Promise(resolver => this.actual.set({ titulo, mensaje, aceptar, peligro, resolver }));
  }

  responder(ok: boolean): void {
    const p = this.actual();
    this.actual.set(null);
    p?.resolver(ok);
  }
}
