package com.snnsoluciones.backnathbitpos.dto.plataforma;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActualizarPlataformaRequest {
    
    @NotBlank(message = "Nombre requerido")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;
    
    @NotNull(message = "Porcentaje de incremento requerido")
    @DecimalMin(value = "0.00", message = "Porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "Porcentaje no puede exceder 100%")
    private BigDecimal porcentajeIncremento;
    
    @Size(max = 7, message = "Color hex debe ser formato #RRGGBB")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color debe ser formato hexadecimal #RRGGBB")
    private String colorHex;
    
    @Size(max = 50, message = "Icono máximo 50 caracteres")
    private String icono;
    
    @Size(max = 500, message = "Descripción máximo 500 caracteres")
    private String descripcion;
}