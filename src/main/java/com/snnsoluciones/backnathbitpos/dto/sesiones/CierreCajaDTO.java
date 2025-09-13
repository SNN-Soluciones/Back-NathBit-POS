package com.snnsoluciones.backnathbitpos.dto.sesiones;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CierreCajaDTO {
    private BigDecimal montoCierre;
    private String observaciones;
}