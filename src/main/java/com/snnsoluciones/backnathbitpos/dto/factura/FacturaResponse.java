package com.snnsoluciones.backnathbitpos.dto.factura;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response completo de factura con todos los elementos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponse {

    private Long id;
    private String consecutivo;
    private String clave;
    private TipoDocumento tipoDocumento;
    private EstadoFactura estado;

    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteIdentificacion;

    // Moneda
    private Moneda moneda;
    private BigDecimal tipoCambio;

    // Totales
    private BigDecimal subtotal;
    private BigDecimal totalDescuentosLineas;
    private BigDecimal montoDescuentoGlobal;
    private BigDecimal totalDescuentos;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalImpuestos;
    private BigDecimal total;
    private BigDecimal totalMonedaLocal;

    // Detalles
    @Builder.Default
    private List<DetalleFacturaResponse> detalles = new ArrayList<>();

    // Otros cargos
    @Builder.Default
    private List<OtroCargoResponse> otrosCargos = new ArrayList<>();

    // Medios de pago
    @Builder.Default
    private List<MedioPagoResponse> mediosPago = new ArrayList<>();

    // Metadata
    private String fechaEmision;
    private String sucursalNombre;
    private String terminalNombre;
    private String cajeroNombre;
    private String situacionComprobante;

    // Hacienda
    private String mensajeHacienda;
    private String xmlFirmado;

    /**
     * DTO interno para detalles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleFacturaResponse {
        private Integer numeroLinea;
        private Long productoId;
        private String productoNombre;
        private String codigoCabys;
        private BigDecimal cantidad;
        private String unidadMedida;
        private BigDecimal precioUnitario;
        private BigDecimal totalDescuentosLinea;
        private BigDecimal subtotal;
        private String codigoTarifaIVA;
        private BigDecimal tasaImpuesto;
        private BigDecimal montoImpuesto;
        private BigDecimal total;
        private List<DescuentoResponse> descuentos;
    }

    /**
     * DTO interno para otros cargos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtroCargoResponse {
        private String tipoDocumentoOC;
        private String nombreCargo;
        private BigDecimal porcentaje;
        private BigDecimal montoCargo;
        private Integer numeroLinea;
    }

    /**
     * DTO interno para descuentos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescuentoResponse {
        private String codigoDescuento;
        private String descripcion;
        private BigDecimal porcentaje;
        private BigDecimal montoDescuento;
        private Integer orden;
    }

    /**
     * DTO interno para medios de pago
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedioPagoResponse {
        private String medioPago;
        private String descripcion;
        private BigDecimal monto;
        private String referencia;
    }
}