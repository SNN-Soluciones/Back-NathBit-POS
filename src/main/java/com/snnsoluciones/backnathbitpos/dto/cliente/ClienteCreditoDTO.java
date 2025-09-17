package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClienteCreditoDTO {
    private Long id;
    private String nombre;
    private String identificacion;
    private Boolean permiteCredito;
    private BigDecimal limiteCredito;
    private BigDecimal saldoActual;
    private BigDecimal creditoDisponible;
    private Boolean bloqueadoPorMora;
    private String estadoCredito;
}