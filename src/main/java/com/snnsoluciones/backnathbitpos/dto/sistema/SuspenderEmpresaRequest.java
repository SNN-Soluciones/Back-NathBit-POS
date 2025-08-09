package com.snnsoluciones.backnathbitpos.dto.sistema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspenderEmpresaRequest {
    
    @NotBlank(message = "El motivo es requerido")
    private String motivo;
    
    private Boolean notificarUsuarios;
    
    private Integer diasSuspension; // null = indefinida
}
