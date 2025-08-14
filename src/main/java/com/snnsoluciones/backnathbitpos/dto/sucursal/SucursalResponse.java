package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SucursalResponse {
    private Long id;
    private String nombre;
    private String numeroSucursal;
    private String telefono;
    private String email;
    private Boolean activa;
    private Long empresaId;
    private ModoFacturacion modoFacturacion = ModoFacturacion.ELECTRONICO;
    private String empresaNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}