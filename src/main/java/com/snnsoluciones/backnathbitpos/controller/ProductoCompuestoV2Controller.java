package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Dto;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Request;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoV2Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v2/productos/{productoId}/compuesto")
@RequiredArgsConstructor
@Tag(name = "Productos Compuestos V2")
public class ProductoCompuestoV2Controller {

    private final ProductoCompuestoV2Service compuestoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductoCompuestoV2Dto>> crear(
        @RequestParam Long empresaId,
        @PathVariable Long productoId,
        @Valid @RequestBody ProductoCompuestoV2Request request) {

        log.info("POST compuesto V2 - producto: {}", productoId);
        try {
            ProductoCompuestoV2Dto resultado = compuestoService.crear(empresaId, productoId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Compuesto creado", resultado));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando compuesto V2", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<ProductoCompuestoV2Dto>> obtener(
        @RequestParam Long empresaId,
        @PathVariable Long productoId) {

        log.info("GET compuesto V2 - producto: {}", productoId);
        try {
            ProductoCompuestoV2Dto resultado = compuestoService.obtener(empresaId, productoId);
            return ResponseEntity.ok(ApiResponse.ok("Compuesto obtenido", resultado));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.<ProductoCompuestoV2Dto>builder()
                .success(true)
                .message("Sin configuración")
                .data(null)
                .build());
        } catch (Exception e) {
            log.error("Error obteniendo compuesto V2", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductoCompuestoV2Dto>> actualizar(
        @RequestParam Long empresaId,
        @PathVariable Long productoId,
        @Valid @RequestBody ProductoCompuestoV2Request request) {

        log.info("PUT compuesto V2 - producto: {}", productoId);
        try {
            ProductoCompuestoV2Dto resultado = compuestoService.actualizar(empresaId, productoId, request);
            return ResponseEntity.ok(ApiResponse.ok("Compuesto actualizado", resultado));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando compuesto V2", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @   DeleteMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(
        @RequestParam Long empresaId,
        @PathVariable Long productoId) {

        log.info("DELETE compuesto V2 - producto: {}", productoId);
        try {
            compuestoService.eliminar(empresaId, productoId);
            return ResponseEntity.ok(ApiResponse.ok("Compuesto eliminado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}