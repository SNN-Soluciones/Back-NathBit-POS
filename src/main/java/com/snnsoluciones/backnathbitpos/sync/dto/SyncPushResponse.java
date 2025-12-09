package com.snnsoluciones.backnathbitpos.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response del push - Confirmación y mapeo de IDs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushResponse {
    
    private Boolean success;
    private LocalDateTime syncTimestamp;
    private String mensaje;
    
    // Mapeo de UUIDs → Server IDs
    private List<IdMapping> clientesMapeados;
    private List<IdMapping> facturasMapeadas;
    private List<IdMapping> sesionesMapeadas;
    
    // Errores (si los hay)
    private List<SyncError> errores;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdMapping {
        private String uuid;
        private Long serverId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncError {
        private String uuid;
        private String tabla;
        private String error;
    }
}