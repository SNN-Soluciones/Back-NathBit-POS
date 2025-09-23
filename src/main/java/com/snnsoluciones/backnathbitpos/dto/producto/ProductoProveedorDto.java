package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorDto {
    private Long id;
    
    // Información del producto
    private Long productoId;
    private String productoNombre;
    private String productoCodigoInterno;
    
    // Información del proveedor
    private Long proveedorId;
    private String proveedorNombre;
    private String proveedorIdentificacion;
    
    // Datos de la relación
    private String codigoProveedor;
    private String descripcionProveedor;
    private String unidadCompra;
    private Integer factorConversion;
    private BigDecimal precioCompra;
    private String observaciones;
    private Boolean activo;
    
    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}