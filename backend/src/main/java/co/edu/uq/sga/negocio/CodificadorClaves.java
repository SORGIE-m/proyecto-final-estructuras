package co.edu.uq.sga.negocio;

/** Abstracción para proteger contraseñas. */
public interface CodificadorClaves {
    String codificar(String clave);

    boolean coincide(String clave, String codificada);
}
