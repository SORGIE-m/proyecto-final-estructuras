package co.edu.uq.sga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** SGA — Sistema de Gestión de Alojamiento de Aparta-Suites UQ (Salento, Quindío). */
@SpringBootApplication
@EnableScheduling
public class SgaAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(SgaAplicacion.class, args);
    }
}
