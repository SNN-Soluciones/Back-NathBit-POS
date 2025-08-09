package com.snnsoluciones.backnathbitpos.dto.sucursal;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SucursalResponse {
    private Long id;
    private String nombre;
    private String codigo;
    private String direccion;
    private String telefono;
    private String email;
    private Boolean activa;
    private Long empresaId;
    private String empresaNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}