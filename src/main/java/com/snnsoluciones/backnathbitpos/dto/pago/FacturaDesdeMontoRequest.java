package com.snnsoluciones.backnathbitpos.dto.pago;

import com.snnsoluciones.backnathbitpos.dto.factura.MedioPagoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaDesdeMontoRequest {

    @NotNull(message = "El monto total es requerido")
    @DecimalMin(value = "1.0", message = "El monto debe ser mayor a 0")
    private BigDecimal montoTotal;

    @NotEmpty(message = "Debe incluir al menos un medio de pago")
    @Valid
    private List<MedioPagoRequest> pagos;

    private String observaciones;

    @NotNull(message = "El cajero es requerido")
    private Long cajeroId;

    // sesionCajaId NO va aquí, se calcula en backend
}