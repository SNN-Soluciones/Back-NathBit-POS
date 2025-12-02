package com.snnsoluciones.backnathbitpos.dto.orden;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response después de procesar un pago parcial
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoParcialResponse {

    // ===== DATOS DE LA FACTURA GENERADA =====
    
    /**
     * Tipo de documento generado: TI, FI, TE, FE
     */
    private String tipoDocumentoGenerado;

    /**
     * ID de la factura electrónica (si aplica)
     */
    private Long facturaId;

    /**
     * Consecutivo de factura electrónica (si aplica)
     */
    private String consecutivo;

    /**
     * Clave de 50 dígitos (solo para documentos electrónicos)
     */
    private String clave;

    /**
     * ID de la factura interna (si aplica)
     */
    private Long facturaInternaId;

    /**
     * Número de factura interna (si aplica)
     */
    private String numeroInterno;

    // ===== MONTOS =====

    /**
     * Total facturado en este pago parcial
     */
    private BigDecimal totalFacturado;

    /**
     * Vuelto (si pagó en efectivo)
     */
    private BigDecimal vuelto;

    /**
     * Total pendiente que queda en la orden
     */
    private BigDecimal totalPendienteOrden;

    /**
     * Total ya pagado de la orden (acumulado)
     */
    private BigDecimal totalPagadoOrden;

    // ===== ITEMS =====

    /**
     * IDs de los items que se marcaron como pagados
     */
    private List<Long> itemsPagados;

    /**
     * Cantidad de items que quedan pendientes
     */
    private Integer itemsPendientesCount;

    // ===== ESTADO DE LA ORDEN =====

    /**
     * ID de la orden
     */
    private Long ordenId;

    /**
     * Número de la orden
     */
    private String ordenNumero;

    /**
     * true si la orden se cerró (todos los items pagados)
     */
    private Boolean ordenCerrada;

    /**
     * Nuevo estado de la orden
     */
    private String ordenEstado;

    /**
     * true si la mesa fue liberada
     */
    private Boolean mesaLiberada;

    /**
     * Código de la mesa (si aplica)
     */
    private String mesaCodigo;

    // ===== METADATA =====

    /**
     * Cantidad de facturas emitidas para esta orden (incluyendo esta)
     */
    private Integer totalFacturasEmitidas;

    /**
     * Fecha/hora del pago
     */
    private LocalDateTime fechaPago;

    /**
     * Mensaje informativo
     */
    private String mensaje;
}