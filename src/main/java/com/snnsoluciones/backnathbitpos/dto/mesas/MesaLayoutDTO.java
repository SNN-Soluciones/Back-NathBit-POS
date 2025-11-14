// MesaLayoutDTO.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaLayoutDTO {
    private Long mesaId;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Double rotation; // Para futuro
}
