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

    @Min(1)
    Integer numeroPersonas,

    @DecimalMin("0")
    @DecimalMax("100")
    BigDecimal porcentajeServicio,

    String observaciones,
    String ordenNumero,

    @NotEmpty
    List<ItemRequest> items
) {
    public record ItemRequest(
        @NotNull Long productoId,
        @NotNull BigDecimal cantidad,
        String notas,

        BigDecimal precioUnitarioOverride,

        List<OpcionRequest> opciones
    ) {}

    public record OpcionRequest(
        @NotNull Long slotId,
        @NotNull Long productoOpcionId,
        @NotNull BigDecimal cantidad
    ) {}
}
