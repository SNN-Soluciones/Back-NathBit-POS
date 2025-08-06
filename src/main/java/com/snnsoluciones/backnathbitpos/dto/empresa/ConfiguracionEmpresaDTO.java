package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import lombok.Data;

import java.util.Map;

@Data
public class ConfiguracionEmpresaDTO {
    private Long empresaId;
    private Map<String, Object> configuracion;
    private TipoEmpresa tipo;
    private PlanSuscripcion plan;
    private Integer limiteUsuarios;
    private Integer limiteSucursales;
}