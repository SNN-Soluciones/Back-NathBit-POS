package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoCABySCreateDto {
    @NotBlank(message = "El código es requerido")
    @Size(min = 13, max = 13, message = "El código debe tener 13 dígitos")
    private String codigo;
    
    @NotBlank(message = "La descripción es requerida")
    private String descripcion;
    
    @Pattern(regexp = "^(BIEN|SERVICIO)$", message = "El tipo debe ser BIEN o SERVICIO")
    private String tipo;
    
    private String impuestoSugerido;
}