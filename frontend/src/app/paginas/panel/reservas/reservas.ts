import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../../core/api.service';
import { FechaPipe, HoraPipe, MonedaPipe, isoLocal, sumarDias } from '../../../core/formatos';
import { Cotizacion, Folio, Ocupante, Opcion, PropuestaGrupo, Reembolso, Reserva } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { ConfirmacionService } from '../../../compartido/confirmacion.service';
import { Icono } from '../../../compartido/icono';

type Vista = 'lista' | 'directa' | 'grupo';

@Component({
  selector: 'app-reservas',
  imports: [FormsModule, Icono, MonedaPipe, FechaPipe, HoraPipe],
  templateUrl: './reservas.html',
  styleUrl: './reservas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Reservas implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly ruta = inject(ActivatedRoute);

  readonly vista = signal<Vista>('lista');
  readonly reservas = signal<Reserva[]>([]);
  readonly cargando = signal(true);
  filtroEstado = '';
  filtroCanal = '';
  texto = '';

  // detalle
  readonly detalle = signal<Reserva | null>(null);
  readonly folio = signal<Folio | null>(null);
  readonly reembolso = signal<Reembolso | null>(null);
  readonly ocupado = signal(false);
  movimiento = { concepto: '', monto: 0 };

  // reserva directa
  readonly hoy = isoLocal(new Date());
  directa = {
    titular: { documento: '', nombre: '', fechaNacimiento: '', telefono: '', correo: '' },
    apartamentoCodigo: '', entrada: this.hoy, salida: sumarDias(this.hoy, 2), horaLlegada: '15:00', observaciones: ''
  };
  readonly ocupantesDirecta = signal<Ocupante[]>([{ nombre: '', fechaNacimiento: '' }]);
  readonly libres = signal<Opcion[]>([]);
  readonly cotizacionDirecta = signal<Cotizacion | null>(null);

  // grupo
  grupo = { entrada: sumarDias(this.hoy, 14), salida: sumarDias(this.hoy, 17), personas: 12, horaLlegada: '15:00' };
  readonly propuesta = signal<PropuestaGrupo | null>(null);
  readonly integrantes = signal<Ocupante[]>([]);

  ngOnInit(): void {
    if (this.ruta.snapshot.queryParamMap.get('nueva')) this.vista.set('directa');
    this.cargar();
    this.buscarLibres();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.reservas({ estado: this.filtroEstado, canal: this.filtroCanal, texto: this.texto }).subscribe({
      next: l => { this.reservas.set(l); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }

  // ------------------------------------------------------------------ detalle y folio

  abrir(r: Reserva): void {
    this.detalle.set(r);
    this.reembolso.set(null);
    this.movimiento = { concepto: '', monto: 0 };
    this.api.folio(r.codigo).subscribe(f => this.folio.set(f));
  }

  private refrescarDetalle(codigo: string): void {
    this.api.reserva(codigo).subscribe(r => {
      this.detalle.set(r);
      this.reservas.update(l => l.map(x => (x.codigo === r.codigo ? r : x)));
    });
    this.api.folio(codigo).subscribe(f => this.folio.set(f));
  }

  accion(tipo: 'confirmar' | 'checkIn' | 'checkOut' | 'noShow'): void {
    const r = this.detalle();
    if (!r) return;
    const textos = { confirmar: 'Reserva confirmada', checkIn: 'Check-in registrado', checkOut: 'Check-out registrado', noShow: 'Marcada como no-show' };
    this.ocupado.set(true);
    this.api[tipo](r.codigo).subscribe({
      next: () => { this.avisos.exito(textos[tipo], r.codigo); this.ocupado.set(false); this.refrescarDetalle(r.codigo); },
      error: () => this.ocupado.set(false)
    });
  }

  verCancelacion(): void {
    const r = this.detalle();
    if (r) this.api.previsualizarCancelacion(r.codigo).subscribe(x => this.reembolso.set(x));
  }

  async cancelar(): Promise<void> {
    const r = this.detalle();
    const re = this.reembolso();
    if (!r || !re) return;
    const ok = await this.confirmacion.confirmar('Cancelar reserva',
      `Se devolverán ${re.reembolso.toLocaleString('es-CO')} COP (${re.porcentaje} %) y se anularán los cargos. Esta acción no se puede deshacer.`,
      'Cancelar reserva', true);
    if (!ok) return;
    this.api.cancelar(r.codigo, 'Cancelada por recepción').subscribe(() => {
      this.avisos.exito('Reserva cancelada', r.codigo);
      this.reembolso.set(null);
      this.refrescarDetalle(r.codigo);
    });
  }

  registrarMovimiento(tipo: 'cargo' | 'pago'): void {
    const r = this.detalle();
    if (!r || !this.movimiento.concepto.trim() || this.movimiento.monto <= 0) {
      this.avisos.aviso('Datos incompletos', 'Indique el concepto y un monto mayor que cero.');
      return;
    }
    this.ocupado.set(true);
    this.api[tipo](r.codigo, this.movimiento.concepto, this.movimiento.monto).subscribe({
      next: f => {
        this.folio.set(f);
        this.movimiento = { concepto: '', monto: 0 };
        this.avisos.exito(tipo === 'cargo' ? 'Cargo registrado' : 'Pago registrado');
        this.ocupado.set(false);
        this.refrescarDetalle(r.codigo);
      },
      error: () => this.ocupado.set(false)
    });
  }

  pagarSaldo(): void {
    const f = this.folio();
    if (f && f.saldo > 0) {
      this.movimiento = { concepto: 'Pago del saldo', monto: f.saldo };
      this.registrarMovimiento('pago');
    }
  }

  async deshacer(): Promise<void> {
    const r = this.detalle();
    const ultima = this.folio()?.ultimaOperacion;
    if (!r || !ultima) return;
    const ok = await this.confirmacion.confirmar('Deshacer última operación',
      `Se registrará un movimiento contrario a "#${ultima.numero} ${ultima.concepto}" por ${ultima.monto.toLocaleString('es-CO')} COP. El movimiento original no se borra.`,
      'Deshacer');
    if (!ok) return;
    this.api.deshacer(r.codigo).subscribe(f => { this.folio.set(f); this.avisos.info('Operación anulada', 'Se registró el movimiento contrario.'); this.refrescarDetalle(r.codigo); });
  }

  // ------------------------------------------------------------------ reserva directa

  buscarTitular(): void {
    const doc = this.directa.titular.documento.trim();
    if (!doc) return;
    this.api.huesped(doc).subscribe({
      next: h => {
        this.directa.titular = { documento: h.documento, nombre: h.nombre, fechaNacimiento: h.fechaNacimiento, telefono: h.telefono, correo: h.correo };
        const l = [...this.ocupantesDirecta()];
        l[0] = { nombre: h.nombre, documento: h.documento, fechaNacimiento: h.fechaNacimiento };
        this.ocupantesDirecta.set(l);
        this.avisos.info('Huésped encontrado', h.nombre);
        this.cotizarDirecta();
      },
      error: () => undefined
    });
  }

  buscarLibres(): void {
    const personas = Math.max(1, this.ocupantesDirecta().length);
    this.api.disponibilidad(this.directa.entrada, this.directa.salida, personas, 'codigo').subscribe({
      next: l => {
        this.libres.set(l);
        if (!l.some(o => o.apartamento.codigo === this.directa.apartamentoCodigo)) {
          this.directa.apartamentoCodigo = l[0]?.apartamento.codigo ?? '';
        }
        this.cotizarDirecta();
      },
      error: () => this.libres.set([])
    });
  }

  actualizarOcupante(lista: 'directa' | 'grupo', i: number, campo: 'nombre' | 'fechaNacimiento', valor: string): void {
    const s = lista === 'directa' ? this.ocupantesDirecta : this.integrantes;
    s.update(l => l.map((o, k) => (k === i ? { ...o, [campo]: valor } : o)));
    if (lista === 'directa' && campo === 'fechaNacimiento') this.cotizarDirecta();
  }

  agregarOcupanteDirecta(): void {
    this.ocupantesDirecta.update(l => [...l, { nombre: '', fechaNacimiento: '' }]);
    this.buscarLibres();
  }

  quitarOcupanteDirecta(i: number): void {
    if (this.ocupantesDirecta().length > 1) {
      this.ocupantesDirecta.update(l => l.filter((_, k) => k !== i));
      this.buscarLibres();
    }
  }

  cotizarDirecta(): void {
    if (!this.directa.apartamentoCodigo) { this.cotizacionDirecta.set(null); return; }
    const ocupantes = this.ocupantesDirecta().map(o => ({ nombre: o.nombre || 'Ocupante', fechaNacimiento: o.fechaNacimiento || '1990-01-01' }));
    this.api.cotizar(this.directa.apartamentoCodigo, this.directa.entrada, this.directa.salida, ocupantes)
      .subscribe({ next: c => this.cotizacionDirecta.set(c), error: () => this.cotizacionDirecta.set(null) });
  }

  crearDirecta(): void {
    const t = this.directa.titular;
    if (!t.documento || !t.nombre || !t.fechaNacimiento || !t.telefono || !t.correo) {
      this.avisos.aviso('Faltan datos del titular', 'Complete documento, nombre, fecha de nacimiento, teléfono y correo.');
      return;
    }
    if (this.ocupantesDirecta().some(o => !o.nombre.trim() || !o.fechaNacimiento)) {
      this.avisos.aviso('Faltan datos de ocupantes', 'Cada ocupante necesita nombre y fecha de nacimiento.');
      return;
    }
    this.ocupado.set(true);
    this.api.reservarDirecta({ ...this.directa, ocupantes: this.ocupantesDirecta() }).subscribe({
      next: r => {
        this.avisos.exito('Reserva directa creada', `${r.codigo} · ${r.apartamentoNombre}`);
        this.ocupado.set(false);
        this.vista.set('lista');
        this.cargar();
        this.abrir(r);
      },
      error: () => this.ocupado.set(false)
    });
  }

  // ------------------------------------------------------------------ grupos

  proponer(): void {
    this.api.proponerGrupo(this.grupo.entrada, this.grupo.salida, this.grupo.personas).subscribe({
      next: p => {
        this.propuesta.set(p);
        const actuales = this.integrantes();
        const nueva: Ocupante[] = [];
        for (let i = 0; i < this.grupo.personas; i++) {
          nueva.push(actuales[i] ?? { nombre: i === 0 ? this.directa.titular.nombre || 'Integrante 1' : `Integrante ${i + 1}`, fechaNacimiento: '1990-01-01' });
        }
        this.integrantes.set(nueva);
      },
      error: () => this.propuesta.set(null)
    });
  }

  crearGrupo(): void {
    const t = this.directa.titular;
    if (!t.documento || !t.nombre || !t.fechaNacimiento || !t.telefono || !t.correo) {
      this.avisos.aviso('Faltan datos del titular', 'Complete los datos del titular del grupo.');
      return;
    }
    this.ocupado.set(true);
    this.api.reservarGrupo({ titular: t, entrada: this.grupo.entrada, salida: this.grupo.salida, horaLlegada: this.grupo.horaLlegada, ocupantes: this.integrantes() })
      .subscribe({
        next: l => {
          this.avisos.exito('Grupo reservado', `${l.length} apartamento(s): ${l.map(r => r.apartamentoCodigo).join(', ')}`);
          this.ocupado.set(false);
          this.propuesta.set(null);
          this.vista.set('lista');
          this.cargar();
        },
        error: () => this.ocupado.set(false)
      });
  }
}
