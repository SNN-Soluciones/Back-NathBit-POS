package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFacturaInternaRequest {

    private LocalDateTime fechaEmision;


    // Contexto requerido
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;

    @NotNull(message = "El usuario es requerido")
    private Long usuarioId;

    private Long sesionCajaId;

    private Long ordenId;

    // Cliente opcional
    private Long clienteId;
    private String nombreCliente;

    // Detalles
    @NotEmpty(message = "Debe incluir al menos un producto")
    private List<DetalleFacturaInternaRequest> detalles;

    // Totales
    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    private BigDecimal descuentoPorcentaje;

    private String numeroViper;


    // Medios de pago
    @NotEmpty(message = "Debe incluir al menos un medio de pago")
    private List<MedioPagoInternoRequest> mediosPago;

    @DecimalMin(value = "0.0", message = "El pago recibido no puede ser negativo")
    private BigDecimal pagoRecibido;

    // Notas
    private String notas;

    private Long meseroId;
    private Long mesaId;

    // Impuesto de servicio (solo si viene, si no viene = 0)
    @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje no puede ser mayor a 100")
    private BigDecimal porcentajeServicio;

    private String condicionVenta; // "CONTADO" o "CREDITO"
    private Integer plazoCredito;
}