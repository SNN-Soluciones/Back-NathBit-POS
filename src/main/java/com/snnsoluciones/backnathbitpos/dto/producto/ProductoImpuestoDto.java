package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImpuestoDto {
    private Long id;
    private TipoImpuesto tipoImpuesto;
    private CodigoTarifaIVA tarifaIva;
    private BigDecimal porcentaje;
    private Boolean activo;
}
