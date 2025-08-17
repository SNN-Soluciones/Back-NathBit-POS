package com.snnsoluciones.backnathbitpos.dto.cabys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCABySDto {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    
    // Datos del CAByS
    private Long cabysId;
    private String cabysCodigo;
    private String cabysDescripcion;
    private String cabysImpuesto;
    
    private Boolean activo;
    private LocalDateTime createdAt;
}