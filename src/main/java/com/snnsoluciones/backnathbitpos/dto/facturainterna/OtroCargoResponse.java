package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtroCargoResponse {
    private Long id;
    private String tipoCargo;
    private String descripcion;
    private BigDecimal porcentaje;
    private BigDecimal monto;
    private Boolean aplicadoAutomaticamente;
}