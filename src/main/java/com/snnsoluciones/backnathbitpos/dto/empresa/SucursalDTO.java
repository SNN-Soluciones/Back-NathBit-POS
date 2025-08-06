package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SucursalDTO {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private Boolean esPrincipal;
    private Boolean activa;
    private Integer cantidadUsuarios;
    private LocalDateTime createdAt;
}