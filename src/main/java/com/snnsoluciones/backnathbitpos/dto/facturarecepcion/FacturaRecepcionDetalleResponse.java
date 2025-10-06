package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FacturaRecepcionDetalleResponse {
    
    private Long id;
    private Integer numeroLinea;
    
    private String codigoCabys;
    private String codigoComercial;
    private String tipoCodigoComercial;
    
    private String detalle;
    private BigDecimal cantidad;
    private String unidadMedida;
    private String unidadMedidaComercial;
    
    private BigDecimal precioUnitario;
    private BigDecimal montoTotal;
    private BigDecimal subTotal;
    private BigDecimal montoDescuento;
    private BigDecimal montoTotalLinea;
    
    private Long productoId; // Si se matcheó con producto existente
    private String productoNombre;
}