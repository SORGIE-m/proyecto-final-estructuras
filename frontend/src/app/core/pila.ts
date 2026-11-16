/**
 * Pila genérica enlazada (LIFO) del lado del cliente. Se usa para la navegación "atrás"
 * entre pantallas, igual que la pila propia del backend.
 */
interface Nodo<T> {
  dato: T;
  debajo: Nodo<T> | null;
}

export class Pila<T> implements Iterable<T> {
  private cima: Nodo<T> | null = null;
  private cantidad = 0;

  apilar(dato: T): void {
    this.cima = { dato, debajo: this.cima };
    this.cantidad++;
  }

  desapilar(): T | undefined {
    if (!this.cima) return undefined;
    const dato = this.cima.dato;
    this.cima = this.cima.debajo;
    this.cantidad--;
    return dato;
  }

  verCima(): T | undefined {
    return this.cima?.dato;
  }

  get tamanio(): number {
    return this.cantidad;
  }

  estaVacia(): boolean {
    return this.cantidad === 0;
  }

  vaciar(): void {
    this.cima = null;
    this.cantidad = 0;
  }

  *[Symbol.iterator](): Iterator<T> {
    for (let n = this.cima; n; n = n.debajo) yield n.dato;
  }
}
