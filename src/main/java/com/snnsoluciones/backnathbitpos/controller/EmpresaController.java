package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.common.PageResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.CertificadoResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.UrlCertificadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gestión de empresas")
@Slf4j
public class EmpresaController {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Listar todas las empresas")
    @GetMapping("/root")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<List<EmpresaResponse>>> listar() {
        List<Empresa> empresas = empresaService.listarTodas();
        List<EmpresaResponse> response = empresas.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Obtener empresa por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtenerPorId(@PathVariable Long id) {
        Empresa empresa = empresaService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(empresa)));
    }

    @Operation(summary = "Crear nueva empresa")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> crear(
        @Valid @RequestBody EmpresaRequest request) {

        // Validar código único
        if (empresaService.existeCodigo(request.getCodigo())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El código ya existe"));
        }

        Empresa empresa = convertirAEntity(request);
        Empresa nuevaEmpresa = empresaService.crear(empresa);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Empresa creada", convertirAResponse(nuevaEmpresa)));
    }

    @Operation(summary = "Actualizar empresa")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody EmpresaRequest request) {

        Empresa empresa = convertirAEntity(request);
        Empresa actualizada = empresaService.actualizar(id, empresa);

        return ResponseEntity.ok(ApiResponse.ok("Empresa actualizada", convertirAResponse(actualizada)));
    }

    @Operation(summary = "Eliminar empresa")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Empresa eliminada", null));
    }

    @Operation(summary = "Listar empresas accesibles por usuario con paginación")
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<EmpresaResponse>>> listarPorUsuario(
        @PathVariable(name = "usuarioId") Long usuarioId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "sortBy", defaultValue = "nombre") String sortBy,
        @RequestParam(name = "sortDirection", defaultValue = "ASC") String sortDirection) {

        // Validación de permisos
        Long usuarioActualId = getCurrentUserId();
        Usuario usuarioActual = usuarioService.buscarPorId(usuarioActualId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuarioActual.esRolSistema() && !usuarioActualId.equals(usuarioId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Solo puede ver sus propias empresas"));
        }

        // Crear Pageable
        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Obtener página de empresas
        Page<Empresa> empresasPage = empresaService.listarPorUsuarioPaginado(usuarioId, pageable);

        // Convertir a response
        Page<EmpresaResponse> responsePage = empresasPage.map(this::convertirAResponse);
        PageResponse<EmpresaResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    /**
     * Subir certificado de facturación electrónica
     */
    @Operation(summary = "Subir certificado P12 para facturación electrónica")
    @PostMapping(value = "/{id}/certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CertificadoResponse>> subirCertificado(
        @PathVariable Long id,
        @RequestParam("certificado") MultipartFile certificado,
        @RequestParam("pin") String pin) {

        log.info("Subiendo certificado para empresa ID: {}", id);

        try {
            // Validar que el archivo sea .p12
            String filename = certificado.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".p12")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El archivo debe ser un certificado .p12"));
            }

            // Validar tamaño (máximo 10MB)
            if (certificado.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El certificado no debe superar los 10MB"));
            }

            CertificadoResponse response = empresaService.subirCertificado(id, certificado, pin);
            return ResponseEntity.ok(ApiResponse.ok("Certificado procesado exitosamente", response));
        } catch (Exception e) {
            log.error("Error al subir certificado: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al procesar certificado: " + e.getMessage()));
        }
    }

    /**
     * Obtener URL pre-firmada del certificado
     */
    @Operation(summary = "Obtener URL temporal para descargar certificado")
    @GetMapping("/{id}/certificado-url")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UrlCertificadoResponse>> obtenerUrlCertificado(
        @PathVariable Long id) {

        try {
            UrlCertificadoResponse response = empresaService.generarUrlCertificado(id);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al generar URL: " + e.getMessage()));
        }
    }

    /**
     * Eliminar certificado
     */
    @Operation(summary = "Eliminar certificado de la empresa")
    @DeleteMapping("/{id}/certificado")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarCertificado(@PathVariable Long id) {
        try {
            empresaService.eliminarCertificado(id);
            return ResponseEntity.ok(ApiResponse.ok("Certificado eliminado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al eliminar certificado: " + e.getMessage()));
        }
    }

    /**
     * Subir logo de la empresa
     */
    @Operation(summary = "Subir logo de la empresa")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> subirLogo(
        @PathVariable Long id,
        @RequestParam("logo") MultipartFile logo) {

        log.info("Subiendo logo para empresa ID: {}", id);

        try {
            // Validar formato
            String contentType = logo.getContentType();
            if (!isValidImageType(contentType)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Formato de imagen no válido. Use JPG, PNG o SVG"));
            }

            // Validar tamaño (5MB)
            if (logo.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El logo no debe superar los 5MB"));
            }

            String logoUrl = empresaService.subirLogo(id, logo);
            Map<String, String> response = new HashMap<>();
            response.put("url", logoUrl);
            response.put("mensaje", "Logo subido correctamente");

            return ResponseEntity.ok(ApiResponse.ok("Logo subido exitosamente", response));

        } catch (Exception e) {
            log.error("Error al subir logo: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al subir logo: " + e.getMessage()));
        }
    }

    /**
     * Eliminar logo
     */
    @Operation(summary = "Eliminar logo de la empresa")
    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarLogo(@PathVariable Long id) {
        try {
            empresaService.eliminarLogo(id);
            return ResponseEntity.ok(ApiResponse.ok("Logo eliminado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al eliminar logo: " + e.getMessage()));
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/svg+xml")
        );
    }

    // Métodos de conversión
    private EmpresaResponse convertirAResponse(Empresa empresa) {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(empresa.getId());
        response.setNombre(empresa.getNombre());
        response.setCodigo(empresa.getCodigo());
        response.setTipoIdentificacion(empresa.getTipoIdentificacion());
        response.setIdentificacion(empresa.getIdentificacion());
        response.setDireccion(empresa.getDireccion());
        response.setTelefono(empresa.getTelefono());
        response.setEmail(empresa.getEmail());
        response.setActiva(empresa.getActiva());
        response.setCreatedAt(empresa.getCreatedAt());
        response.setUpdatedAt(empresa.getUpdatedAt());
        return response;
    }

    private Empresa convertirAEntity(EmpresaRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombre(request.getNombre());
        empresa.setCodigo(request.getCodigo());
        empresa.setTipoIdentificacion(request.getTipoIdentificacion());
        empresa.setIdentificacion(request.getIdentificacion());
        empresa.setDireccion(request.getDireccion());
        empresa.setTelefono(request.getTelefono());
        empresa.setEmail(request.getEmail());
        empresa.setActiva(request.getActiva());
        return empresa;
    }

    private Long getCurrentUserId() {
        return (Long) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }
}