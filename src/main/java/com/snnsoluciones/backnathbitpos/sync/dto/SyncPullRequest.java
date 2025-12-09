package com.snnsoluciones.backnathbitpos.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request para pull - ¿Qué cambios hay desde mi última sincronización?
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPullRequest {
    
    private Long terminalId;
    private Long sucursalId;
    
    /**
     * Última sincronización del POS
     * El backend devuelve solo registros con updated_at > lastSync
     */
    private LocalDateTime lastSync;
    
    /**
     * Opcional: especificar qué tablas sincronizar
     * Si es null o vacío, sincroniza todas
     */
    private java.util.List<String> tablas;
}