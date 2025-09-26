package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.ProductoMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/migration/productos")
@RequiredArgsConstructor
@Tag(name = "Migración Productos", description = "Endpoints para migración de productos desde Excel")
@Slf4j
public class ProductoMigrationController {

    private final ProductoMigrationService migrationService;

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROOT')")
    @Operation(
        summary = "Importar productos desde Excel",
        description = "Lee el archivo Excel y crea los productos con categorías e impuestos"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> importarProductosDesdeExcel(
            @RequestParam("archivo") 
            @Parameter(description = "Archivo Excel (.xlsx) con los productos", required = true) 
            MultipartFile archivo,
            
            @RequestParam("empresaId") 
            @Parameter(description = "ID de la empresa", required = true, example = "5") 
            Long empresaId,
            
            @RequestParam("sucursalId") 
            @Parameter(description = "ID de la sucursal", required = true, example = "8") 
            Long sucursalId,
            
            @RequestParam("empresaCabysId") 
            @Parameter(description = "ID del EmpresaCAByS para asignar a los productos", required = true, example = "77") 
            Long empresaCabysId) {
        
        log.info("=== RECIBIENDO ARCHIVO PARA MIGRACIÓN DE PRODUCTOS ===");
        log.info("Archivo: {}, Tamaño: {} bytes", archivo.getOriginalFilename(), archivo.getSize());
        log.info("Empresa ID: {}, Sucursal ID: {}, EmpresaCAByS ID: {}", 
            empresaId, sucursalId, empresaCabysId);
        
        try {
            Map<String, Object> resultado = migrationService.migrarProductosDesdeExcel(
                archivo, empresaId, sucursalId, empresaCabysId
            );
            
            String mensaje = String.format(
                "Migración completada: %d productos creados, %d duplicados, %d errores",
                resultado.get("productosCreados"),
                resultado.get("productosDuplicados"), 
                resultado.get("errores")
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message(mensaje)
                    .data(resultado)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error en migración de productos", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error en migración: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/verificar-duplicados/{empresaId}/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
    @Operation(
        summary = "Verificar productos existentes",
        description = "Lista códigos de productos que ya existen para evitar duplicados"
    )
    public ResponseEntity<ApiResponse<String>> verificarProductosExistentes(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId) {
        
        // Este endpoint puede ser útil para verificar antes de migrar
        return ResponseEntity.ok(
            ApiResponse.<String>builder()
                .success(true)
                .message("Para ver productos existentes")
                .data("Usar GET /api/productos/empresa/" + empresaId + "/" + sucursalId)
                .build()
        );
    }
    
    @PostMapping("/preview")
    @PreAuthorize("hasRole('ROOT')")
    @Operation(
        summary = "Preview de migración",
        description = "Muestra qué se importaría sin crear nada en la BD"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewMigracion(
            @RequestParam("archivo") MultipartFile archivo) {
        
        log.info("=== PREVIEW DE MIGRACIÓN DE PRODUCTOS ===");
        
        try {
            // Aquí podrías implementar una vista previa sin guardar
            // Por ahora solo retornamos info básica
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Preview no implementado aún")
                    .data(Map.of(
                        "archivo", archivo.getOriginalFilename(),
                        "tamaño", archivo.getSize(),
                        "tipo", archivo.getContentType()
                    ))
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }
}