package co.edu.uq.sga.web.seguridad;

import co.edu.uq.sga.dominio.Rol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca un endpoint como protegido: exige sesión y uno de los roles indicados. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereRol {
    Rol[] value();
}
