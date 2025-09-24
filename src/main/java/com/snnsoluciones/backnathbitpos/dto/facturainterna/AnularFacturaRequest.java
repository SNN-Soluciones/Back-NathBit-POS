package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularFacturaRequest {
    
    @NotBlank(message = "Debe especificar el motivo de anulación")
    private String motivo;
}