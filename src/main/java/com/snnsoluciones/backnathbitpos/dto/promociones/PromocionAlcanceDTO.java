package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionAlcanceDTO {
    private Long id;
    private String nombre;
    private RolPromocionAlcance rol;
}