package com.snnsoluciones.backnathbitpos.dto.bitacora;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta genérica para acciones sobre la bitácora
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraActionResponse {
    private boolean exitoso;
    private String mensaje;
    private Long bitacoraId;
    private String nuevoEstado;
    private LocalDateTime proximoIntento;
}