package com.snnsoluciones.backnathbitpos.dto.factura;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para detalle de factura con soporte para múltiples descuentos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFacturaRequest {

    @NotNull(message = "El producto es requerido")
    private Long productoId;

    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 13, fraction = 3, message = "Formato de cantidad inválido")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 13, fraction = 5, message = "Formato de precio inválido")
    private BigDecimal precioUnitario;

    /**
     * Lista de descuentos aplicados a esta línea
     * Máximo 5 descuentos según Hacienda
     */
    @Valid
    @Size(max = 5, message = "Máximo 5 descuentos permitidos por línea")
    @Builder.Default
    private List<DescuentoRequest> descuentos = new ArrayList<>();

    /**
     * Para override de descripción si es necesario
     */
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcionPersonalizada;

    /**
     * Código de tarifa IVA específico para esta línea
     * Si no se envía, se usa el default del producto
     */
    @Pattern(regexp = "^(01|02|03|04|05|06|07|08|09|10|11)?$",
        message = "Código de tarifa IVA no válido")
    private String codigoTarifaIVA;

    /**
     * Calcula el subtotal con descuentos aplicados
     */
    public BigDecimal calcularSubtotalConDescuentos() {
        BigDecimal montoTotal = cantidad.multiply(precioUnitario);
        BigDecimal montoConDescuentos = montoTotal;

        // Aplicar descuentos en cascada
        for (DescuentoRequest desc : descuentos) {
            if (desc.getMontoDescuento() != null) {
                montoConDescuentos = montoConDescuentos.subtract(desc.getMontoDescuento());
            } else if (desc.getPorcentaje() != null) {
                BigDecimal montoDesc = montoConDescuentos
                    .multiply(desc.getPorcentaje())
                    .divide(new BigDecimal("100"), 5, BigDecimal.ROUND_HALF_UP);
                montoConDescuentos = montoConDescuentos.subtract(montoDesc);
            }
        }

        return montoConDescuentos;
    }
}