package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFacturaInternaRequest {

    // Contexto requerido
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;

    @NotNull(message = "El usuario es requerido")
    private Long usuarioId;

    // Cliente opcional
    private Long clienteId;
    private String nombreCliente;

    // Detalles
    @NotEmpty(message = "Debe incluir al menos un producto")
    private List<DetalleFacturaInternaRequest> detalles;

    // Totales
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    // Medios de pago
    @NotEmpty(message = "Debe incluir al menos un medio de pago")
    private List<MedioPagoInternoRequest> mediosPago;

    @DecimalMin(value = "0.0", message = "El pago recibido no puede ser negativo")
    private BigDecimal pagoRecibido;

    // Notas
    private String notas;
}