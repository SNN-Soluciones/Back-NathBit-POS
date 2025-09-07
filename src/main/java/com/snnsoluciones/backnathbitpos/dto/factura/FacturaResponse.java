package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response completo de factura con TODOS los elementos del XML
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponse {
    // ===== IDENTIFICADORES =====
    private Long id;
    private String clave;
    private String consecutivo;
    private TipoDocumento tipoDocumento;
    private EstadoFactura estado;

    // ===== EMISOR =====
    private EmisorDto emisor;

    // ===== RECEPTOR =====
    private ReceptorDto receptor;

    // ===== DATOS COMERCIALES =====
    private CondicionVenta condicionVenta;
    private Integer plazoCredito;
    private Moneda moneda;
    private BigDecimal tipoCambio;
    private String fechaEmision;
    private SituacionDocumento situacionComprobante;
    private String observaciones;

    // ===== DETALLES =====
    @Builder.Default
    private List<DetalleFacturaDto> detalles = new ArrayList<>();

    // ===== OTROS CARGOS =====
    @Builder.Default
    private List<OtroCargoDto> otrosCargos = new ArrayList<>();

    // ===== REFERENCIAS (NC, ND, etc) =====
    @Builder.Default
    private List<InformacionReferenciaDto> referencias = new ArrayList<>();

    // ===== RESUMEN FACTURA =====
    private ResumenFacturaDto resumen;

    // ===== MEDIOS DE PAGO =====
    @Builder.Default
    private List<MedioPagoDto> mediosPago = new ArrayList<>();

    // ===== METADATA SISTEMA =====
    private String sucursalNombre;
    private String terminalNombre;
    private String cajeroNombre;
    private Long sesionCajaId;

    // ===== HACIENDA =====
    private String mensajeHacienda;
    private String xmlFirmado;
    private String xmlRespuesta;

    // ========== DTOs INTERNOS ==========

    /**
     * Datos del Emisor
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmisorDto {
        private String nombre;
        private TipoIdentificacion tipoIdentificacion;
        private String numeroIdentificacion;
        private UbicacionDto ubicacion;
        private TelefonoDto telefono;
        private String correoElectronico;
        private String codigoActividad;
        private String proveedorSistemas; // Cédula del proveedor del sistema
    }

    /**
     * Datos del Receptor
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceptorDto {
        private Long clienteId;
        private String nombre;
        private TipoIdentificacion tipoIdentificacion;
        private String numeroIdentificacion;
        private String correoElectronico;
        private String codigoActividadReceptor;
        private UbicacionDto ubicacion;
        private TelefonoDto telefono;
    }

    /**
     * Ubicación
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UbicacionDto {
        private String provincia;
        private String canton;
        private String distrito;
        private String barrio;
        private String otrasSenas;
    }

    /**
     * Teléfono
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelefonoDto {
        private String codigoPais;
        private String numTelefono;
    }

    /**
     * Detalle de línea de factura
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleFacturaDto {
        private Integer numeroLinea;
        private String codigoCABYS;
        private CodigoComercialDto codigoComercial;
        private BigDecimal cantidad;
        private UnidadMedida unidadMedida;
        private String detalle;
        private BigDecimal precioUnitario;
        private BigDecimal montoTotal;
        private BigDecimal subTotal;
        private BigDecimal baseImponible;

        // Descuentos
        @Builder.Default
        private List<DescuentoDto> descuentos = new ArrayList<>();
        private BigDecimal montoTotalLinea;

        // Impuestos
        @Builder.Default
        private List<ImpuestoDto> impuestos = new ArrayList<>();

        // Indicadores
        private Boolean esServicio;
        private BigDecimal impuestoAsumidoEmisorFabrica;
        private BigDecimal impuestoNeto;

        // Producto
        private Long productoId;
        private String productoNombre;
        private Boolean seleccionado; // Para UI
    }

    /**
     * Código comercial
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodigoComercialDto {
        private String tipo; // 01: EAN13, 02: EAN8, 03: UPC-A, 04: Código interno
        private String codigo;
    }

    /**
     * Descuento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescuentoDto {
        private String codigoDescuento;
        private String descripcion;
        private BigDecimal porcentaje;
        private BigDecimal montoDescuento;
        private Integer orden;
    }

    /**
     * Impuesto
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpuestoDto {
        private TipoImpuesto codigo;
        private CodigoTarifaIVA codigoTarifaIVA;
        private BigDecimal tarifa;
        private BigDecimal monto;
        private ExoneracionDto exoneracion;
    }

    /**
     * Exoneración
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExoneracionDto {
        private String tipoDocumentoEX;
        private String numeroDocumentoEX;
        private String institucionOtorgante;
        private String fechaEmisionExoneracion;
        private BigDecimal porcentajeExonerado;
        private BigDecimal montoExoneracion;
        private BigDecimal codigoInstitucion;
        private String articulo;
        private String inciso;
    }

    /**
     * Otro cargo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtroCargoDto {
        private String tipoDocumentoOC;
        private String detalle;
        private String nombreCargo;
        private BigDecimal porcentaje;
        private BigDecimal montoCargo;
        private Integer numeroLinea;
    }

    /**
     * Información de referencia (para NC, ND, etc)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InformacionReferenciaDto {
        private TipoDocumento tipoDoc;
        private String numero;
        private String fechaEmision;
        private CodigoReferencia codigo;
        private String razon;
    }

    /**
     * Resumen de factura
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenFacturaDto {
        // Totales por tipo
        private BigDecimal totalServGravados;
        private BigDecimal totalServExentos;
        private BigDecimal totalServExonerado;
        private BigDecimal totalServNoSujeto;

        private BigDecimal totalMercanciasGravadas;
        private BigDecimal totalMercanciasExentas;
        private BigDecimal totalMercExonerada;
        private BigDecimal totalMercNoSujeta;

        // Totales generales
        private BigDecimal totalGravado;
        private BigDecimal totalExento;
        private BigDecimal totalExonerado;
        private BigDecimal totalNoSujeto;

        private BigDecimal totalVenta;
        private BigDecimal totalDescuentos;
        private BigDecimal totalVentaNeta;
        private BigDecimal totalImpuesto;
        private BigDecimal totalImpAsumEmisorFabrica;
        private BigDecimal totalIVADevuelto;
        private BigDecimal totalOtrosCargos;
        private BigDecimal totalComprobante;

        // Desglose de impuestos
        @Builder.Default
        private List<TotalDesgloseImpuestoDto> totalDesgloseImpuesto = new ArrayList<>();
    }

    /**
     * Total desglose impuesto
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalDesgloseImpuestoDto {
        private TipoImpuesto codigo;
        private CodigoTarifaIVA codigoTarifaIVA;
        private BigDecimal totalMontoImpuesto;
    }

    /**
     * Medio de pago
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedioPagoDto {
        private MedioPago tipoMedioPago;
        private BigDecimal totalMedioPago;
        private String referencia;
        private String descripcion;
    }
}