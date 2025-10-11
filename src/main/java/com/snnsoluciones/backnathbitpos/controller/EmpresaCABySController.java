package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cabys.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.EmpresaCABySService;
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

@RestController
@RequestMapping("/api/empresa-cabys")
@RequiredArgsConstructor
@Tag(name = "Empresa CAByS", description = "Gestión de códigos CAByS por empresa")
@Slf4j
public class EmpresaCABySController {

    private final EmpresaCABySService service;

    @Operation(summary = "Buscar en catálogo CAByS")
    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CABySDto>>> buscarCatalogo(
            @RequestParam(required = false) String impuesto,
            @RequestParam(required = false) String busqueda) {
        
        List<CABySDto> resultados = service.buscarEnCatalogo(impuesto, busqueda);
        return ResponseEntity.ok(ApiResponse.ok("Encontrados: " + resultados.size(), resultados));
    }

    @Operation(summary = "Asignar CAByS a empresa")
    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaCABySDto>> asignar(
            @Valid @RequestBody AsignarCABySRequest request) {
        
        try {
            EmpresaCABySDto asignado = service.asignar(request.getEmpresaId(), request.getCodigoCabysId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("CAByS asignado exitosamente", asignado));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Listar CAByS de empresa")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<EmpresaCABySDto>>> listarPorEmpresa(
            @PathVariable Long empresaId) {
        
        List<EmpresaCABySDto> lista = service.listarPorEmpresa(empresaId);
        return ResponseEntity.ok(ApiResponse.ok("CAByS asignados: " + lista.size(), lista));
    }

    @Operation(summary = "Quitar CAByS de empresa")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> quitar(@PathVariable Long id) {
        
        try {
            service.quitar(id);
            return ResponseEntity.ok(ApiResponse.ok("CAByS removido"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Listar CAByS de sucursal")
    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<EmpresaCABySDto>>> listarPorSucursal(
        @PathVariable Long sucursalId) {

        List<EmpresaCABySDto> lista = service.listarPorSucursal(sucursalId);
        return ResponseEntity.ok(ApiResponse.ok("CAByS asignados: " + lista.size(), lista));
    }
}