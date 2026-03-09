package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaInternaResponse {
    private Long id;
    private String numero;
    private LocalDateTime fecha;

    // Empresa y sucursal
    private String empresaNombre;
    private String sucursalNombre;
    private String empresaCedula;   // ← agregar esta línea

    private String cajeroNombre;

    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteCedula;
    private String clienteEmail;

    // Totales
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal total;
    private String numeroViper;

    // Pago
    private BigDecimal pagoRecibido;
    private BigDecimal vuelto;

    // Estado
    private String estado;
    private String notas;

    // Detalles
    private List<DetalleFacturaInternaResponse> detalles;

    // Medios de pago (cuando es MIXTO)
    private List<MedioPagoResponse> mediosPago;

    private Long meseroId;
    private String meseroNombre;

    // Mesa
    private Long mesaId;
    private String mesaCodigo;

    // Impuesto de servicio
    private BigDecimal porcentajeServicio;
    private BigDecimal impuestoServicio;
}