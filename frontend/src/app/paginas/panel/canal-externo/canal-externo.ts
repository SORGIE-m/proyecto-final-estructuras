import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { FechaPipe } from '../../../core/formatos';
import { Procesamiento, SolicitudExterna } from '../../../core/modelos';
import { NotificacionesService } from '../../../core/notificaciones.service';
import { Icono } from '../../../compartido/icono';

@Component({
  selector: 'app-canal-externo',
  imports: [FormsModule, Icono, FechaPipe],
  template: `
    <div class="pagina">
      <div class="encabezado-pagina">
        <div>
          <span class="sobretitulo">Cola FIFO</span>
          <h1 class="titulo-display">Canal <span class="cursiva">externo</span></h1>
          <p>Las solicitudes de las plataformas de terceros se procesan una a una en orden de llegada. Si chocan con una reserva vigente se rechazan como conflicto de canal.</p>
        </div>
        <div class="fila">
          <input class="entrada cantidad" type="number" min="1" max="20" [(ngModel)]="cantidad" aria-label="Cantidad a simular" />
          <button class="btn secundario" type="button" (click)="simular()"><app-icono nombre="enviar" [tamano]="15" /> Simular llegada</button>
        </div>
      </div>

      <div class="rejilla distribucion">
        <section class="tarjeta">
          <h3>En espera <span class="chip sin-punto">{{ pendientes().length }}</span></h3>
          <div class="cola">
            @for (s of pendientes(); track s.id; let i = $index) {
              <div class="solicitud" [class.siguiente]="i === 0">
                @if (i === 0) { <span class="marca">Siguiente</span> }
                <div class="fila-entre"><strong>{{ s.plataforma }}</strong><span class="tenue">{{ s.referencia }}</span></div>
                <div class="datos"><span>{{ s.apartamento }}</span> · {{ s.entrada | fecha: 'corta' }} → {{ s.salida | fecha: 'corta' }} · {{ s.personas }} pers.</div>
                <span class="tenue">{{ s.titularNombre }} · recibida {{ s.recibidaEn | fecha: 'hora' }}</span>
              </div>
            } @empty {
              <div class="vacio"><span class="cursiva">Cola vacía</span>Simula la llegada de solicitudes para probar el canal.</div>
            }
          </div>
          <div class="fila mt">
            <button class="btn" type="button" [disabled]="!pendientes().length || ocupado()" (click)="siguiente()"><app-icono nombre="adelante" [tamano]="15" /> Procesar siguiente</button>
            <button class="btn secundario" type="button" [disabled]="!pendientes().length || ocupado()" (click)="todas()">Procesar todas</button>
          </div>
        </section>

        <section class="tarjeta">
          <div class="fila-entre mb">
            <h3 class="tarjeta-titulo" style="margin:0">Procesadas</h3>
            <div class="resumen">
              <span class="chip ACEPTADA">{{ cuenta('ACEPTADA') }} aceptadas</span>
              <span class="chip CONFLICTO">{{ cuenta('CONFLICTO') }} conflictos</span>
              <span class="chip RECHAZADA">{{ cuenta('RECHAZADA') }} rechazadas</span>
            </div>
          </div>
          <div class="historial escalonado">
            @for (p of historial(); track p.solicitud.id + p.procesadaEn) {
              <div class="resultado" [class]="p.resultado">
                <div class="fila-entre">
                  <div><strong>{{ p.solicitud.plataforma }} · {{ p.solicitud.apartamento }}</strong>
                    <span class="tenue"> {{ p.solicitud.entrada | fecha: 'corta' }} → {{ p.solicitud.salida | fecha: 'corta' }}</span></div>
                  <span class="chip" [class]="p.resultado">{{ p.resultado }}</span>
                </div>
                <p>{{ p.motivo }}</p>
                <span class="tenue">{{ p.solicitud.titularNombre }} · {{ p.procesadaEn | fecha: 'hora' }}
                  @if (p.reservaCodigo) { · reserva {{ p.reservaCodigo }} }
                  @if (p.reservaEnConflicto) { · choca con {{ p.reservaEnConflicto }} }</span>
              </div>
            } @empty { <div class="vacio">Aún no se han procesado solicitudes.</div> }
          </div>
        </section>
      </div>
    </div>`,
  styles: [`
    .distribucion { grid-template-columns: minmax(320px, 1fr) 1.3fr; align-items: start; }
    .cantidad { width: 84px; }
    .cola { display: flex; flex-direction: column; gap: 10px; }
    .solicitud { position: relative; padding: 14px 16px; border-radius: 14px; background: var(--superficie-2); border: 1px solid var(--borde);
      animation: aparecer .35s both; strong { font-weight: 500; } .datos { margin: 4px 0; font-size: 14px; span { font-weight: 500; letter-spacing: .06em; } } }
    .solicitud.siguiente { border-color: var(--tierra-600); box-shadow: var(--sombra-sm); background: #fff; }
    .marca { position: absolute; top: -9px; right: 14px; background: var(--tierra-700); color: #fff; font-size: 10px; letter-spacing: .16em;
      text-transform: uppercase; padding: 3px 10px; border-radius: 999px; }
    .resumen { display: flex; gap: 6px; flex-wrap: wrap; }
    .historial { display: flex; flex-direction: column; gap: 10px; max-height: 640px; overflow-y: auto; }
    .resultado { padding: 12px 16px; border-radius: 14px; border: 1px solid var(--borde); border-left: 4px solid var(--estado-preparado); background: var(--superficie);
      strong { font-weight: 500; } p { margin: 6px 0 4px; font-size: 13.5px; color: var(--texto-suave); } }
    .resultado.CONFLICTO { border-left-color: var(--estado-fuera); background: #fdf6f4; }
    .resultado.RECHAZADA { border-left-color: var(--estado-pendiente); }
    @media (max-width: 1000px) { .distribucion { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CanalExterno implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(NotificacionesService);
  readonly pendientes = signal<SolicitudExterna[]>([]);
  readonly historial = signal<Procesamiento[]>([]);
  readonly ocupado = signal(false);
  cantidad = 3;

  private readonly conteo = computed(() => {
    const c: Record<string, number> = { ACEPTADA: 0, CONFLICTO: 0, RECHAZADA: 0 };
    for (const p of this.historial()) c[p.resultado]++;
    return c;
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    forkJoin({ p: this.api.externasPendientes(), h: this.api.historialExterno() }).subscribe(x => {
      this.pendientes.set(x.p);
      this.historial.set(x.h);
      this.ocupado.set(false);
    });
  }

  cuenta(r: string): number {
    return this.conteo()[r] ?? 0;
  }

  simular(): void {
    this.api.simularExternas(this.cantidad).subscribe(l => { this.avisos.info('Solicitudes recibidas', `${l.length} nueva(s) en la cola`); this.cargar(); });
  }

  siguiente(): void {
    this.ocupado.set(true);
    this.api.procesarSiguiente().subscribe({ next: p => { this.notificar(p); this.cargar(); }, error: () => this.ocupado.set(false) });
  }

  todas(): void {
    this.ocupado.set(true);
    this.api.procesarTodas().subscribe({
      next: l => {
        const conflictos = l.filter(p => p.resultado === 'CONFLICTO').length;
        this.avisos.info('Cola procesada', `${l.length} solicitud(es) · ${conflictos} conflicto(s) de canal`);
        this.cargar();
      },
      error: () => this.ocupado.set(false)
    });
  }

  private notificar(p: Procesamiento): void {
    if (p.resultado === 'ACEPTADA') this.avisos.exito('Solicitud aceptada', `Reserva ${p.reservaCodigo} creada`);
    else if (p.resultado === 'CONFLICTO') this.avisos.aviso('Conflicto de canal', p.motivo);
    else this.avisos.error('Solicitud rechazada', p.motivo);
  }
}
