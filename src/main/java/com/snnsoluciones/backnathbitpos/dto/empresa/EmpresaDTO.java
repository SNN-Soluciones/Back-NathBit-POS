package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmpresaDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String nombreComercial;
    private String cedulaJuridica;
    private String telefono;
    private String email;
    private String direccion;
    private TipoEmpresa tipo;
    private PlanSuscripcion plan;
    private Boolean activa;
    private Integer cantidadSucursales;
    private Integer cantidadUsuarios;
    private LocalDateTime createdAt;
}