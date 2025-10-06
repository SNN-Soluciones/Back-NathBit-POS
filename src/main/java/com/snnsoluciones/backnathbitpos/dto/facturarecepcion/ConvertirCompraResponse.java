package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvertirCompraResponse {
    
    private Long compraId;
    private Long facturaRecepcionId;
    private String mensaje;
}