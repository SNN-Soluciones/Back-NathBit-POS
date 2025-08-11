package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear relación empresa-cabys
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCABySCreateDto {
    @NotNull(message = "El ID del código CAByS es requerido")
    private Long codigoCabysId;
    
    @Size(max = 200, message = "La descripción personalizada no puede exceder 200 caracteres")
    private String descripcionPersonalizada;
}

