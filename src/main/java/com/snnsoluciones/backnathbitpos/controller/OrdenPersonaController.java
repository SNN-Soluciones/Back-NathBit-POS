package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.orden.*;
import com.snnsoluciones.backnathbitpos.service.OrdenPersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestionar personas en órdenes
 * Permite dividir la cuenta entre varias personas
 */
@RestController
@RequestMapping("/api/ordenes/{ordenId}/personas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Personas en Órdenes", description = "Gestión de personas/comensales para dividir cuentas")
public class OrdenPersonaController {

    private final OrdenPersonaService ordenPersonaService;

    // ==================== CREAR PERSONA ====================

    @Operation(
        summary = "Crear persona en orden",
        description = "Agrega una nueva persona a la orden para asignarle items"
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<OrdenPersonaDTO>> crearPersona(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            @Valid @RequestBody CrearPersonaRequest request) {
        
        log.info("📝 POST /api/ordenes/{}/personas - Nombre: {}", ordenId, request.nombre());
        
        try {
            OrdenPersonaDTO persona = ordenPersonaService.crearPersona(ordenId, request);
            return ResponseEntity.ok(ApiResponse.success("Persona creada exitosamente", persona));
        } catch (Exception e) {
            log.error("❌ Error creando persona: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== LISTAR PERSONAS ====================

    @Operation(
        summary = "Listar personas de orden",
        description = "Obtiene todas las personas activas de una orden"
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<List<OrdenPersonaDTO>>> listarPersonas(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId) {
        
        log.info("🔍 GET /api/ordenes/{}/personas", ordenId);
        
        try {
            List<OrdenPersonaDTO> personas = ordenPersonaService.listarPersonas(ordenId);
            return ResponseEntity.ok(ApiResponse.success(
                "Personas encontradas: " + personas.size(), personas));
        } catch (Exception e) {
            log.error("❌ Error listando personas: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== AGREGAR ITEMS CON PERSONA ====================

    @Operation(
        summary = "Agregar items con persona asignada",
        description = """
            Agrega items a la orden asignándolos automáticamente a una persona.
            Si la persona no existe, se crea automáticamente.
            
            Ejemplo de uso:
            - Mesero toma orden de "Andrés": 3 imperiales, 1 nachos
            - Sistema busca/crea persona "Andrés" y le asigna esos items
            """
    )
    @PostMapping("/agregar-items")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<OrdenResponse>> agregarItemsConPersona(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            @Valid @RequestBody AgregarItemsConPersonaRequest request) {
        
        log.info("📝 POST /api/ordenes/{}/personas/agregar-items - Persona: {}, Items: {}", 
            ordenId, request.nombrePersona(), request.items().size());
        
        try {
            OrdenResponse orden = ordenPersonaService.agregarItemsConPersona(ordenId, request);
            return ResponseEntity.ok(ApiResponse.success(
                "Items agregados para " + request.nombrePersona(), orden));
        } catch (Exception e) {
            log.error("❌ Error agregando items con persona: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== ASIGNAR ITEM A PERSONA ====================

    @Operation(
        summary = "Asignar item existente a persona",
        description = "Asigna un item que ya existe en la orden a una persona específica"
    )
    @PutMapping("/items/{itemId}/asignar")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> asignarItemAPersona(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            @Parameter(description = "ID del item") @PathVariable Long itemId,
            @Valid @RequestBody AsignarItemAPersonaRequest request) {
        
        log.info("🔗 PUT /api/ordenes/{}/personas/items/{}/asignar - Persona: {}", 
            ordenId, itemId, request.personaId());
        
        try {
            ordenPersonaService.asignarItemAPersona(ordenId, itemId, request);
            return ResponseEntity.ok(ApiResponse.success("Item asignado exitosamente", null));
        } catch (Exception e) {
            log.error("❌ Error asignando item: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== DESASIGNAR ITEM DE PERSONA ====================

    @Operation(
        summary = "Desasignar item de persona",
        description = "Remueve la asignación de un item, convirtiéndolo en item compartido"
    )
    @PutMapping("/items/{itemId}/desasignar")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> desasignarItemDePersona(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            @Parameter(description = "ID del item") @PathVariable Long itemId) {
        
        log.info("🔓 PUT /api/ordenes/{}/personas/items/{}/desasignar", ordenId, itemId);
        
        try {
            ordenPersonaService.desasignarItemDePersona(ordenId, itemId);
            return ResponseEntity.ok(ApiResponse.success("Item desasignado (ahora es compartido)", null));
        } catch (Exception e) {
            log.error("❌ Error desasignando item: {}", e.getMessage());
            throw e;
        }
    }
}