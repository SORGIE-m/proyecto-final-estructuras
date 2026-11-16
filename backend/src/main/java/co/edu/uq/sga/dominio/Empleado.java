package co.edu.uq.sga.dominio;

import co.edu.uq.sga.excepciones.ReglaNegocioException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Empleado del alojamiento (recepcionista o administrador). Orden natural por documento. */
public class Empleado implements Comparable<Empleado>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String documento;
    private String nombre;
    private Rol rol;
    private String usuario;
    private String correo;
    private String telefono;
    private String claveHash;
    private boolean activo = true;

    public Empleado(String documento, String nombre, Rol rol, String usuario, String correo, String telefono) {
        if (documento == null || documento.isBlank()) {
            throw new ReglaNegocioException("El documento del empleado es obligatorio");
        }
        this.documento = documento.trim();
        actualizar(nombre, rol, usuario, correo, telefono);
    }

    public final void actualizar(String nombre, Rol rol, String usuario, String correo, String telefono) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del empleado es obligatorio");
        }
        if (rol == null || rol == Rol.HUESPED) {
            throw new ReglaNegocioException("El rol del empleado debe ser RECEPCIONISTA o ADMINISTRADOR");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new ReglaNegocioException("El usuario es obligatorio");
        }
        this.nombre = nombre.trim();
        this.rol = rol;
        this.usuario = usuario.trim().toLowerCase();
        this.correo = correo == null ? "" : correo.trim().toLowerCase();
        this.telefono = telefono == null ? "" : telefono.trim();
    }

    @Override
    public int compareTo(Empleado o) {
        return documento.compareTo(o.documento);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Empleado e && documento.equals(e.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documento);
    }

    public String getDocumento() { return documento; }
    public String getNombre() { return nombre; }
    public Rol getRol() { return rol; }
    public String getUsuario() { return usuario; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
