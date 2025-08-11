package com.snnsoluciones.backnathbitpos.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUbicacionCreateDTO {
    
    @NotBlank(message = "La provincia es obligatoria")
    @Size(min = 1, max = 1, message = "El código de provincia debe tener 1 dígito")
    private String provinciaId;
    
    @NotBlank(message = "El cantón es obligatorio")
    @Size(min = 2, max = 2, message = "El código de cantón debe tener 2 dígitos")
    private String cantonId;
    
    @NotBlank(message = "El distrito es obligatorio")
    @Size(min = 2, max = 2, message = "El código de distrito debe tener 2 dígitos")
    private String distritoId;
    
    private Long barrioId; // Opcional
    
    @Size(min = 5, max = 250, message = "Las otras señas deben tener entre 5 y 250 caracteres")
    private String otrasSenas;
}