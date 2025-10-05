package com.snnsoluciones.backnathbitpos.dto.mr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record AceptarFacturaRequest(
    @NotBlank(message = "La clave de Hacienda es requerida")
    String claveHacienda,
    
    @NotBlank(message = "El contenido XML es requerido")
    String xmlContent,
    
    @NotNull(message = "El ID de empresa es requerido")
    Long empresaId,
    
    @NotNull(message = "El ID de sucursal es requerido")
    Long sucursalId,
    
    Long proveedorId,  // Si existe
    
    CrearProveedorDto nuevoProveedor,  // Si no existe
    
    List<MappingProductoDto> productosMapping,
    
    boolean procesarInventario,
    
    String observaciones
) {}