package com.snnsoluciones.backnathbitpos.dto.factura;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para notificación de error
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionErrorDto {
    private String tipoError; // SISTEMA, HACIENDA_RECHAZO
    private Long facturaId;
    private String clave;
    private String mensaje;
    private String detalleError;
    private LocalDateTime fechaError;
    
    // Info contacto
    private String emailDestinatario;
    private String nombreEmpresa;
    private String telefonoContacto;
}