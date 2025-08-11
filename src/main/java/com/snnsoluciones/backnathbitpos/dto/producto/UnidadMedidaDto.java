package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidadMedidaDto {
    private Long id;
    private String codigo;
    private String simbolo;
    private String descripcion;
    private Boolean activo;
}
