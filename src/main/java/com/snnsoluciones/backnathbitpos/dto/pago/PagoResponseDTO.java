package com.snnsoluciones.backnathbitpos.dto.pago;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoResponseDTO {
    private Long id;
    private String numeroRecibo;
    private BigDecimal monto;
    private String medioPago;
    private String referencia;
    private LocalDateTime fechaPago;
    private String clienteNombre;
    private String facturaConsecutivo;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoActual;
    private String cajero;
}