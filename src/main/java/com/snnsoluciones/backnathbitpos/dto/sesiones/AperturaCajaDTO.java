package com.snnsoluciones.backnathbitpos.dto.sesiones;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AperturaCajaDTO {
    private Long terminalId;
    private BigDecimal montoInicial;
    private String observaciones;
}