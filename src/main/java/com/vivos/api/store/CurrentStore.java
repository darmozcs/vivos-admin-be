package com.vivos.api.store;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parametro {@code UUID} el id de la tienda del usuario autenticado.
 * La tienda se resuelve con el {@code sub} del JWT; nunca viene del cliente.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentStore {
}
