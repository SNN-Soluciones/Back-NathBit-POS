package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResumenDTO {
    private Long totalClientes;
    private Long clientesActivos;
    private Long clientesConExoneracion;
    private Long clientesConCredito;
    private Long clientesConUbicacion;
    private BigDecimal porcentajeConExoneracion;
    private BigDecimal porcentajeConCredito;
}