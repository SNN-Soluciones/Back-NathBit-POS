// src/main/java/com/snnsoluciones/backnathbitpos/controller/EmpresaCreacionController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.service.EmpresaCreacionService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas - Creación", description = "Endpoint unificado para crear empresas")
public class EmpresaCreacionController {

    private final EmpresaCreacionService empresaCreacionService;
    private final EmpresaService empresaService;
    private final ObjectMapper objectMapper;
    private final UsuarioEmpresaService usuarioEmpresaService;

    @Operation(summary = "Crear empresa completa",
        description = "Crea una empresa con todos sus datos, logo, certificado y configuración de Hacienda en una sola operación")
    @PostMapping(value = "/crear-completo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CrearEmpresaCompletaResponse>> crearEmpresaCompleta(
        @RequestPart("datos") String datosJson,
        @RequestPart(value = "logo", required = false) MultipartFile logo,
        @RequestPart(value = "certificado", required = false) MultipartFile certificado,
        Authentication auth) {

        try {
            // 1. Parsear el JSON de los datos
            CrearEmpresaCompletaRequest request = objectMapper.readValue(
                datosJson,
                CrearEmpresaCompletaRequest.class
            );

            log.info("Creando empresa completa: {}", request.getNombreComercial());

            // 2. Validaciones de archivos
            if (logo != null && !logo.isEmpty()) {
                // Validar tipo de archivo para logo
                String contentType = logo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El logo debe ser una imagen"));
                }

                // Validar tamaño (máx 5MB)
                if (logo.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El logo no puede superar 5MB"));
                }
            }

            if (certificado != null && !certificado.isEmpty()) {
                // Validar extensión del certificado
                String filename = certificado.getOriginalFilename();
                if (filename == null || !filename.toLowerCase().endsWith(".p12")) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El certificado debe ser un archivo .p12"));
                }

                // Validar tamaño (máx 1MB)
                if (certificado.getSize() > 1024 * 1024) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El certificado no puede superar 1MB"));
                }
            }

            // 3. Validar que si requiere Hacienda, debe venir el certificado
            if (request.getRequiereHacienda() && (certificado == null || certificado.isEmpty())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El certificado es requerido para facturación electrónica"));
            }

            // 4. Llamar al servicio
            CrearEmpresaCompletaResponse response = empresaCreacionService.crearEmpresaCompleta(
                request,
                logo,
                certificado
            );

            log.info("Empresa creada exitosamente con ID: {}", response.getEmpresaId());

            usuarioEmpresaService.asignar(Long.parseLong(auth.getName()), response.getEmpresaId(), null);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Empresa creada exitosamente", response));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (RuntimeException e) {
            log.error("Error de negocio: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error inesperado creando empresa: ", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error interno al crear la empresa"));
        }
    }

    @Operation(summary = "Actualizar empresa completa",
        description = "Actualiza una empresa con todos sus datos, logo, certificado y configuración de Hacienda")
    @PutMapping(value = "/actualizar-completo/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CrearEmpresaCompletaResponse>> actualizarEmpresaCompleta(
        @PathVariable Long id,
        @RequestPart("datos") String datosJson,
        @RequestPart(value = "logo", required = false) MultipartFile logo,
        @RequestPart(value = "certificado", required = false) MultipartFile certificado,
        Authentication auth) {

        try {
            // 1. Parsear el JSON de los datos
            CrearEmpresaCompletaRequest request = objectMapper.readValue(
                datosJson,
                CrearEmpresaCompletaRequest.class
            );

            log.info("Actualizando empresa completa: {} (ID: {})", request.getNombreComercial(), id);

            // 2. Validar que la empresa existe
            Empresa empresaExistente = empresaService.buscarPorId(id);
            if (empresaExistente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Empresa no encontrada"));
            }

            // 3. Actualizar empresa
            CrearEmpresaCompletaResponse response = empresaCreacionService.actualizarEmpresaCompleta(
                id,
                request,
                logo,
                certificado,
                auth.getName()
            );

            log.info("✅ Empresa actualizada exitosamente con ID: {}", response.getEmpresaId());

            return ResponseEntity.ok(ApiResponse.ok(
                "Empresa actualizada exitosamente",
                response
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error actualizando empresa: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    @Operation(summary = "Validar certificado P12",
        description = "Valida un certificado P12 con su PIN sin guardarlo")
    @PostMapping(value = "/validar-certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> validarCertificado(
        @RequestPart("certificado") MultipartFile certificado,
        @RequestParam("pin") String pin) {

        try {
            // Esta funcionalidad permite validar el certificado antes de crear la empresa
            // Útil para dar feedback inmediato al usuario

            // TODO: Implementar en CertificadoService
            // boolean valido = certificadoService.validarCertificado(certificado, pin);

            return ResponseEntity.ok(ApiResponse.ok("Certificado válido", true));

        } catch (Exception e) {
            log.error("Error validando certificado: ", e);
            return ResponseEntity.ok(ApiResponse.error("Certificado inválido o PIN incorrecto"));
        }
    }
}