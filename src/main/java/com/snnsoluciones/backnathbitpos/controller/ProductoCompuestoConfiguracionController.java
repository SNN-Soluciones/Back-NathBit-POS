package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ActualizarConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CrearConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dedicado a gestionar configuraciones condicionales
 * de productos compuestos
 */
@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Configuraciones Condicionales", description = "Gestión de configuraciones condicionales para productos compuestos")
public class ProductoCompuestoConfiguracionController {

    private final ProductoCompuestoService compuestoService;

    /**
     * Obtener todas las configuraciones de un producto compuesto
     * GET /api/productos/{productoId}/configuraciones
     */
    @GetMapping("/{productoId}/configuraciones")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Listar configuraciones",
            description = "Obtiene todas las configuraciones condicionales de un producto compuesto")
    public ResponseEntity<ApiResponse<List<ProductoCompuestoConfiguracionDTO>>> obtenerConfiguraciones(
            @PathVariable Long productoId) {

        log.info("Obteniendo configuraciones del producto: {}", productoId);

        try {
            List<ProductoCompuestoConfiguracionDTO> configuraciones =
                    compuestoService.obtenerConfiguraciones(productoId);

            return ResponseEntity.ok(
                    ApiResponse.ok(
                            "Configuraciones obtenidas: " + configuraciones.size(),
                            configuraciones
                    )
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo configuraciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener configuraciones: " + e.getMessage()));
        }
    }

    /**
     * Crear una nueva configuración condicional
     * POST /api/productos/{productoId}/configuraciones
     */
    @PostMapping("/{productoId}/configuraciones")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Crear configuración",
            description = "Crea una nueva configuración condicional para un producto compuesto")
    public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> crearConfiguracion(
            @PathVariable Long productoId,
            @Valid @RequestBody CrearConfiguracionRequest request) {

        log.info("Creando configuración '{}' para producto: {}", request.getNombre(), productoId);

        try {
            ProductoCompuestoConfiguracionDTO resultado =
                    compuestoService.crearConfiguracion(productoId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Configuración creada exitosamente", resultado));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al crear configuración: " + e.getMessage()));
        }
    }

    /**
     * Actualizar una configuración existente
     * PUT /api/productos/configuraciones/{configId}
     */
    @PutMapping("/configuraciones/{configId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar configuración",
            description = "Actualiza una configuración condicional existente")
    public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> actualizarConfiguracion(
            @PathVariable Long configId,
            @Valid @RequestBody ActualizarConfiguracionRequest request) {

        log.info("Actualizando configuración: {}", configId);

        try {
            ProductoCompuestoConfiguracionDTO resultado =
                    compuestoService.actualizarConfiguracion(configId, request);

            return ResponseEntity.ok(
                    ApiResponse.ok("Configuración actualizada exitosamente", resultado)
            );

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al actualizar configuración: " + e.getMessage()));
        }
    }

    /**
     * Eliminar una configuración
     * DELETE /api/productos/configuraciones/{configId}
     */
    @DeleteMapping("/configuraciones/{configId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
    @Operation(summary = "Eliminar configuración",
            description = "Elimina una configuración condicional (solo ROOT y SUPER_ADMIN)")
    public ResponseEntity<ApiResponse<Void>> eliminarConfiguracion(
            @PathVariable Long configId) {

        log.info("Eliminando configuración: {}", configId);

        try {
            compuestoService.eliminarConfiguracion(configId);

            return ResponseEntity.ok(
                    ApiResponse.ok("Configuración eliminada exitosamente", null)
            );

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al eliminar configuración: " + e.getMessage()));
        }
    }

    /**
     * Obtener configuración que se activa con una opción específica
     * ENDPOINT CLAVE PARA EL FRONTEND
     * GET /api/productos/{productoId}/configuraciones/por-opcion/{opcionId}
     */
    @GetMapping("/{productoId}/configuraciones/por-opcion/{opcionId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Obtener configuración por opción",
            description = "Obtiene la configuración que se activa al seleccionar una opción específica (KEY para frontend)")
    public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> obtenerConfiguracionPorOpcion(
            @PathVariable Long productoId,
            @PathVariable Long opcionId) {

        log.info("Obteniendo configuración para producto {} con opción {}", productoId, opcionId);

        try {
            ProductoCompuestoConfiguracionDTO configuracion =
                    compuestoService.obtenerConfiguracionPorOpcion(productoId, opcionId);

            return ResponseEntity.ok(
                    ApiResponse.ok("Configuración encontrada", configuracion)
            );

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo configuración por opción: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener configuración: " + e.getMessage()));
        }
    }
}