package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaInternaListResponse {
    private Long id;
    private String numero;
    private LocalDateTime fecha;
    private String clienteNombre;
    private BigDecimal total;
    private String estado;
    private String formaPago;
}