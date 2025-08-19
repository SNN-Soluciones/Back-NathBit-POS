package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para anular factura
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnularFacturaRequest {
    @NotBlank(message = "El motivo es requerido")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String motivo;
}

