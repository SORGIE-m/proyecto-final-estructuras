package co.edu.uq.sga.negocio;

import co.edu.uq.sga.excepciones.SgaException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria. Formato: iteraciones$sal$hash. */
@Component
public class CodificadorPbkdf2 implements CodificadorClaves {

    private static final int ITERACIONES = 60_000;
    private static final int BITS = 256;
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String codificar(String clave) {
        byte[] sal = new byte[16];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(clave, sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    @Override
    public boolean coincide(String clave, String codificada) {
        if (clave == null || codificada == null) {
            return false;
        }
        String[] partes = codificada.split("\\$");
        if (partes.length != 3) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] esperado = b64.decode(partes[2]);
        byte[] calculado = derivar(clave, b64.decode(partes[1]), Integer.parseInt(partes[0]));
        return MessageDigest.isEqual(esperado, calculado);
    }

    private byte[] derivar(String clave, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new SgaException("No fue posible procesar la contraseña");
        }
    }
}
