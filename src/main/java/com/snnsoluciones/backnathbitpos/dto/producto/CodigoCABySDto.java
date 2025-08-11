package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;

// DTO para listados y búsquedas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoCABySDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private String tipo;
    private String impuestoSugerido;
    private Boolean activo;
}