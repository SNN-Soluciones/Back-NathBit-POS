package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;

// DTO para respuesta con información completa
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCABySDto {
    private Long id;
    private Long empresaId;
    private CodigoCABySDto codigoCabys;
    private String descripcionPersonalizada;
    private Boolean activo;
    private String createdAt;
}

