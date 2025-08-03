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
public class AsignarSucursalesRequest {
    
    @NotEmpty(message = "Debe especificar al menos una sucursal")
    private List<UUID> sucursalIds;
    
    private boolean reemplazar = false; // Si true, reemplaza las sucursales actuales. Si false, las agrega.
}