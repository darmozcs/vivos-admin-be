package com.vivos.api.live;

import com.vivos.api.common.BadRequestException;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Filtros opcionales de {@code GET /lives} y {@code GET /lives/summary}; se combinan con AND.
 * Los rangos min/max son inclusive.
 *
 * @param from               dia inicial (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param to                 dia final (inclusive) de {@code createdAt}, en la zona horaria de la tienda
 * @param title              el titulo contiene el texto, sin distinguir mayusculas
 * @param minDurationMinutes duracion efectiva minima, en minutos enteros
 * @param maxDurationMinutes duracion efectiva maxima, en minutos enteros
 * @param minBuyers          compradoras minimas (ventas no canceladas con items)
 * @param maxBuyers          compradoras maximas
 * @param minUnits           unidades minimas
 * @param maxUnits           unidades maximas
 * @param minTotal           monto total minimo
 * @param maxTotal           monto total maximo
 */
public record LiveFilter(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String title,
        Integer minDurationMinutes,
        Integer maxDurationMinutes,
        Integer minBuyers,
        Integer maxBuyers,
        Integer minUnits,
        Integer maxUnits,
        BigDecimal minTotal,
        BigDecimal maxTotal
) {

    static final String INVALID_DATE_RANGE = "La fecha desde no puede ser posterior a la fecha hasta";
    static final String INVALID_RANGE = "El minimo no puede ser mayor que el maximo";

    /** 400 si algun numero es negativo, si from > to o si algun minimo supera su maximo. */
    void validate() {
        requireNonNegative("minDurationMinutes", minDurationMinutes);
        requireNonNegative("maxDurationMinutes", maxDurationMinutes);
        requireNonNegative("minBuyers", minBuyers);
        requireNonNegative("maxBuyers", maxBuyers);
        requireNonNegative("minUnits", minUnits);
        requireNonNegative("maxUnits", maxUnits);
        requireNonNegative("minTotal", minTotal == null ? null : minTotal.signum());
        requireNonNegative("maxTotal", maxTotal == null ? null : maxTotal.signum());
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(INVALID_DATE_RANGE);
        }
        requireOrdered(minDurationMinutes, maxDurationMinutes);
        requireOrdered(minBuyers, maxBuyers);
        requireOrdered(minUnits, maxUnits);
        requireOrdered(minTotal, maxTotal);
    }

    private static void requireNonNegative(String name, Integer value) {
        if (value != null && value < 0) {
            throw new BadRequestException("El parametro " + name + " no es valido");
        }
    }

    private static <T extends Comparable<T>> void requireOrdered(T min, T max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException(INVALID_RANGE);
        }
    }
}
