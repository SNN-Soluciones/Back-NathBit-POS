package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear moneda (solo admin/root)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonedaCreateDto {
    @NotBlank(message = "El código es requerido")
    @Size(min = 3, max = 3, message = "El código debe tener 3 caracteres")
    @Pattern(regexp = "^[A-Z]{3}$", message = "El código debe ser 3 letras mayúsculas")
    private String codigo;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    private String nombre;
    
    @NotBlank(message = "El símbolo es requerido")
    @Size(max = 5, message = "El símbolo no puede exceder 5 caracteres")
    private String simbolo;
    
    @NotNull(message = "Los decimales son requeridos")
    @Min(value = 0, message = "Los decimales no pueden ser negativos")
    @Max(value = 4, message = "Los decimales no pueden exceder 4")
    private Integer decimales;
    
    @NotNull(message = "Debe indicar si es moneda local")
    private Boolean esLocal;
    
    private Integer orden;
}
