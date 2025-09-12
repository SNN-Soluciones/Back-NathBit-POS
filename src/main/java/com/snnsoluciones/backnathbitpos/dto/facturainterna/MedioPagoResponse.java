package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedioPagoResponse {
    private Long id;
    private String tipoPago;
    private String descripcionPago;
    private BigDecimal monto;
    private String referencia;
    private BigDecimal cambio;
    private String banco;
    private String numeroAutorizacion;
}