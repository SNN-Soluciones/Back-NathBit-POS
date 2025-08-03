package com.snnsoluciones.backnathbitpos.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrdenDetalle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta con información del detalle de orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con información del detalle de orden")
public class OrdenDetalleResponse {

    @Schema(description = "ID único del detalle")
    private UUID id;

    @Schema(description = "Número de línea en la orden", example = "1")
    private Integer numeroLinea;

    // Información del producto
    @Schema(description = "ID del producto")
    private UUID productoId;

    @Schema(description = "Código del producto", example = "PROD001")
    private String productoCodigo;

    @Schema(description = "Nombre del producto", example = "Hamburguesa Clásica")
    private String productoNombre;

    @Schema(description = "Categoría del producto", example = "Platos Principales")
    private String productoCategoria;

    // Cantidades y precios
    @Schema(description = "Cantidad ordenada", example = "2.000")
    private BigDecimal cantidad;

    @Schema(description = "Precio unitario", example = "5500.00")
    private BigDecimal precioUnitario;

    @Schema(description = "Porcentaje de descuento aplicado", example = "10.00")
    private BigDecimal porcentajeDescuento;

    @Schema(description = "Monto del descuento", example = "550.00")
    private BigDecimal montoDescuento;

    @Schema(description = "Motivo del descuento")
    private String motivoDescuento;

    // Impuestos
    @Schema(description = "Tarifa de IVA aplicada", example = "13.00")
    private BigDecimal tarifaIva;

    @Schema(description = "Monto del IVA", example = "643.50")
    private BigDecimal montoIva;

    // Totales
    @Schema(description = "Subtotal (cantidad x precio unitario)", example = "11000.00")
    private BigDecimal subtotal;

    @Schema(description = "Total de la línea", example = "11093.50")
    private BigDecimal total;

    // Estado y tiempos
    @Schema(description = "Estado del detalle")
    private EstadoOrdenDetalle estado;

    @Schema(description = "Fecha y hora del pedido")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaPedido;

    @Schema(description = "Fecha y hora cuando se preparó")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaPreparado;

    @Schema(description = "Fecha y hora cuando se sirvió")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaServido;

    @Schema(description = "Observaciones del detalle")
    private String observaciones;

    @Schema(description = "Indica si el detalle está cancelado")
    private Boolean cancelado;

    @Schema(description = "Motivo de cancelación")
    private String motivoCancelacion;
}