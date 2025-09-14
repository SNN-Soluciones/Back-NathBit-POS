package com.snnsoluciones.backnathbitpos.dto.compra;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// DTO para datos extraídos del XML
@Data
public class FacturaXmlDto {
    private String clave;
    private String numeroConsecutivo;
    private LocalDateTime fechaEmision;
    private EmisorDto emisor;
    private String condicionVenta;
    private Integer plazoCredito;
    private String medioPago;
    private String codigoMoneda;
    private BigDecimal tipoCambio;
    
    // Totales
    private BigDecimal totalServiciosGravados;
    private BigDecimal totalServiciosExentos;
    private BigDecimal totalServiciosExonerados;
    private BigDecimal totalMercanciasGravadas;
    private BigDecimal totalMercanciasExentas;
    private BigDecimal totalMercanciasExoneradas;
    private BigDecimal totalGravado;
    private BigDecimal totalExento;
    private BigDecimal totalExonerado;
    private BigDecimal totalVenta;
    private BigDecimal totalDescuentos;
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuesto;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalComprobante;
    
    private List<DetalleDto> detalles;
    private String xmlOriginal;
    
    @Data
    public static class EmisorDto {
        private String tipoIdentificacion;
        private String numeroIdentificacion;
        private String nombre;
        private String nombreComercial;
        private String correoElectronico;
        private String telefono;
    }
    
    @Data
    public static class DetalleDto {
        private Integer numeroLinea;
        private String codigo;
        private String codigoCabys;
        private BigDecimal cantidad;
        private String unidadMedida;
        private String detalle;
        private BigDecimal precioUnitario;
        private BigDecimal montoTotal;
        private BigDecimal montoDescuento;
        private String naturalezaDescuento;
        private BigDecimal subTotal;
        private String codigoImpuesto;
        private String codigoTarifa;
        private BigDecimal tarifa;
        private BigDecimal montoImpuesto;
        private Boolean tieneExoneracion = false;
        private BigDecimal montoExoneracion;
        private BigDecimal montoTotalLinea;
    }
}

