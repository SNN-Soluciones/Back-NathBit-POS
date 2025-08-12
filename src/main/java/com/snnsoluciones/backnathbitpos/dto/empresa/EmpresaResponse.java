package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmpresaResponse {
    private Long id;
    private String nombre;
    private String codigo;
    private TipoIdentificacion tipoIdentificacion;
    private String identificacion;
    private String direccion;
    private String telefono;
    private String email;
    private Boolean activa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}