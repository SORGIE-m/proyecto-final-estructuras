import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ErrorApi } from './modelos';
import { NotificacionesService } from './notificaciones.service';
import { SesionService } from './sesion.service';

/** Agrega el token y traduce los errores de la API a avisos legibles. */
export const interceptorApi: HttpInterceptorFn = (req, next) => {
  const sesion = inject(SesionService);
  const avisos = inject(NotificacionesService);
  const router = inject(Router);
  const token = sesion.token();
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((e: HttpErrorResponse) => {
      const cuerpo = e.error as ErrorApi | null;
      const mensaje = cuerpo?.mensaje ?? (e.status === 0
        ? 'No hay conexión con el servidor. Verifique que el backend esté en ejecución.'
        : 'Ocurrió un error inesperado.');
      if (e.status === 401 && !req.url.endsWith('/auth/login')) {
        sesion.limpiar();
        avisos.aviso('Sesión finalizada', mensaje);
        router.navigate(['/ingresar'], { queryParams: { volver: router.url } });
      } else if (!req.headers.has('X-Silencioso')) {
        avisos.error(titulo(e.status), mensaje);
      }
      return throwError(() => e);
    })
  );
};

function titulo(estado: number): string {
  switch (estado) {
    case 400: return 'Datos no válidos';
    case 403: return 'Acceso restringido';
    case 404: return 'No encontrado';
    case 409: return 'Conflicto';
    case 422: return 'No se pudo completar';
    case 0: return 'Sin conexión';
    default: return 'Error';
  }
}
