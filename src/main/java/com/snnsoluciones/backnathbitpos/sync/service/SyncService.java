package com.snnsoluciones.backnathbitpos.sync.service;

import com.snnsoluciones.backnathbitpos.sync.dto.SyncPullRequest;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPullResponse;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushRequest;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushResponse;

/**
 * Servicio de sincronización POS ↔ Backend
 */
public interface SyncService {
    
    /**
     * Pull: Descarga cambios desde el servidor
     * Solo registros con updated_at > lastSync
     */
    SyncPullResponse pull(SyncPullRequest request);
    
    /**
     * Push: Sube cambios del POS al servidor
     * Clientes nuevos, facturas, sesiones de caja
     */
    SyncPushResponse push(SyncPushRequest request);
    
    /**
     * Full: Descarga TODO (primera sincronización)
     * Ignora lastSync, trae todos los datos
     */
    SyncPullResponse full(Long terminalId, Long sucursalId);
}