package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import lombok.Data;

@Data
public class EstadisticasEmpresaDTO {
    private Long empresaId;
    private String nombreEmpresa;
    private Long sucursalesActivas;
    private Long usuariosActivos;
    private PlanSuscripcion plan;
    private Integer limiteUsuarios;
    private Integer limiteSucursales;
    private Double porcentajeUsoUsuarios;
    private Double porcentajeUsoSucursales;
}