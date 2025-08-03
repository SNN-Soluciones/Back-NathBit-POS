package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambioPasswordRequest {
    @NotBlank
    private String passwordActual;
    
    @NotBlank
    @Size(min = 8)
    private String passwordNuevo;
    
    @NotBlank
    private String confirmarPassword;
}