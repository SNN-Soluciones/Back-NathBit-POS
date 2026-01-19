package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear/actualizar impuestos de un producto.
 *
 * IMPORTANTE: Las exoneraciones NO se definen aquí, vienen del CLIENTE
 * en el momento de facturar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de un impuesto para producto")
public class CrearImpuestoDto {

    @NotNull(message = "El tipo de impuesto es obligatorio")
    @Schema(description = "Tipo de impuesto según Hacienda CR",
        example = "IVA",
        allowableValues = {"IVA", "CONSUMO", "ESPECIFICO_BEBIDAS", "ESPECIFICO_COMBUSTIBLES", "UNICO_COMBUSTIBLES", "OTROS"},
        required = true)
    private TipoImpuesto tipo;

    @NotNull(message = "La tarifa es obligatoria")
    @DecimalMin(value = "0.0", message = "La tarifa debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "La tarifa no puede exceder 100%")
    @Schema(description = "Tarifa del impuesto en porcentaje",
        example = "13.0",
        required = true)
    private BigDecimal tarifa;

    @Schema(description = "Código de tarifa según Hacienda CR (01-08)",
        example = "08",
        nullable = true)
    private String codigoTarifa;

    @Schema(description = "Monto fijo del impuesto (solo para impuestos específicos)",
        example = "112.00",
        nullable = true)
    private BigDecimal montoImpuesto;
}