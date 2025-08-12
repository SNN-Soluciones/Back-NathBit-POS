package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear/actualizar impuesto en producto
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImpuestoCreateDto {

    @NotNull(message = "Tipo de impuesto es requerido")
    private TipoImpuesto tipoImpuesto;

    private CodigoTarifaIVA codigoTarifaIVA; // Solo si es IVA

    @NotNull(message = "Porcentaje es requerido")
    @DecimalMin(value = "0.00", message = "Porcentaje debe ser mayor o igual a 0")
    @DecimalMax(value = "100.00", message = "Porcentaje no puede ser mayor a 100")
    @Digits(integer = 3, fraction = 2, message = "Porcentaje formato inválido")
    private BigDecimal porcentaje;
}