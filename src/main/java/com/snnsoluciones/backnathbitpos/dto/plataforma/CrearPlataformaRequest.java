package com.snnsoluciones.backnathbitpos.dto.plataforma;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrearPlataformaRequest {
    
    @NotBlank(message = "Código requerido")
    @Size(max = 20, message = "Código máximo 20 caracteres")
    @Pattern(regexp = "^[A-Z_]+$", message = "Solo letras mayúsculas y guión bajo permitidos")
    private String codigo; // UBER, RAPPI, DIDI
    
    @NotBlank(message = "Nombre requerido")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre; // UberEats, Rappi, Didi Food

    private Long sucursalId; // ⭐ AGREGAR (opcional, puede ser null para toda la empresa)

    @NotNull(message = "Porcentaje de incremento requerido")
    @DecimalMin(value = "0.00", message = "Porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "Porcentaje no puede exceder 100%")
    private BigDecimal porcentajeIncremento; // 25.00
    
    @Size(max = 7, message = "Color hex debe ser formato #RRGGBB")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color debe ser formato hexadecimal #RRGGBB")
    private String colorHex; // #FF9900
    
    @Size(max = 50, message = "Icono máximo 50 caracteres")
    private String icono; // uber-icon, fa-uber
    
    @Size(max = 500, message = "Descripción máximo 500 caracteres")
    private String descripcion;
}