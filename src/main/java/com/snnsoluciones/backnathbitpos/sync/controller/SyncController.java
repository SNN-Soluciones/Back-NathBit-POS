package com.snnsoluciones.backnathbitpos.sync.controller;

import com.snnsoluciones.backnathbitpos.sync.dto.SyncPullRequest;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPullResponse;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushRequest;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushResponse;
import com.snnsoluciones.backnathbitpos.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Sincronización POS ↔ Backend")
public class SyncController {

    private final SyncService syncService;

    /**
     * PULL - Descarga cambios desde el servidor
     * Solo registros modificados después de lastSync
     */
    @GetMapping("/pull")
    @Operation(summary = "Pull - Descarga cambios", 
               description = "Trae registros modificados desde lastSync")
    public ResponseEntity<SyncPullResponse> pull(
            @RequestParam Long terminalId,
            @RequestParam Long sucursalId,
            @RequestParam(required = false) LocalDateTime lastSync) {
        
        log.info("📥 GET /api/sync/pull - Terminal: {}, Sucursal: {}, LastSync: {}", 
            terminalId, sucursalId, lastSync);
        
        SyncPullRequest request = SyncPullRequest.builder()
            .terminalId(terminalId)
            .sucursalId(sucursalId)
            .lastSync(lastSync)
            .build();
        
        SyncPullResponse response = syncService.pull(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUSH - Sube cambios del POS al servidor
     * Clientes nuevos, facturas, sesiones de caja
     */
    @PostMapping("/push")
    @Operation(summary = "Push - Sube cambios", 
               description = "Envía datos creados/modificados en el POS")
    public ResponseEntity<SyncPushResponse> push(@RequestBody SyncPushRequest request) {
        
        log.info("📤 POST /api/sync/push - Terminal: {}, Sucursal: {}", 
            request.getTerminalId(), request.getSucursalId());
        
        SyncPushResponse response = syncService.push(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * FULL - Descarga TODO (primera sincronización)
     * Ignora lastSync, trae todos los datos
     */
    @GetMapping("/full")
    @Operation(summary = "Full Sync - Descarga todo", 
               description = "Primera sincronización, descarga todos los datos")
    public ResponseEntity<SyncPullResponse> full(
            @RequestParam Long terminalId,
            @RequestParam Long sucursalId) {
        
        log.info("📦 GET /api/sync/full - Terminal: {}, Sucursal: {}", 
            terminalId, sucursalId);
        
        SyncPullResponse response = syncService.full(terminalId, sucursalId);
        
        return ResponseEntity.ok(response);
    }
}