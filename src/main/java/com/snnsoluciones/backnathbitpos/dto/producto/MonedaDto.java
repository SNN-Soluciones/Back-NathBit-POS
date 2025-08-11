package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;

// DTO para respuesta completa
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonedaDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String simbolo;
    private Integer decimales;
    private Boolean activa;
    private Boolean esLocal;
    private Integer orden;
}
