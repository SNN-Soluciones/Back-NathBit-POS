package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.enums.MetodoImpresion;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.ModoImpresion;
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
    private MetodoImpresion metodoImpresion;
    private String empresaNombre;
    private ModoImpresion modoImpresion;
    private String ipOrquestador;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}