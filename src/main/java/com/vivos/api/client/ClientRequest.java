package com.vivos.api.client;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de POST y PATCH /clients. Como en el mock, el PATCH reemplaza todos los campos:
 * los opcionales ausentes quedan como cadena vacia (o null en departamento y tipo de entrega).
 */
public record ClientRequest(

        @NotNull(message = "El nombre es obligatorio")
        @Pattern(regexp = "(?s).*\\S.*", message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String fullName,

        @Size(max = 100, message = "La cuenta no puede superar 100 caracteres")
        String account,

        @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
        String phone,

        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
        String address,

        Department department,

        DeliveryType deliveryType
) {
}
