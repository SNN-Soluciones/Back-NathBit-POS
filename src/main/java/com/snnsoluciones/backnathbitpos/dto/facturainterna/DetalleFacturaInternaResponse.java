package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaInternaResponse {
    private Long id;
    
    // Producto
    private Long productoId;
    private String codigoProducto;
    private String nombreProducto;
    
    // Cantidades y precios
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    
    private String notas;
}