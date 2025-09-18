package com.snnsoluciones.backnathbitpos.dto.pago;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PagoRequest {
    private String metodo; // EFECTIVO, TARJETA, etc.
    private BigDecimal monto;
}