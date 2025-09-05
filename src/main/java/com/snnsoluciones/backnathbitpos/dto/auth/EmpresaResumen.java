package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class EmpresaResumen {
    private Long id;
    private String nombre;           // Nombre Razón Social
    private String nombreComercial;
    private String email;
    private String identificacion;
    private String logo;
    private boolean activa;
}