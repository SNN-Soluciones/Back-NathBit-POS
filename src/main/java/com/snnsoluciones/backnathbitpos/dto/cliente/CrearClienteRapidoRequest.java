// CrearClienteRapidoRequest.java
package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CrearClienteRapidoRequest {
    @NotNull
    private Long empresaId;
    
    @NotNull
    private TipoIdentificacion tipoIdentificacion;
    
    @NotNull
    private String numeroIdentificacion;
    
    @NotNull
    private String nombre;
    
    private String email;
    private String telefono;
}