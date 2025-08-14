package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaRequest;
import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.ConfigHaciendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config-hacienda")
@RequiredArgsConstructor
@Tag(name = "Configuración Hacienda", description = "Configuración de facturación electrónica")
public class ConfigHaciendaController {

    private final ConfigHaciendaService configHaciendaService;
    private final EmpresaService empresaService;

    @Operation(summary = "Obtener configuración por empresa")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ConfigHaciendaResponse>> obtenerPorEmpresa(
        @PathVariable Long empresaId) {

        EmpresaConfigHacienda config = configHaciendaService.buscarPorEmpresa(empresaId)
            .orElse(null);

        if (config == null) {
            return ResponseEntity.ok(ApiResponse.ok("No hay configuración de Hacienda", null));
        }

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(config)));
    }

    @Operation(summary = "Crear o actualizar configuración")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ConfigHaciendaResponse>> crearOActualizar(
        @Valid @RequestBody ConfigHaciendaRequest request) {

        Empresa empresa = empresaService.buscarPorId(request.getEmpresaId());
        if (empresa == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("La empresa no existe"));
        }

        // Verificar si la empresa requiere Hacienda
        if (!empresa.getRequiereHacienda()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Esta empresa no requiere configuración de Hacienda"));
        }

        // Crear o actualizar usando el servicio
        EmpresaConfigHacienda config = configHaciendaService.crearOActualizar(request);

        return new ResponseEntity<>(
            ApiResponse.ok("Configuración guardada exitosamente", convertirAResponse(config)),
            HttpStatus.CREATED
        );
    }

    @Operation(summary = "Eliminar configuración")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        configHaciendaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Configuración eliminada", null));
    }

    @Operation(summary = "Verificar configuración completa")
    @GetMapping("/verificar/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> verificarConfiguracion(
        @PathVariable Long empresaId) {

        boolean completa = configHaciendaService.esConfiguracionCompleta(empresaId);

        return ResponseEntity.ok(ApiResponse.ok(
            completa ? "Configuración completa" : "Configuración incompleta",
            completa
        ));
    }

    // Helper para convertir
    private ConfigHaciendaResponse convertirAResponse(EmpresaConfigHacienda config) {
        boolean configuracionCompleta = config.getUsuarioHacienda() != null &&
            config.getClaveHacienda() != null;

        return ConfigHaciendaResponse.builder()
            .id(config.getId())
            .ambiente(config.getAmbiente())
            .tipoAutenticacion(config.getTipoAutenticacion())
            .usuarioHacienda(config.getUsuarioHacienda())
            .tieneClaveConfigurada(config.getClaveHacienda() != null)
            .tieneCertificadoConfigurado(config.getCertificadoEncriptado() != null)
            .fechaEmisionCertificado(config.getFechaEmisionCertificado())
            .fechaVencimientoCertificado(config.getFechaVencimientoCertificado())
            .empresaId(config.getEmpresa().getId())
            .empresaNombre(config.getEmpresa().getNombreComercial())
            .empresaIdentificacion(config.getEmpresa().getIdentificacion())
            .configuracionCompleta(configuracionCompleta)
            .mensajeEstado(configuracionCompleta ?
                "Configuración lista para " + config.getAmbiente() :
                "Faltan datos de configuración")
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }
}