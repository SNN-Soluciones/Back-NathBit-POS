package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VerificarProduccionDTO {
    private Long empresaId;
    private Long productoId;
    private Long sucursalId;
    private BigDecimal cantidad;
}