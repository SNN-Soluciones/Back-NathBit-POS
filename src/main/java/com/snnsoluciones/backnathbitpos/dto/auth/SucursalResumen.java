package com.snnsoluciones.backnathbitpos.dto.auth;

import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SucursalResumen {
    private Long id;
    private String nombre;
    private String numeroSucursal;
    private ModoFacturacion modoFacturacion;
    private boolean activa;
}