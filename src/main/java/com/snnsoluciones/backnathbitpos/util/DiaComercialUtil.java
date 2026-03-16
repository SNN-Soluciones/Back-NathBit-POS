package com.snnsoluciones.backnathbitpos.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DiaComercialUtil {

    private static final LocalTime HORA_INICIO_DIA = LocalTime.of(4, 0, 0);
    private static final LocalTime HORA_FIN_DIA    = LocalTime.of(3, 59, 59);

    /**
     * Inicio del día comercial: fecha a las 04:00:00
     */
    public static LocalDateTime inicioDia(LocalDate fecha) {
        return fecha.atTime(HORA_INICIO_DIA);
    }

    /**
     * Fin del día comercial: día siguiente a las 03:59:59
     */
    public static LocalDateTime finDia(LocalDate fecha) {
        return fecha.plusDays(1).atTime(HORA_FIN_DIA);
    }

    /**
     * Dado un LocalDateTime, devuelve a qué día comercial pertenece.
     * Si es entre 00:00 y 03:59 → pertenece al día anterior.
     */
    public static LocalDate diaComercialDe(LocalDateTime fechaHora) {
        if (fechaHora.toLocalTime().isBefore(HORA_INICIO_DIA)) {
            return fechaHora.toLocalDate().minusDays(1);
        }
        return fechaHora.toLocalDate();
    }

    /**
     * Día comercial de hoy
     */
    public static LocalDate hoy() {
        return diaComercialDe(LocalDateTime.now());
    }
}