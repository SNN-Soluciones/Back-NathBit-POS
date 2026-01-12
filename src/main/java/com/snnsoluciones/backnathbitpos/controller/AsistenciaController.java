package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.asistencia.AsistenciaDTO;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaRequest;
import com.snnsoluciones.backnathbitpos.dto.asistencia.MarcarAsistenciaResponse;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller para gestión de asistencias (entradas/salidas)
 * 
 * Endpoints principales:
 * - POST /api/asistencia/marcar - Marcar entrada o salida
 * - GET /api/asistencia/historial - Ver historial personal
 * - GET /api/asistencia/empresa/{empresaId} - Ver asistencias de empresa (Admin)
 */
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asistencias", description = "Control de entradas y salidas de usuarios")
public class AsistenciaController {
    
    private final AsistenciaService asistenciaService;
    
    // ==================== MARCAR ENTRADA/SALIDA ====================
    
    @Operation(summary = "Marcar entrada o salida", 
               description = "Permite al usuario marcar su entrada o salida del día")
    @PostMapping("/marcar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MarcarAsistenciaResponse>> marcarAsistencia(
            @Valid @RequestBody MarcarAsistenciaRequest request,
            Authentication authentication) {
        
        log.info("POST /api/asistencia/marcar - Tipo: {}", request.getTipo());
        
        try {
            // Obtener datos del usuario del JWT
            Long usuarioId = obtenerUsuarioIdDeAuth(authentication);
            Long empresaId = obtenerEmpresaIdDeAuth(authentication);
            Long sucursalId = obtenerSucursalIdDeAuth(authentication);
            
            MarcarAsistenciaResponse response = asistenciaService.marcarAsistencia(
                usuarioId, 
                empresaId, 
                sucursalId, 
                request
            );
            
            String mensaje = "ENTRADA".equals(request.getTipo()) 
                ? "Entrada marcada exitosamente" 
                : "Salida marcada exitosamente";
            
            return ResponseEntity.ok(ApiResponse.success(mensaje, response));
            
        } catch (Exception e) {
            log.error("Error marcando asistencia: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== HISTORIAL PERSONAL ====================
    
    @Operation(summary = "Ver mi historial de asistencias", 
               description = "Obtiene el historial de asistencias del usuario autenticado")
    @GetMapping("/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AsistenciaDTO>>> obtenerHistorial(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        
        log.info("GET /api/asistencia/historial");
        
        try {
            Long usuarioId = obtenerUsuarioIdDeAuth(authentication);
            
            // Por defecto: últimos 30 días
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusDays(30);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }
            
            List<AsistenciaDTO> historial = asistenciaService.obtenerHistorial(
                usuarioId, 
                fechaInicio, 
                fechaFin
            );
            
            return ResponseEntity.ok(ApiResponse.success("Historial obtenido", historial));
            
        } catch (Exception e) {
            log.error("Error obteniendo historial: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== ENDPOINTS ADMIN ====================
    
    @Operation(summary = "Ver asistencias de empresa por fecha", 
               description = "Lista todas las asistencias de una empresa en una fecha (Admin)")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AsistenciaDTO>>> listarAsistenciasEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        
        log.info("GET /api/asistencia/empresa/{}", empresaId);
        
        try {
            // Por defecto: hoy
            if (fecha == null) {
                fecha = LocalDate.now();
            }
            
            List<AsistenciaDTO> asistencias = asistenciaService
                .listarAsistenciasPorEmpresaYFecha(empresaId, fecha);
            
            return ResponseEntity.ok(ApiResponse.success("Asistencias obtenidas", asistencias));
            
        } catch (Exception e) {
            log.error("Error listando asistencias: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "Ver usuarios presentes", 
               description = "Lista usuarios con entrada activa (sin salida) en una empresa (Admin)")
    @GetMapping("/empresa/{empresaId}/presentes")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AsistenciaDTO>>> listarUsuariosPresentes(
            @PathVariable Long empresaId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        
        log.info("GET /api/asistencia/empresa/{}/presentes", empresaId);
        
        try {
            // Por defecto: hoy
            if (fecha == null) {
                fecha = LocalDate.now();
            }
            
            List<AsistenciaDTO> presentes = asistenciaService
                .listarUsuariosPresentes(empresaId, fecha);
            
            return ResponseEntity.ok(
                ApiResponse.success("Usuarios presentes obtenidos", presentes)
            );
            
        } catch (Exception e) {
            log.error("Error listando usuarios presentes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ==================== HELPERS ====================
    
    /**
     * Extrae el ID del usuario del Authentication
     */
    private Long obtenerUsuarioIdDeAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof ContextoUsuario) {
            return ((ContextoUsuario) principal).getUserId();
        }
        
        throw new RuntimeException("No se pudo obtener el ID del usuario");
    }
    
    /**
     * Extrae el ID de la empresa del Authentication
     */
    private Long obtenerEmpresaIdDeAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof ContextoUsuario) {
            Long empresaId = ((ContextoUsuario) principal).getEmpresaId();
            if (empresaId == null) {
                throw new RuntimeException("No se pudo obtener el ID de la empresa");
            }
            return empresaId;
        }
        
        throw new RuntimeException("No se pudo obtener el ID de la empresa");
    }
    
    /**
     * Extrae el ID de la sucursal del Authentication
     */
    private Long obtenerSucursalIdDeAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof ContextoUsuario) {
            Long sucursalId = ((ContextoUsuario) principal).getSucursalId();
            if (sucursalId == null) {
                throw new RuntimeException("No se pudo obtener el ID de la sucursal");
            }
            return sucursalId;
        }
        
        throw new RuntimeException("No se pudo obtener el ID de la sucursal");
    }
}