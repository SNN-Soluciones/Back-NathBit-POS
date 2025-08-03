package com.snnsoluciones.backnathbitpos.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta con información de la orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con información de la orden")
public class OrdenResponse {

  @Schema(description = "ID único de la orden")
  private UUID id;

  @Schema(description = "Número de orden", example = "M-20240115-00001")
  private String numeroOrden;

  @Schema(description = "Tipo de orden")
  private TipoOrden tipo;

  @Schema(description = "Estado actual de la orden")
  private EstadoOrden estado;

  // Información de mesa
  @Schema(description = "ID de la mesa")
  private UUID mesaId;

  @Schema(description = "Número de la mesa", example = "12")
  private String mesaNumero;

  @Schema(description = "Nombre de la mesa", example = "Mesa 12")
  private String mesaNombre;

  @Schema(description = "Zona de la mesa", example = "Terraza")
  private String mesaZona;

  // Información de cliente
  @Schema(description = "ID del cliente")
  private UUID clienteId;

  @Schema(description = "Nombre del cliente")
  private String clienteNombre;

  @Schema(description = "Identificación del cliente")
  private String clienteIdentificacion;

  // Información de mesero
  @Schema(description = "ID del mesero")
  private UUID meseroId;

  @Schema(description = "Nombre del mesero")
  private String meseroNombre;

  // Información de caja (si está pagada)
  @Schema(description = "ID de la caja")
  private UUID cajaId;

  @Schema(description = "Nombre de la caja")
  private String cajaNombre;

  // Fechas y tiempos
  @Schema(description = "Fecha y hora de creación de la orden")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime fechaOrden;

  @Schema(description = "Fecha y hora de última actualización")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;

  // Información general
  @Schema(description = "Cantidad de personas", example = "4")
  private Integer cantidadPersonas;

  @Schema(description = "Observaciones de la orden")
  private String observaciones;

  // Totales
  @Schema(description = "Subtotal antes de descuentos e impuestos", example = "10000.00")
  private BigDecimal subtotal;

  @Schema(description = "Total de descuentos aplicados", example = "1000.00")
  private BigDecimal totalDescuentos;

  @Schema(description = "Total de impuestos", example = "1300.00")
  private BigDecimal totalImpuestos;

  @Schema(description = "Total a pagar", example = "10300.00")
  private BigDecimal total;

  // Información de delivery/para llevar
  @Schema(description = "Nombre del cliente (delivery/llevar)")
  private String nombreClienteDelivery;

  @Schema(description = "Teléfono de contacto (delivery/llevar)")
  private String telefonoDelivery;

  @Schema(description = "Dirección de entrega (delivery)")
  private String direccionDelivery;

  @Schema(description = "Hora estimada de entrega")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime horaEntregaEstimada;

  // Detalles de la orden
  @Schema(description = "Lista de productos en la orden")
  private List<OrdenDetalleResponse> detalles;

  @Schema(description = "Cantidad total de productos")
  private Integer cantidadProductos;

  @Schema(description = "ID del tenant")
  private String tenantId;
}