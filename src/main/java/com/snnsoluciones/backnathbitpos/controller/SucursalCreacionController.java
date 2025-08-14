package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sucursal.CrearSucursalCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.sucursal.CrearSucursalCompletaResponse;
import com.snnsoluciones.backnathbitpos.service.SucursalCreacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@Tag(name = "Sucursales - Creación", description = "Endpoint unificado para crear sucursales con terminales")
public class SucursalCreacionController {

    private final SucursalCreacionService sucursalCreacionService;

    @Operation(summary = "Crear sucursal completa con terminales",
        description = "Crea una sucursal con todas sus terminales en una sola operación transaccional")
    @PostMapping("/crear-completo")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CrearSucursalCompletaResponse>> crearSucursalCompleta(
        @Valid @RequestBody CrearSucursalCompletaRequest request) {

        log.info("=== INICIANDO CREACIÓN DE SUCURSAL COMPLETA ===");
        log.info("Sucursal: {} para empresa ID: {}", request.getNombre(), request.getEmpresaId());

        try {
            CrearSucursalCompletaResponse response = sucursalCreacionService.crearSucursalCompleta(request);

            log.info("=== SUCURSAL CREADA EXITOSAMENTE ===");
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sucursal creada exitosamente con sus terminales", response));

        } catch (RuntimeException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al crear la sucursal"));
        }
    }
}