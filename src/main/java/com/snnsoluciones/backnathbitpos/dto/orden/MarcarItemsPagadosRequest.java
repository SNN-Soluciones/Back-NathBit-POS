// MarcarItemsPagadosRequest.java
package com.snnsoluciones.backnathbitpos.dto.orden;

import lombok.Data;
import java.util.List;

@Data
public class MarcarItemsPagadosRequest {
    private List<Long> itemIds;
    private Long facturaInternaId;
}