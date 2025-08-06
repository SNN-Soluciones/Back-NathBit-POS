package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.Data;

import java.util.Map;

@Data
public class ConfiguracionSucursalDTO {
    private Long sucursalId;
    private Map<String, Object> configuracion;
    private Boolean esPrincipal;
}