import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavegacionService } from './core/navegacion.service';
import { CapaGlobal } from './compartido/capa-global';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CapaGlobal],
  template: `<router-outlet /><app-capa-global />`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class App {
  // se instancia al arrancar para registrar todo el historial de navegación en la pila
  private readonly navegacion = inject(NavegacionService);
}
