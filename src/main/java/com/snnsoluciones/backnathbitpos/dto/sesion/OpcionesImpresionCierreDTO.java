package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para opciones de impresión/envío de cierre de caja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionesImpresionCierreDTO {
    
    /**
     * ¿Incluir movimientos de caja en el reporte?
     * (Vales, depósitos, entradas, salidas)
     */
    @Builder.Default
    private Boolean incluirMovimientos = true;
    
    /**
     * ¿Incluir listado de facturas/documentos emitidos?
     */
    @Builder.Default
    private Boolean incluirFacturas = true;
    
    /**
     * ¿Incluir detalle de plataformas digitales?
     * (UberEats, Rappi, etc.)
     */
    @Builder.Default
    private Boolean incluirPlataformas = false;
    
    /**
     * Lista de correos adicionales donde enviar el cierre
     * (Además del correo de la sucursal)
     */
    @Builder.Default
    private List<String> correosAdicionales = new ArrayList<>();
    
    /**
     * Acción a realizar:
     * - "imprimir": Solo generar PDF
     * - "enviar": Solo enviar por email
     * - "ambos": Generar PDF y enviar email
     */
    private String accion;
    
    /**
     * Observaciones adicionales (opcional)
     */
    private String observacionesAdicionales;
}