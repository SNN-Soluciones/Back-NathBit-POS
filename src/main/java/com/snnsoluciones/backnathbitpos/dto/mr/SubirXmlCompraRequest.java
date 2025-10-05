package com.snnsoluciones.backnathbitpos.dto.mr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubirXmlCompraRequest(
    @NotBlank(message = "El contenido XML es requerido")
    String xmlContent,
    
    @NotNull(message = "El ID de empresa es requerido")
    Long empresaId,
    
    @NotNull(message = "El ID de sucursal es requerido")
    Long sucursalId
) {}