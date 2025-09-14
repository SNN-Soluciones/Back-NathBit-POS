package com.snnsoluciones.backnathbitpos.dto.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

// DTO de respuesta
@Data
public class CompraDto {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private Long sucursalId;
    private String sucursalNombre;
    private Long proveedorId;
    private String proveedorNombre;
    private String proveedorIdentificacion;
    private String tipoCompra;
    private String tipoDocumentoHacienda;
    private String numeroDocumento;
    private String claveHacienda;
    private String consecutivoHacienda;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaRecepcion;
    private String condicionVenta;
    private Integer plazoCredito;
    private String medioPago;
    private String moneda;
    private BigDecimal tipoCambio;
    
    // Totales
    private BigDecimal totalGravado;
    private BigDecimal totalExento;
    private BigDecimal totalExonerado;
    private BigDecimal totalVenta;
    private BigDecimal totalDescuentos;
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuesto;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalComprobante;
    
    // Estado
    private String estado;
    private String estadoHacienda;
    private String mensajeHacienda;
    
    private String observaciones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<CompraDetalleDto> detalles;
}