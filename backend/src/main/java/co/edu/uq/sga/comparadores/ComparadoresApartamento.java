package co.edu.uq.sga.comparadores;

import co.edu.uq.sga.dominio.Apartamento;

import java.util.Comparator;

/** Órdenes alternativos al natural (por código) para los apartamentos. */
public final class ComparadoresApartamento {

    private ComparadoresApartamento() {
    }

    /** Por tarifa base (temporada baja) por persona por noche; desempata por código. */
    public static Comparator<Apartamento> porPrecio() {
        return Comparator.comparingLong((Apartamento a) -> a.getTarifas().baja())
                .thenComparing(Comparator.naturalOrder());
    }

    /** De mayor a menor capacidad. */
    public static Comparator<Apartamento> porCapacidad() {
        return Comparator.comparingInt(Apartamento::getCapacidad).reversed()
                .thenComparing(Comparator.naturalOrder());
    }

    /** De mayor a menor número de dormitorios. */
    public static Comparator<Apartamento> porDormitorios() {
        return Comparator.comparingInt(Apartamento::getDormitorios).reversed()
                .thenComparing(Comparator.naturalOrder());
    }

    /** De más a menos reservas registradas (popularidad). */
    public static Comparator<Apartamento> porNumeroReservas() {
        return Comparator.comparingInt((Apartamento a) -> a.getReservas().tamanio()).reversed()
                .thenComparing(Comparator.naturalOrder());
    }
}
