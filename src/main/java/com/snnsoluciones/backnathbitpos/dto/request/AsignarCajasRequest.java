package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarCajasRequest {
    
    @NotEmpty(message = "Debe especificar al menos una caja")
    private List<UUID> cajaIds;
    
    private boolean reemplazar = false; // Si true, reemplaza las cajas actuales. Si false, las agrega.
}