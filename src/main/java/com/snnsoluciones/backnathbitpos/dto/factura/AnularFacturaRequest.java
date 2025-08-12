package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularFacturaRequest {
    
    @NotBlank(message = "El motivo de anulación es requerido")
    @Size(min = 10, max = 200, message = "El motivo debe tener entre 10 y 200 caracteres")
    private String motivo;
    
    // Para auditoría
    private String usuarioAnula;
}