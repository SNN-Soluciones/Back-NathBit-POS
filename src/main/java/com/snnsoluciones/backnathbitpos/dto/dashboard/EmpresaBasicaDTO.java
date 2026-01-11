package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

/**
 * DTO con datos básicos de la empresa para dashboard detallado
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaBasicaDTO {
    
    /**
     * ID de la empresa
     */
    private Long id;
    
    /**
     * Nombre comercial
     */
    private String nombreComercial;
    
    /**
     * Identificación fiscal (cédula jurídica)
     */
    private String identificacion;
}