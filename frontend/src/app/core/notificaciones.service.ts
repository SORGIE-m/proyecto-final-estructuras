import { Injectable, signal } from '@angular/core';

export type TipoAviso = 'exito' | 'error' | 'info' | 'aviso';

export interface Aviso {
  id: number;
  tipo: TipoAviso;
  titulo: string;
  mensaje?: string;
}

/** Avisos flotantes (toasts) de la aplicación. */
@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private secuencia = 0;
  readonly avisos = signal<Aviso[]>([]);

  mostrar(tipo: TipoAviso, titulo: string, mensaje?: string, duracion = 4200): void {
    const aviso: Aviso = { id: ++this.secuencia, tipo, titulo, mensaje };
    this.avisos.update(l => [...l.slice(-3), aviso]);
    setTimeout(() => this.cerrar(aviso.id), duracion);
  }

  exito(titulo: string, mensaje?: string) { this.mostrar('exito', titulo, mensaje); }
  error(titulo: string, mensaje?: string) { this.mostrar('error', titulo, mensaje, 6000); }
  info(titulo: string, mensaje?: string) { this.mostrar('info', titulo, mensaje); }
  aviso(titulo: string, mensaje?: string) { this.mostrar('aviso', titulo, mensaje); }

  cerrar(id: number): void {
    this.avisos.update(l => l.filter(a => a.id !== id));
  }
}
