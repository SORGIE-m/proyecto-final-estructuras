import { Routes } from '@angular/router';
import { rolPermitido, soloInvitados } from './core/guardas';
import { LayoutPanel } from './layout/layout-panel';
import { LayoutPublico } from './layout/layout-publico';

export const routes: Routes = [
  {
    path: '',
    component: LayoutPublico,
    children: [
      { path: '', title: 'Aparta-Suites UQ · Salento', loadComponent: () => import('./paginas/inicio/inicio').then(m => m.Inicio) },
      { path: 'reservar', title: 'Disponibilidad · Aparta-Suites UQ', loadComponent: () => import('./paginas/reservar/reservar').then(m => m.Reservar) },
      { path: 'ingresar', title: 'Ingresar · Aparta-Suites UQ', canActivate: [soloInvitados], loadComponent: () => import('./paginas/ingresar/ingresar').then(m => m.Ingresar) },
      { path: 'registro', title: 'Crear cuenta · Aparta-Suites UQ', canActivate: [soloInvitados], data: { registro: true }, loadComponent: () => import('./paginas/ingresar/ingresar').then(m => m.Ingresar) },
      { path: 'mi-cuenta', title: 'Mi cuenta · Aparta-Suites UQ', canActivate: [rolPermitido('HUESPED')], loadComponent: () => import('./paginas/mi-cuenta/mi-cuenta').then(m => m.MiCuenta) }
    ]
  },
  {
    path: 'panel',
    component: LayoutPanel,
    canActivate: [rolPermitido('RECEPCIONISTA', 'ADMINISTRADOR')],
    children: [
      { path: '', title: 'Inicio · SGA', loadComponent: () => import('./paginas/panel/tablero/tablero').then(m => m.Tablero) },
      { path: 'reservas', title: 'Reservas · SGA', loadComponent: () => import('./paginas/panel/reservas/reservas').then(m => m.Reservas) },
      { path: 'calendario', title: 'Calendario · SGA', loadComponent: () => import('./paginas/panel/calendario/calendario').then(m => m.Calendario) },
      { path: 'preparacion', title: 'Preparación · SGA', loadComponent: () => import('./paginas/panel/preparacion/preparacion').then(m => m.Preparacion) },
      { path: 'mapa', title: 'Mapa del edificio · SGA', loadComponent: () => import('./paginas/panel/mapa/mapa').then(m => m.Mapa) },
      { path: 'canal-externo', title: 'Canal externo · SGA', loadComponent: () => import('./paginas/panel/canal-externo/canal-externo').then(m => m.CanalExterno) },
      { path: 'estadisticas', title: 'Estadísticas · SGA', canActivate: [rolPermitido('ADMINISTRADOR')], loadComponent: () => import('./paginas/panel/estadisticas/estadisticas').then(m => m.Estadisticas) },
      { path: 'administracion', title: 'Administración · SGA', canActivate: [rolPermitido('ADMINISTRADOR')], loadComponent: () => import('./paginas/panel/administracion/administracion').then(m => m.Administracion) }
    ]
  },
  { path: '**', redirectTo: '' }
];
