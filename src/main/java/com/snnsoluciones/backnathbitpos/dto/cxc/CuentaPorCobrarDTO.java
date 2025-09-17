package com.snnsoluciones.backnathbitpos.dto.cxc;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CuentaPorCobrarDTO {
    private Long id;
    private Long facturaId;
    private String numeroFactura;
    private Long clienteId;
    private String clienteNombre;
    private String clienteIdentificacion;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal montoOriginal;
    private BigDecimal saldo;
    private String estado; // VIGENTE, VENCIDA, PAGADA, PARCIAL
    private Integer diasMora;
    private BigDecimal montoAbonado;
}