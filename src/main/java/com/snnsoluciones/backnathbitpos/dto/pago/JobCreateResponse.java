package com.snnsoluciones.backnathbitpos.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response al crear un job de generación de facturas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateResponse {
    
    /**
     * ID único del job creado
     */
    private String jobId;
    
    /**
     * Estado inicial del job (típicamente "EN_QUEUE")
     */
    private String status;
    
    /**
     * Nombre completo del cajero que ejecutará las facturas
     */
    private String cajeroNombre;
    
    /**
     * ID de la sesión de caja asociada
     */
    private Long sesionCajaId;
    
    /**
     * ID del terminal asociado
     */
    private Long terminalId;
    
    /**
     * Mensaje descriptivo
     */
    private String mensaje;
}