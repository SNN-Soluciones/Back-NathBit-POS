package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.plataforma.*;
import com.snnsoluciones.backnathbitpos.service.PlataformaDigitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plataformas-digitales")
@RequiredArgsConstructor
@Tag(name = "Plataformas Digitales", description = "Configuración de plataformas como UberEats, Rappi, etc.")
public class PlataformaDigitalController {

    private final PlataformaDigitalService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Crear nueva plataforma digital")
    public ResponseEntity<ApiResponse<PlataformaDigitalDTO>> crear(
        @RequestParam Long empresaId,
        @Valid @RequestBody CrearPlataformaRequest request) {

        PlataformaDigitalDTO plataforma = service.crear(empresaId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Plataforma creada exitosamente", plataforma));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ROOT', 'CAJERO', 'MESERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Listar plataformas activas para POS")
    public ResponseEntity<ApiResponse<List<PlataformaDigitalDTO>>> listarActivas(
        @RequestParam Long empresaId,
        @RequestParam Long sucursalId) {

        List<PlataformaDigitalDTO> plataformas = service.listarActivas(empresaId, sucursalId);
        return ResponseEntity.ok(ApiResponse.success("Plataformas activas", plataformas));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Listar todas las plataformas (admin)")
    public ResponseEntity<ApiResponse<List<PlataformaDigitalDTO>>> listarTodas(
        @RequestParam Long empresaId) {

        List<PlataformaDigitalDTO> plataformas = service.listarTodas(empresaId);
        return ResponseEntity.ok(ApiResponse.success("Todas las plataformas", plataformas));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Actualizar plataforma")
    public ResponseEntity<ApiResponse<PlataformaDigitalDTO>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarPlataformaRequest request) {

        PlataformaDigitalDTO plataforma = service.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.success("Plataforma actualizada", plataforma));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Activar/Desactivar plataforma")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(
        @PathVariable Long id,
        @RequestParam Boolean activo) {

        service.cambiarEstado(id, activo);
        return ResponseEntity.ok(ApiResponse.success(
            activo ? "Plataforma activada" : "Plataforma desactivada", 
            null
        ));
    }
}