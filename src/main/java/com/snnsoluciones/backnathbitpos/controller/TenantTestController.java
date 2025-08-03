package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.service.TenantTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de prueba para verificar el funcionamiento del multi-tenant.
 * NOTA: Este controlador está disponible solo en perfil de desarrollo.
 */
@RestController
@RequestMapping("/test/tenant")
@RequiredArgsConstructor
@Slf4j
@Profile("dev") // Solo disponible en desarrollo
@Tag(name = "Tenant Test", description = "Endpoints de prueba para multi-tenant (solo desarrollo)")
public class TenantTestController {

    private final TenantTestService tenantTestService;

    @GetMapping("/current")
    @Operation(summary = "Obtener tenant actual", description = "Muestra el tenant actual del contexto")
    public ResponseEntity<Map<String, String>> getCurrentTenant() {
        Map<String, String> response = new HashMap<>();
        response.put("currentTenant", TenantContext.getCurrentTenant());
        response.put("hasTenant", String.valueOf(TenantContext.hasTenant()));
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-access")
    @Operation(summary = "Probar acceso multi-tenant", description = "Ejecuta pruebas de acceso a diferentes tenants")
    public ResponseEntity<Map<String, String>> testMultiTenantAccess() {
        try {
            tenantTestService.testMultiTenantAccess();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Prueba completada. Revisar logs para detalles.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en prueba multi-tenant", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Información del sistema multi-tenant", description = "Muestra información sobre la configuración multi-tenant")
    public ResponseEntity<Map<String, Object>> getMultiTenantInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("multiTenantEnabled", true);
        info.put("strategy", "SCHEMA");
        info.put("currentTenant", TenantContext.getCurrentTenant());
        info.put("defaultSchema", "public");
        
        return ResponseEntity.ok(info);
    }
}