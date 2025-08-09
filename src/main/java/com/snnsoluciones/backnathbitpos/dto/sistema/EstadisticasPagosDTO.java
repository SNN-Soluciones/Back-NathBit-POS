package com.snnsoluciones.backnathbitpos.dto.sistema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasPagosDTO {
    private Double totalFacturado;
    private Double totalCobrado;
    private Double totalPendiente;
    private Integer empresasAlDia;
    private Integer empresasConRetraso;
    private Integer empresasSuspendidas;
    private Double tasaMorosidad;
}