import { Pipe, PipeTransform } from '@angular/core';

const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fechaCorta = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short' });
const fechaLarga = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
const fechaHora = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });

/** Interpreta 'AAAA-MM-DD' como fecha local (sin desfase de zona horaria). */
export function aFecha(valor: string): Date {
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    const [a, m, d] = valor.split('-').map(Number);
    return new Date(a, m - 1, d);
  }
  return new Date(valor);
}

export function isoLocal(fecha: Date): string {
  const m = String(fecha.getMonth() + 1).padStart(2, '0');
  const d = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${m}-${d}`;
}

export function sumarDias(iso: string, dias: number): string {
  const f = aFecha(iso);
  f.setDate(f.getDate() + dias);
  return isoLocal(f);
}

@Pipe({ name: 'moneda' })
export class MonedaPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return valor === null || valor === undefined ? '—' : moneda.format(valor);
  }
}

@Pipe({ name: 'fecha' })
export class FechaPipe implements PipeTransform {
  transform(valor: string | null | undefined, formato: 'corta' | 'larga' | 'hora' = 'larga'): string {
    if (!valor) return '—';
    const f = aFecha(valor);
    if (isNaN(f.getTime())) return valor;
    return (formato === 'corta' ? fechaCorta : formato === 'hora' ? fechaHora : fechaLarga).format(f);
  }
}

@Pipe({ name: 'hora' })
export class HoraPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return valor ? valor.substring(0, 5) : '—';
  }
}
