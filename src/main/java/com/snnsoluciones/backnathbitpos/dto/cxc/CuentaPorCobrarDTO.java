package com.snnsoluciones.backnathbitpos.dto.cxc;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CuentaPorCobrarDTO {
    private Long id;
    private Long facturaId;
    private String facturaConsecutivo;
    private Long clienteId;
    private String clienteNombre;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal montoOriginal;
    private BigDecimal saldo;
    private String estado;
    private Integer diasMora;
}