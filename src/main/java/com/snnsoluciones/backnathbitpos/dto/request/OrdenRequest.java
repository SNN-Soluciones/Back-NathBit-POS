package com.snnsoluciones.backnathbitpos.dto.request;

import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para crear o actualizar una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para crear o actualizar una orden")
public class OrdenRequest {

  @NotNull(message = "El tipo de orden es requerido")
  @Schema(description = "Tipo de orden", example = "MESA")
  private TipoOrden tipo;

  @Schema(description = "ID de la mesa (requerido para ordenes tipo MESA)")
  private UUID mesaId;

  @Schema(description = "ID del cliente (opcional)")
  private UUID clienteId;

  @Schema(description = "ID del mesero asignado")
  private UUID meseroId;

  @Min(value = 1, message = "La cantidad de personas debe ser al menos 1")
  @Schema(description = "Cantidad de personas", example = "4")
  private Integer cantidadPersonas;

  @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
  @Schema(description = "Observaciones generales de la orden")
  private String observaciones;

  // Campos para delivery/para llevar
  @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
  @Schema(description = "Nombre del cliente (para delivery/llevar)")
  private String nombreClienteDelivery;

  @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
  @Schema(description = "Nombre del cliente (para órdenes sin cliente registrado)")
  private String nombreCliente;

  @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
  @Schema(description = "Teléfono de contacto (para delivery/llevar)")
  private String telefonoDelivery;

  @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
  @Schema(description = "Dirección de entrega (para delivery)")
  private String direccionDelivery;

  @Schema(description = "Hora estimada de entrega (para delivery/llevar)")
  private LocalDateTime horaEntregaEstimada;
}