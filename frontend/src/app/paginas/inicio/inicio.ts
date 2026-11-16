import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MonedaPipe, isoLocal, sumarDias } from '../../core/formatos';
import { Apartamento } from '../../core/modelos';
import { Icono } from '../../compartido/icono';
import { ImagenApartamento } from '../../compartido/imagen-apartamento';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, FormsModule, Icono, ImagenApartamento, MonedaPipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Inicio implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly destacados = signal<Apartamento[]>([]);
  readonly cargando = signal(true);
  entrada = sumarDias(isoLocal(new Date()), 7);
  salida = sumarDias(isoLocal(new Date()), 10);
  personas = 2;

  readonly servicios = [
    { icono: 'montana', titulo: 'Vista al valle', texto: 'Ventanales hacia las montañas del Quindío' },
    { icono: 'wifi', titulo: 'Wi-Fi 24 horas', texto: 'Conexión estable en todo el edificio' },
    { icono: 'cafe', titulo: 'Café de origen', texto: 'Cafetera y café local en cada cocina' },
    { icono: 'palma', titulo: 'Valle del Cocora', texto: 'A minutos de las palmas de cera' }
  ];

  ngOnInit(): void {
    this.api.apartamentos('capacidad').subscribe({
      next: l => { this.destacados.set(l.filter(a => a.estado !== 'FUERA_SERVICIO').slice(0, 6)); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }

  buscar(): void {
    this.router.navigate(['/reservar'], { queryParams: { entrada: this.entrada, salida: this.salida, personas: this.personas } });
  }
}
