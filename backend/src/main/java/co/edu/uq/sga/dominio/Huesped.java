package co.edu.uq.sga.dominio;

import co.edu.uq.sga.estructuras.Conjunto;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/** Perfil del huésped. Orden natural por documento (árbol binario de búsqueda). */
public class Huesped implements Comparable<Huesped>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final String documento;
    private String nombre;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String foto;
    private String claveHash;
    private final Conjunto<String> favoritos = new Conjunto<>();
    /** Códigos de reserva de las estancias finalizadas, de la más reciente a la más antigua. */
    private final ListaDoble<String> historial = new ListaDoble<>();

    public Huesped(String documento, String nombre, LocalDate fechaNacimiento, String telefono, String correo) {
        if (documento == null || documento.isBlank()) {
            throw new ReglaNegocioException("El documento es obligatorio");
        }
        this.documento = documento.trim();
        actualizar(nombre, fechaNacimiento, telefono, correo);
    }

    public final void actualizar(String nombre, LocalDate fechaNacimiento, String telefono, String correo) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            throw new FechasInvalidasException("La fecha de nacimiento no es válida");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new ReglaNegocioException("El teléfono es obligatorio");
        }
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw new ReglaNegocioException("El correo electrónico no es válido");
        }
        this.nombre = nombre.trim();
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono.trim();
        this.correo = correo.trim().toLowerCase();
    }

    public int edadEn(LocalDate fecha) {
        return java.time.Period.between(fechaNacimiento, fecha).getYears();
    }

    public void registrarEstanciaFinalizada(String codigoReserva) {
        if (!historial.contiene(codigoReserva)) {
            historial.agregarAlInicio(codigoReserva);
        }
    }

    @Override
    public int compareTo(Huesped o) {
        return documento.compareTo(o.documento);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Huesped h && documento.equals(h.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documento);
    }

    public String getDocumento() { return documento; }
    public String getNombre() { return nombre; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public Conjunto<String> getFavoritos() { return favoritos; }
    public ListaDoble<String> getHistorial() { return historial; }
}
