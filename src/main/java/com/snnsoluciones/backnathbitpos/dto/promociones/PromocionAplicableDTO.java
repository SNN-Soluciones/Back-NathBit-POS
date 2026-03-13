package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una promoción que califica para la orden actual,
 * con el detalle de qué ítems se ven afectados y cuánto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionAplicableDTO {

    private Long promocionId;
    private String nombre;
    private TipoPromocion tipo;
    private List<ItemDescuentoDTO> itemsAfectados;
    private BigDecimal totalDescuento;

    /**
     * Solo para ALL_YOU_CAN_EAT / BARRA_LIBRE.
     * Indica los ítems iniciales que se crearán al aplicar la promo.
     */
    private List<ItemRondaDTO> itemsInicialesAYCE;
}