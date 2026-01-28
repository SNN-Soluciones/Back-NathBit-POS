// dto/mesas/SillaLayoutDTO.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SillaLayoutDTO {
    private Long sillaId;
    private Integer numero;
    private Double x; // Posición relativa a la barra
    private Double y;
    private Double rotation; // Para rotar sillas individuales
}