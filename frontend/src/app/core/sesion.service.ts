import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { Rol, Sesion } from './modelos';

const CLAVE = 'sga.sesion';

/** Estado de la sesión del usuario (señales) con persistencia en el navegador. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly _sesion = signal<Sesion | null>(this.leer());

  readonly sesion = this._sesion.asReadonly();
  readonly autenticado = computed(() => this._sesion() !== null);
  readonly rol = computed<Rol | null>(() => this._sesion()?.rol ?? null);
  readonly esPersonal = computed(() => this.rol() === 'RECEPCIONISTA' || this.rol() === 'ADMINISTRADOR');
  readonly esAdmin = computed(() => this.rol() === 'ADMINISTRADOR');
  readonly esHuesped = computed(() => this.rol() === 'HUESPED');
  readonly token = computed(() => this._sesion()?.token ?? null);

  private leer(): Sesion | null {
    try {
      const crudo = localStorage.getItem(CLAVE);
      if (!crudo) return null;
      const s = JSON.parse(crudo) as Sesion;
      return new Date(s.expira) > new Date() ? s : null;
    } catch {
      return null;
    }
  }

  establecer(s: Sesion): void {
    this._sesion.set(s);
    try { localStorage.setItem(CLAVE, JSON.stringify(s)); } catch { /* almacenamiento no disponible */ }
  }

  iniciar(usuario: string, clave: string): Observable<Sesion> {
    return this.api.login(usuario, clave).pipe(tap(s => this.establecer(s)));
  }

  registrar(datos: unknown): Observable<Sesion> {
    return this.api.registro(datos).pipe(tap(s => this.establecer(s)));
  }

  /** Ruta de inicio según el rol. */
  inicio(): string {
    switch (this.rol()) {
      case 'HUESPED': return '/mi-cuenta';
      case 'RECEPCIONISTA':
      case 'ADMINISTRADOR': return '/panel';
      default: return '/';
    }
  }

  cerrar(redirigir = true): void {
    if (this._sesion()) {
      this.api.logout().subscribe({ error: () => undefined });
    }
    this.limpiar();
    if (redirigir) this.router.navigateByUrl('/');
  }

  limpiar(): void {
    this._sesion.set(null);
    try { localStorage.removeItem(CLAVE); } catch { /* ignorado */ }
  }
}
