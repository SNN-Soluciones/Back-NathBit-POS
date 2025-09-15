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

    // Totales - DEPRECADOS (mantener por compatibilidad)
    @Deprecated
    private BigDecimal totalServiciosGravados;
    @Deprecated
    private BigDecimal totalServiciosExentos;
    @Deprecated
    private BigDecimal totalServiciosExonerados;
    @Deprecated
    private BigDecimal totalMercanciasGravadas;
    @Deprecated
    private BigDecimal totalMercanciasExentas;
    @Deprecated
    private BigDecimal totalMercanciasExoneradas;
    @Deprecated
    private BigDecimal totalGravado;
    @Deprecated
    private BigDecimal totalExento;
    @Deprecated
    private BigDecimal totalExonerado;
    @Deprecated
    private BigDecimal totalVenta;
    @Deprecated
    private BigDecimal totalDescuentos;
    @Deprecated
    private BigDecimal totalVentaNeta;
    @Deprecated
    private BigDecimal totalImpuesto;
    @Deprecated
    private BigDecimal totalOtrosCargos;
    @Deprecated
    private BigDecimal totalComprobante;

    // NUEVO: Resumen estructurado según Hacienda
    private ResumenFacturaDto resumenFactura;

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
        // Agregar campos de ubicación
        private String provincia;
        private String canton;
        private String distrito;
        private String otrasSenas;
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
        // Impuestos
        private List<ImpuestoDto> impuestos;
        // Exoneraciones
        private ExoneracionDto exoneracion;
        private BigDecimal montoTotalLinea;
    }

    @Data
    public static class ResumenFacturaDto {
        private BigDecimal totalServiciosGravados = BigDecimal.ZERO;
        private BigDecimal totalServiciosExentos = BigDecimal.ZERO;
        private BigDecimal totalServiciosExonerados = BigDecimal.ZERO;
        private BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
        private BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
        private BigDecimal totalMercanciasExoneradas = BigDecimal.ZERO;
        private BigDecimal totalGravado = BigDecimal.ZERO;
        private BigDecimal totalExento = BigDecimal.ZERO;
        private BigDecimal totalExonerado = BigDecimal.ZERO;
        private BigDecimal totalVenta = BigDecimal.ZERO;
        private BigDecimal totalDescuentos = BigDecimal.ZERO;
        private BigDecimal totalVentaNeta = BigDecimal.ZERO;
        private BigDecimal totalImpuesto = BigDecimal.ZERO;
        private BigDecimal totalIVADevuelto = BigDecimal.ZERO;
        private BigDecimal totalOtrosCargos = BigDecimal.ZERO;
        private BigDecimal totalComprobante = BigDecimal.ZERO;
    }

    @Data
    public static class ImpuestoDto {
        private String codigo; // 01=IVA, 02=Selectivo, etc
        private String codigoTarifa; // 01=0%, 02=1%, 04=2%, 08=4%, etc
        private BigDecimal tarifa;
        private BigDecimal monto;
        private BigDecimal montoExportacion;
    }

    @Data
    public static class ExoneracionDto {
        private String tipoDocumento;
        private String numeroDocumento;
        private String nombreInstitucion;
        private LocalDateTime fechaEmision;
        private BigDecimal porcentajeExoneracion;
        private BigDecimal montoExoneracion;
    }
}