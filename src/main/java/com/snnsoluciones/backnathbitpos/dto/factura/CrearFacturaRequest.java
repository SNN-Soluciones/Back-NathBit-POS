package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request para crear factura con todos los totales calculados por el frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFacturaRequest {
    
    // Identificación
    @NotNull(message = "Tipo de documento es requerido")
    private TipoDocumento tipoDocumento;
    
    private Long terminalId;

    private Long facturaReferenciaId;

    private String numeroViper;

    @Size(max = 100, message = "El nombre del receptor no puede exceder 100 caracteres")
    private String nombreReceptor;
    private String razonNotaCredito;

    @Builder.Default
    private boolean anulacionCompleta = false;

    private String emailReceptor;

    private String actividadReceptor;
    
    private Long sesionCajaId;
    
    @NotNull(message = "Usuario es requerido")
    private Long usuarioId;
    
    // Cliente (opcional para TE)
    private Long clienteId;

    private Long sucursalId;

    @DecimalMin(value = "0.00", message = "El vuelto no puede ser negativo")
    private BigDecimal vuelto;
    
    // Datos comerciales
    @NotBlank(message = "Condición de venta es requerida")
    private String condicionVenta;
    
    private Integer plazoCredito;
    
    @NotNull(message = "Moneda es requerida")
    private Moneda moneda;
    
    @NotNull(message = "Tipo de cambio es requerido")
    @DecimalMin(value = "0.01", message = "Tipo de cambio debe ser mayor a 0")
    private BigDecimal tipoCambio;
    
    @NotBlank(message = "Situación del comprobante es requerida")
    private String situacionComprobante;
    
    private String observaciones;
    
    // Descuento global
    private BigDecimal descuentoGlobalPorcentaje;
    private BigDecimal montoDescuentoGlobal;
    private String motivoDescuentoGlobal;
    
    // Detalles
    @NotEmpty(message = "Debe incluir al menos un detalle")
    @Valid
    private List<DetalleFacturaRequest> detalles;
    
    // Otros cargos
    @Valid
    private List<OtroCargoRequest> otrosCargos;
    
    // Medios de pago
    @NotEmpty(message = "Debe incluir al menos un medio de pago")
    @Valid
    private List<MedioPagoRequest> mediosPago;

    @NotBlank
    private String versionCatalogos; // ej: "MH-4.4-2025-08-21"
    
    // Resumen de impuestos
    @Valid
    private List<ResumenImpuestoRequest> resumenImpuestos;
    
    // ========== TOTALES CALCULADOS POR EL FRONTEND ==========
    
    // Totales por tipo
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalServiciosGravados;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalServiciosExentos;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalServiciosExonerados;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalServiciosNoSujetos;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMercanciasGravadas;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMercanciasExentas;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMercanciasExoneradas;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalMercanciasNoSujetas;
    
    // Totales generales
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalGravado;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalExento;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalExonerado;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalVenta;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalDescuentos;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalVentaNeta;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalImpuesto;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalIVADevuelto;
    
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal totalOtrosCargos;

    @NotNull @DecimalMin("0.00")
    private BigDecimal totalNoSujeto; // suma de los dos
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Total del comprobante debe ser mayor a 0")
    private BigDecimal totalComprobante;

    /**
     * Lista de información de referencia
     * OBLIGATORIO para:
     * - Nota de Crédito (NC)
     * - Nota de Débito (ND)
     * - Factura Electrónica de Compra (FEC)
     * - Recibo Electrónico de Pago (REP)
     * - Factura Electrónica cuando se usa código 11 en autorización/exoneración
     *
     * Máximo 10 referencias según v4.4
     */
    @Valid
    @Size(max = 10, message = "Máximo 10 referencias permitidas")
    private List<InformacionReferenciaDto> informacionReferencia;
}