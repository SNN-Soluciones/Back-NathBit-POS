package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmpresaResumen {
    private Long id;
    private String nombre;
    private String codigo;
}