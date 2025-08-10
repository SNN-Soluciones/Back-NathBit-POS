package com.snnsoluciones.backnathbitpos.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Utilidad para generar la clave numérica de 50 dígitos según
 * el estándar del Ministerio de Hacienda de Costa Rica
 */
public class GeneradorClaveUtil {
    
    private static final String CODIGO_PAIS = "506";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final Random RANDOM = new Random();
    
    /**
     * Genera la clave de 50 dígitos para un documento electrónico
     * 
     * @param fechaEmision Fecha de emisión del documento
     * @param tipoIdentificacion 1=Física, 2=Jurídica, 3=DIMEX, 4=NITE
     * @param identificacion Número de identificación sin guiones
     * @param consecutivo Consecutivo de 20 dígitos
     * @param situacion 1=Normal, 2=Contingencia, 3=Sin Internet
     * @param documentoId ID del documento para generar código seguridad
     * @return Clave de 50 dígitos
     */
    public static String generarClave(
            LocalDateTime fechaEmision,
            int tipoIdentificacion,
            String identificacion,
            String consecutivo,
            int situacion,
            Long documentoId) {
        
        // Validaciones
        if (consecutivo.length() != 20) {
            throw new IllegalArgumentException("El consecutivo debe tener 20 dígitos");
        }
        
        // Componentes
        String fecha = fechaEmision.format(FORMATO_FECHA);
        String identificacionFormateada = formatearIdentificacion(tipoIdentificacion, identificacion);
        String codigoSeguridad = generarCodigoSeguridad(documentoId);
        
        // Armar clave
        return CODIGO_PAIS + 
               fecha + 
               identificacionFormateada + 
               consecutivo + 
               situacion + 
               codigoSeguridad;
    }
    
    /**
     * Formatea la identificación a 12 dígitos según el tipo
     */
    private static String formatearIdentificacion(int tipo, String identificacion) {
        // Limpiar identificación (quitar guiones, espacios, etc)
        String limpia = identificacion.replaceAll("[^0-9]", "");
        
        return switch (tipo) {
            case 1 -> // Física: 9 dígitos + 3 ceros al inicio
                String.format("%03d%s", 0, limpia.substring(0, Math.min(limpia.length(), 9)));
            case 2 -> // Jurídica: 10 dígitos + 2 ceros al inicio
                String.format("%02d%s", 0, limpia.substring(0, Math.min(limpia.length(), 10)));
            case 3, 4 -> // DIMEX/NITE: 11-12 dígitos + padding si necesario
                String.format("%012d", Long.parseLong(limpia));
            default -> throw new IllegalArgumentException("Tipo de identificación inválido: " + tipo);
        };
    }
    
    /**
     * Genera código de seguridad de 8 dígitos
     * Opción A: Últimos 4 del timestamp + 4 random
     */
    public static String generarCodigoSeguridad(Long documentoId) {
        // Últimos 4 dígitos del timestamp actual
        String timestamp = String.valueOf(System.currentTimeMillis());
        String ultimos4Timestamp = timestamp.substring(timestamp.length() - 4);
        
        // 4 dígitos random
        String random4 = String.format("%04d", RANDOM.nextInt(10000));
        
        return ultimos4Timestamp + random4;
    }
    
    /**
     * Descompone una clave para debugging
     */
    public static ClaveDescompuesta descomponerClave(String clave) {
        if (clave.length() != 50) {
            throw new IllegalArgumentException("La clave debe tener 50 dígitos");
        }
        
        return ClaveDescompuesta.builder()
            .pais(clave.substring(0, 3))
            .fecha(clave.substring(3, 11))
            .identificacion(clave.substring(11, 23))
            .consecutivo(clave.substring(23, 43))
            .situacion(clave.substring(43, 44))
            .codigoSeguridad(clave.substring(44, 50))
            .build();
    }
    
    // Clase interna para el resultado descompuesto
    @lombok.Builder
    @lombok.Data
    public static class ClaveDescompuesta {
        private String pais;
        private String fecha;
        private String identificacion;
        private String consecutivo;
        private String situacion;
        private String codigoSeguridad;
    }
}