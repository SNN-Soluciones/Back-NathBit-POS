package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaForCreditResponse {
    // Datos de la factura original
    private Long id;
    private String consecutivo;
    private String clave;
    private String fechaEmision;

    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteIdentificacion;
    private String clienteTipoIdentificacion;
    private String emailReceptor;
    private Boolean clienteTieneExoneracion;
    private List<ExoneracionDto> exoneraciones;

    // Datos comerciales
    private String condicionVenta;
    private Integer plazoCredito;
    private String situacionComprobante;

    // Montos
    private BigDecimal totalComprobante;
    private BigDecimal montoAcreditado;
    private BigDecimal montoDisponibleParaAcreditar;
    private String moneda;
    private BigDecimal tipoCambio;

    // Descuento global (si lo hubiera)
    private BigDecimal descuentoGlobalPorcentaje;
    private BigDecimal montoDescuentoGlobal;
    private String motivoDescuentoGlobal;

    // Info adicional
    private String sucursalNombre;
    private Long sucursalId;
    private Long terminalId;
    private String terminalNombre;

    // Totales desglosados (para referencia)
    private BigDecimal totalServiciosGravados;
    private BigDecimal totalServiciosExentos;
    private BigDecimal totalServiciosExonerados;
    private BigDecimal totalMercanciasGravadas;
    private BigDecimal totalMercanciasExentas;
    private BigDecimal totalMercanciasExoneradas;
    private BigDecimal totalImpuesto;
    private BigDecimal totalDescuentos;
    private BigDecimal totalOtrosCargos;

    // Listas completas
    private List<DetalleForCreditDto> detalles;
    private List<OtroCargoDto> otrosCargos;
    private List<ResumenImpuestoDto> resumenImpuestos;

    // ==================== DTOs ANIDADOS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleForCreditDto {
        // Identificación
        private Long id;
        private Integer numeroLinea;
        private Long productoId;
        private String productoCodigo;
        private String productoNombre;

        // Datos del producto
        private String codigoCabys;
        private Boolean esServicio;
        private String unidadMedida;

        // Cantidades y precios
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;

        // Montos
        private BigDecimal montoTotal;
        private BigDecimal montoDescuento;
        private BigDecimal subtotal;
        private BigDecimal montoImpuesto;
        private BigDecimal montoTotalLinea;

        // Listas anidadas
        private List<DescuentoDto> descuentos;
        private List<ImpuestoDto> impuestos;

        // Campos para el formulario
        private BigDecimal cantidadAcreditar;
        private Boolean seleccionado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescuentoDto {
        private String codigoDescuento;
        private String codigoDescuentoOTRO;
        private String naturalezaDescuento;
        private BigDecimal porcentaje;
        private BigDecimal montoDescuento;
        private Integer orden;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpuestoDto {
        private String codigoImpuesto;
        private String codigoTarifaIVA;
        private BigDecimal tarifa;
        private BigDecimal baseImponible;
        private BigDecimal montoImpuesto;
        private Boolean tieneExoneracion;
        private BigDecimal montoExoneracion;
        private BigDecimal impuestoNeto;
        private ExoneracionDto exoneracion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExoneracionDto {
        private String tipoDocumentoEX;
        private String codigoInstitucion;
        private String numeroDocumentoEX;
        private String fechaEmisionExoneracion;
        private String institucionOtorgante;
        private BigDecimal porcentajeExonerado;
        private String articulo;
        private String inciso;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtroCargoDto {
        private String tipoDocumentoOC;
        private String tipoDocumentoOTROS;
        private String nombreCargo;
        private BigDecimal porcentaje;
        private BigDecimal montoCargo;
        // Campos para terceros
        private String terceroTipoIdentificacion;
        private String terceroNumeroIdentificacion;
        private String terceroNombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenImpuestoDto {
        private String codigoImpuesto;
        private String codigoTarifaIVA;
        private BigDecimal totalMontoImpuesto;
        private BigDecimal totalBaseImponible;
        private BigDecimal totalMontoExoneracion;
        private BigDecimal totalImpuestoNeto;
        private Integer cantidadLineas;
    }
}