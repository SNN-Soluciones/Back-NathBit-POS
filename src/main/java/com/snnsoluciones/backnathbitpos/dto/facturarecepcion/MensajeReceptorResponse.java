package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MensajeReceptorResponse {
    
    private Boolean exitoso;
    private String mensaje;
    private EstadoFacturaRecepcion estadoFinal;
    private String respuestaHacienda;
}