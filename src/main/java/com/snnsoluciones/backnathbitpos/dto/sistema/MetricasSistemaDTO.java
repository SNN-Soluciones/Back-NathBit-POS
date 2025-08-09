package com.snnsoluciones.backnathbitpos.dto.sistema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasSistemaDTO {
    private Long totalEmpresas;
    private Long empresasActivas;
    private Long totalSucursales;
    private Long sucursalesActivas;
    private Long totalUsuarios;
    private Long usuariosActivos;
    private Long transaccionesHoy;
    private Double ventasHoy;
    private Double promedioVentaPorEmpresa;
    private Integer diasOperacion;
}