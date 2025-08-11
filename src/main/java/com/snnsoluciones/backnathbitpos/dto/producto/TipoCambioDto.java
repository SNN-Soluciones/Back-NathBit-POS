package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para tipo de cambio (relacionado)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoCambioDto {
    private Long id;
    private MonedaSelectDto moneda;
    private String fecha;
    private BigDecimal tipoCambioCompra;
    private BigDecimal tipoCambioVenta;
    private BigDecimal tipoCambioReferencia;
    private String fuente;
    private String fechaActualizacion;
}