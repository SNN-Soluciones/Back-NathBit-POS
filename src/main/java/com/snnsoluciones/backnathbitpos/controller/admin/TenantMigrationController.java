package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.tenant.TenantMigrationService;
import com.snnsoluciones.backnathbitpos.service.tenant.TenantMigrationService.MigrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
public class TenantMigrationController {

    private final TenantMigrationService migrationService;

    @PostMapping("/empresa/{empresaId}")
    public ResponseEntity<ApiResponse<MigrationResult>> migrarEmpresa(@PathVariable Long empresaId) {
        log.info("POST /api/admin/migration/empresa/{}", empresaId);
        
        MigrationResult result = migrationService.migrarEmpresa(empresaId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Empresa migrada exitosamente", result));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.getMensaje()));
        }
    }

    @PostMapping("/migrar/{empresaId}/asignar-pins")
    public ResponseEntity<ApiResponse<String>> asignarPins(@PathVariable Long empresaId) {
        try {
            String resultado = migrationService.asignarPinsATenant(empresaId);
            return ResponseEntity.ok(ApiResponse.success(resultado, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}