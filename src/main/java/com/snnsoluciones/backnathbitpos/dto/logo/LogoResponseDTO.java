package com.snnsoluciones.backnathbitpos.dto.logo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para retornar información del logo
 * Prioridad: Logo de Sucursal → Logo de Empresa → null
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoResponseDTO {
    
    /**
     * URL firmada del logo (temporal, válida por 60 minutos)
     */
    private String logoUrl;
    
    /**
     * Nombre de la empresa/sucursal
     */
    private String nombre;
    
    /**
     * Tipo de logo: "SUCURSAL", "EMPRESA", o "NONE"
     */
    private String tipoLogo;
    
    /**
     * ID de la empresa
     */
    private Long empresaId;
    
    /**
     * ID de la sucursal
     */
    private Long sucursalId;
    
    /**
     * Indica si hay logo disponible
     */
    private Boolean tienelogo;
    
    /**
     * Iniciales para mostrar si no hay logo
     */
    private String iniciales;
}