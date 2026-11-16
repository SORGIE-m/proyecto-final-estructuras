package co.edu.uq.sga.soporte;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj real en la zona horaria del alojamiento (Colombia). */
@Component
public class RelojSistema implements Reloj {

    private final ZoneId zona;

    public RelojSistema(@Value("${sga.zona-horaria:America/Bogota}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    @Override
    public LocalDateTime ahora() {
        return LocalDateTime.now(zona).withNano(0);
    }
}
