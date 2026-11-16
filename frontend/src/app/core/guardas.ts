import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './modelos';
import { SesionService } from './sesion.service';

/** Permite el acceso solo a los roles indicados; si no hay sesión, envía al ingreso. */
export function rolPermitido(...roles: Rol[]): CanActivateFn {
  return (_ruta, estado) => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    if (!sesion.autenticado()) {
      return router.createUrlTree(['/ingresar'], { queryParams: { volver: estado.url } });
    }
    return roles.includes(sesion.rol()!) ? true : router.createUrlTree([sesion.inicio()]);
  };
}

export const soloInvitados: CanActivateFn = () => {
  const sesion = inject(SesionService);
  return sesion.autenticado() ? inject(Router).createUrlTree([sesion.inicio()]) : true;
};
