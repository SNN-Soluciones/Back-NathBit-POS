package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request para búsqueda de facturas para referencias
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscarFacturaReferenciaRequest {
    
    /**
     * Término de búsqueda libre
     * Puede ser: clave, consecutivo, nombre del cliente
     */
    @Size(max = 100, message = "Término de búsqueda muy largo")
    private String termino;
    
    /**
     * Fecha desde
     */
    private LocalDate fechaDesde;
    
    /**
     * Fecha hasta  
     */
    private LocalDate fechaHasta;
    
    /**
     * ID de empresa (se obtiene del contexto de seguridad)
     */
    private Long empresaId;
    
    /**
     * ID de sucursal (opcional, se obtiene del contexto)
     */
    private Long sucursalId;
    
    /**
     * Página
     */
    @Builder.Default
    private int pagina = 0;
    
    /**
     * Tamaño de página
     */
    @Builder.Default
    private int tamanio = 20;
}