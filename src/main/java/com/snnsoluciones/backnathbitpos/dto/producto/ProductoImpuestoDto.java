package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para respuesta de impuestos de producto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Impuesto de un producto")
public class ProductoImpuestoDto {

    @Schema(description = "ID del impuesto", example = "1")
    private Long id;

    @Schema(description = "Tipo de impuesto", example = "IVA")
    private TipoImpuesto tipoImpuesto;  // ← Nota: tipoImpuesto, no tipo

    @Schema(description = "Código de tarifa IVA", example = "TARIFA_GENERAL_13")
    private CodigoTarifaIVA codigoTarifaIVA;  // ← Nota: codigoTarifaIVA

    @Schema(description = "Porcentaje del impuesto", example = "13.0")
    private BigDecimal porcentaje;  // ← Nota: porcentaje, no tarifa

    @Schema(description = "Si el impuesto está activo", example = "true")
    private Boolean activo;
}