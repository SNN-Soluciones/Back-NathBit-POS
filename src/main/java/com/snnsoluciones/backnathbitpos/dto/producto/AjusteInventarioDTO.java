package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AjusteInventarioDTO {
    private Long productoId;
    private Long sucursalId;
    private BigDecimal cantidad;
    private String tipoAjuste; // "ABSOLUTO" o "RELATIVO"
    private String motivo;
}