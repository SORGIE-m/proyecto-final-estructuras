import { Injectable, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { Pila } from './pila';

/** Historial de pantallas con una Pila: permite el botón "atrás" propio de la aplicación. */
@Injectable({ providedIn: 'root' })
export class NavegacionService {
  private readonly router = inject(Router);
  private readonly historial = new Pila<string>();
  private regresando = false;
  readonly puedeVolver = signal(false);

  constructor() {
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(e => {
      const url = e.urlAfterRedirects;
      if (!this.regresando && this.historial.verCima() !== url) {
        this.historial.apilar(url);
      }
      this.regresando = false;
      this.puedeVolver.set(this.historial.tamanio > 1);
    });
  }

  volver(): void {
    if (this.historial.tamanio <= 1) return;
    this.historial.desapilar(); // pantalla actual
    const anterior = this.historial.verCima();
    if (anterior) {
      this.regresando = true;
      this.router.navigateByUrl(anterior);
    }
  }

  reiniciar(): void {
    this.historial.vaciar();
    this.puedeVolver.set(false);
  }
}
