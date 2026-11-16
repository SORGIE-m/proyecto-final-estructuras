import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api.service';
import { MonedaPipe, isoLocal } from '../../../core/formatos';
import { Estadisticas as Datos } from '../../../core/modelos';
import { Icono } from '../../../compartido/icono';

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

@Component({
  selector: 'app-estadisticas',
  imports: [FormsModule, Icono, MonedaPipe],
  templateUrl: './estadisticas.html',
  styleUrl: './estadisticas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Estadisticas implements OnInit {
  private readonly api = inject(ApiService);
  readonly datos = signal<Datos | null>(null);
  readonly cargando = signal(false);
  readonly verTabla = signal(false);
  readonly mesActivo = signal<number | null>(null);
  desde = '';
  hasta = '';

  /** Escala común para las dos series de ingresos (un solo eje). */
  readonly maxIngreso = computed(() => Math.max(1, ...(this.datos()?.ingresosPorMes ?? []).flatMap(m => [m.recaudado, m.facturado])));
  readonly marcasEje = computed(() => {
    const max = this.maxIngreso();
    const paso = this.redondear(max / 4);
    return [0, 1, 2, 3, 4].map(i => i * paso).filter(v => v <= max * 1.05);
  });
  readonly maxCanal = computed(() => Math.max(1, ...(this.datos()?.reservasPorCanal ?? []).map(c => c.total)));

  ngOnInit(): void {
    this.api.hoy().subscribe(h => {
      const f = new Date(Number(h.substring(0, 4)), Number(h.substring(5, 7)) - 1, 1);
      this.desde = isoLocal(f);
      this.hasta = isoLocal(new Date(f.getFullYear(), f.getMonth() + 1, 0));
      this.cargar();
    });
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.estadisticas(this.desde, this.hasta).subscribe({
      next: d => { this.datos.set(d); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }

  periodo(tipo: 'mes' | 'trimestre' | 'anio'): void {
    const base = this.desde ? new Date(Number(this.desde.substring(0, 4)), Number(this.desde.substring(5, 7)) - 1, 1) : new Date();
    const hoy = new Date();
    if (tipo === 'mes') {
      this.desde = isoLocal(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
      this.hasta = isoLocal(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0));
    } else if (tipo === 'trimestre') {
      this.desde = isoLocal(new Date(hoy.getFullYear(), hoy.getMonth() - 2, 1));
      this.hasta = isoLocal(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0));
    } else {
      this.desde = isoLocal(new Date(base.getFullYear(), 0, 1));
      this.hasta = isoLocal(new Date(base.getFullYear(), 11, 31));
    }
    this.cargar();
  }

  etiquetaMes(m: string): string {
    return MESES[Number(m.substring(5, 7)) - 1] + ' ' + m.substring(2, 4);
  }

  alto(valor: number): number {
    return (valor / this.maxIngreso()) * 180;
  }

  compacto(v: number): string {
    if (v >= 1_000_000) return (v / 1_000_000).toFixed(1).replace('.0', '') + ' M';
    if (v >= 1000) return Math.round(v / 1000) + ' k';
    return String(v);
  }

  private redondear(v: number): number {
    const p = Math.pow(10, Math.floor(Math.log10(Math.max(1, v))));
    return Math.ceil(v / p) * p;
  }
}
