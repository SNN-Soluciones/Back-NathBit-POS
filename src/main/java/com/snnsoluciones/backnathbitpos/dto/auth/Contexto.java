package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Contexto {
    private EmpresaResumen empresa;
    private SucursalResumen sucursal;
}