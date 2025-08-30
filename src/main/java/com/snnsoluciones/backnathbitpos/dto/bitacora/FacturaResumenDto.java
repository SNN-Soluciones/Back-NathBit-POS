package com.snnsoluciones.backnathbitpos.dto.bitacora;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Información resumida de la factura para mostrar en bitácora
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResumenDto {
    private Long id;
    private String consecutivo;
    private TipoDocumento tipoDocumento;
    private String fechaEmision;
    private String clienteNombre;
    private String clienteIdentificacion;
    private BigDecimal montoTotal;
    private String moneda;
    private String empresaNombre;
    private String sucursalNombre;
}