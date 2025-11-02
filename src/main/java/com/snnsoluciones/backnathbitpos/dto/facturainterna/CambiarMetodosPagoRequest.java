package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para cambiar métodos de pago de una factura interna
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarMetodosPagoRequest {
    
    @NotEmpty(message = "Debe proporcionar al menos un medio de pago")
    @Valid
    private List<MedioPagoInternoRequest> mediosPago;
    
    @NotNull(message = "El motivo del cambio es requerido")
    private String motivo;  // Para auditoría/logs
}