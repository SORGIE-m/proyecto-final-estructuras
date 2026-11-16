import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NotificacionesService } from '../../core/notificaciones.service';
import { SesionService } from '../../core/sesion.service';
import { Icono } from '../../compartido/icono';
import { ImagenApartamento } from '../../compartido/imagen-apartamento';

/** Ingreso y registro de huéspedes. */
@Component({
  selector: 'app-ingresar',
  imports: [FormsModule, RouterLink, Icono, ImagenApartamento],
  templateUrl: './ingresar.html',
  styleUrl: './ingresar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Ingresar {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly avisos = inject(NotificacionesService);

  readonly modoRegistro = signal(this.ruta.snapshot.data['registro'] === true);
  readonly enviando = signal(false);
  readonly verClave = signal(false);

  usuario = '';
  clave = '';
  registro = { documento: '', nombre: '', fechaNacimiento: '', telefono: '', correo: '', clave: '' };

  readonly demo = [
    { etiqueta: 'Administradora', usuario: 'admin', clave: 'Admin123*', icono: 'ajustes' },
    { etiqueta: 'Recepción', usuario: 'recepcion', clave: 'Recep123*', icono: 'llave' },
    { etiqueta: 'Huésped', usuario: 'laura.gomez@correo.com', clave: 'Huesped123*', icono: 'usuario' }
  ];

  usarDemo(d: { usuario: string; clave: string }): void {
    this.usuario = d.usuario;
    this.clave = d.clave;
    this.ingresar();
  }

  ingresar(): void {
    if (!this.usuario || !this.clave) {
      this.avisos.aviso('Faltan datos', 'Escriba su usuario y contraseña.');
      return;
    }
    this.enviando.set(true);
    this.sesion.iniciar(this.usuario.trim(), this.clave).subscribe({
      next: s => {
        this.avisos.exito('Bienvenido', s.nombre);
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver && volver !== '/ingresar' ? volver : this.sesion.inicio());
      },
      error: () => this.enviando.set(false)
    });
  }

  registrarse(): void {
    const r = this.registro;
    if (!r.documento || !r.nombre || !r.fechaNacimiento || !r.telefono || !r.correo || r.clave.length < 8) {
      this.avisos.aviso('Revise el formulario', 'Todos los campos son obligatorios y la contraseña debe tener al menos 8 caracteres.');
      return;
    }
    this.enviando.set(true);
    this.sesion.registrar(r).subscribe({
      next: s => {
        this.avisos.exito('¡Cuenta creada!', `Bienvenida/o, ${s.nombre}`);
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver ?? '/mi-cuenta');
      },
      error: () => this.enviando.set(false)
    });
  }
}
