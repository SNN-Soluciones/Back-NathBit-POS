// MarcarItemsPagadosResponse.java
package com.snnsoluciones.backnathbitpos.dto.orden;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarcarItemsPagadosResponse {
    private Boolean ordenCerrada;
    private Integer itemsPendientes;
    private Boolean mesaLiberada;
}