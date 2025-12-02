package com.snnsoluciones.backnathbitpos.dto.orden;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para mostrar el estado de pagos de una orden
 * Útil para la UI que muestra qué items están pagados/pendientes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenEstadoPagosResponse {

    private Long ordenId;
    private String ordenNumero;
    private String mesaCodigo;
    private String estado;

    // ===== TOTALES =====
    
    private BigDecimal totalOrden;
    private BigDecimal totalPagado;
    private BigDecimal totalPendiente;
    private BigDecimal porcentajePagado;

    // ===== ITEMS =====
    
    private List<ItemEstadoPagoDTO> items;
    private Integer itemsTotales;
    private Integer itemsPagados;
    private Integer itemsPendientes;

    // ===== FACTURAS EMITIDAS =====
    
    private List<FacturaResumenDTO> facturasEmitidas;
    private Integer totalFacturasEmitidas;

    // ===== INNER CLASSES =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemEstadoPagoDTO {
        private Long itemId;
        private Long productoId;
        private String productoNombre;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal total;
        private String estadoPago; // PENDIENTE, PAGADO
        private LocalDateTime fechaPago;
        private String tipoDocumentoPago; // TI, FI, TE, FE
        private Long facturaId;
        private String facturaNumero;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaResumenDTO {
        private Long id;
        private String tipo; // ELECTRONICA, INTERNA
        private String tipoDocumento; // TI, FI, TE, FE
        private String numero;
        private BigDecimal total;
        private LocalDateTime fecha;
        private Integer cantidadItems;
    }
}