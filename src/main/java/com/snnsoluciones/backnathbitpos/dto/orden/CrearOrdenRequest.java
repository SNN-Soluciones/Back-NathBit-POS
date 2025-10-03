package com.snnsoluciones.backnathbitpos.dto.orden;

import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ========== REQUESTS ==========

public record CrearOrdenRequest(
    Long mesaId,

    Long sucursalId,
    
    Long clienteId,
    
    String nombreCliente,
    
    @Min(value = 1, message = "Número de personas debe ser al menos 1")
    Integer numeroPersonas,
    
    @DecimalMin(value = "0", message = "Porcentaje de servicio no puede ser negativo")
    @DecimalMax(value = "100", message = "Porcentaje de servicio no puede ser mayor a 100")
    BigDecimal porcentajeServicio,
    
    String observaciones,

    @NotEmpty List<ItemRequest> items  // AGREGAR ESTO
) {
    public record ItemRequest(
        @NotNull Long productoId,
        @NotNull BigDecimal cantidad,
        String notas
    ) {}
}
