package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO completo para reimpresión de facturas electrónicas
 * Contiene todos los datos necesarios para generar el ticket ESC/POS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaReimpresionDto {

    // ========== IDENTIFICACIÓN DEL DOCUMENTO ==========
    private String clave;
    private String consecutivo;
    private String tipoDocumento;        // "01", "03", "04", etc.
    private String tipoDocumentoNombre;  // "Factura Electrónica", "Nota de Crédito", etc.
    private String fechaEmision;
    private String estado;               // ACEPTADA, RECHAZADA, etc.
    private String condicionVenta;       // CONTADO, CREDITO
    private Integer plazoCredito;
    private String moneda;               // CRC, USD
    private BigDecimal tipoCambio;

    // ========== DATOS DEL EMISOR ==========
    private String empresaNombre;
    private String empresaNombreComercial;
    private String empresaCedula;
    private String empresaTelefono;
    private String empresaCorreo;
    private String empresaDireccion;
    private String sucursalNombre;

    // ========== DATOS DEL CLIENTE/RECEPTOR ==========
    private String clienteNombre;
    private String clienteCedula;
    private String clienteEmail;
    private String clienteTelefono;
    private String clienteDireccion;
    private String clienteActividadEconomica;

    // ========== DETALLES/LÍNEAS ==========
    private List<DetalleReimpresionDto> detalles;

    // ========== OTROS CARGOS (Impuesto servicio, etc.) ==========
    private List<OtroCargoReimpresionDto> otrosCargos;

    // ========== EXONERACIONES (resumen) ==========
    private List<ExoneracionReimpresionDto> exoneraciones;

    // ========== INFORMACIÓN DE REFERENCIA (NC/ND) ==========
    private List<ReferenciaReimpresionDto> informacionReferencia;

    // ========== MEDIOS DE PAGO ==========
    private List<MedioPagoReimpresionDto> mediosPago;

    // ========== TOTALES ==========
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
    private BigDecimal vuelto;

    // ========== HACIENDA ==========
    private String mensajeHacienda;

    // ========== DTOs INTERNOS ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleReimpresionDto {
        private Integer numeroLinea;
        private String codigo;           // Código del producto
        private String descripcion;
        private String descripcionPersonalizada;
        private BigDecimal cantidad;
        private String unidadMedida;
        private BigDecimal precioUnitario;
        private BigDecimal montoDescuento;
        private BigDecimal subtotal;
        private BigDecimal montoImpuesto;
        private BigDecimal montoTotalLinea;
        
        // Datos de exoneración de la línea (si aplica)
        private Boolean tieneExoneracion;
        private BigDecimal montoExoneracion;
        private String institucionExoneracion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtroCargoReimpresionDto {
        private String tipoDocumento;    // "06" = Impuesto servicio
        private String detalle;          // "Impuesto de Servicio 10%"
        private BigDecimal porcentaje;
        private BigDecimal monto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExoneracionReimpresionDto {
        private String tipoDocumento;
        private String numeroDocumento;
        private String nombreInstitucion;
        private String fechaEmision;
        private BigDecimal porcentajeExoneracion;
        private BigDecimal montoExoneracion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenciaReimpresionDto {
        private String tipoDocumento;        // "01", "03", "04"
        private String tipoDocumentoNombre;  // "Factura Electrónica"
        private String numero;               // Clave o consecutivo
        private String fechaEmision;
        private String codigo;               // "01" = Anula, "02" = Corrige
        private String codigoNombre;         // "Anula Documento"
        private String razon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedioPagoReimpresionDto {
        private String codigo;           // "01", "02", etc.
        private String nombre;           // "Efectivo", "Tarjeta", etc.
        private BigDecimal monto;
        private String referencia;
        private String banco;
    }
}