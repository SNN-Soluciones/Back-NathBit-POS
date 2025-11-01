package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.impresion.ImpresoraAndroidDTO;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoUsoImpresora;
import com.snnsoluciones.backnathbitpos.service.ImpresoraAndroidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/impresoras-android")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImpresoraAndroidController {

    private final ImpresoraAndroidService impresoraService;

    /**
     * Crear nueva impresora
     * POST /api/impresoras-android
     */
    @PostMapping
    public ResponseEntity<?> crear(
            @Valid @RequestBody ImpresoraAndroidDTO dto,
            Authentication authentication) {
        try {
            Long usuarioId = obtenerUsuarioId(authentication);
            ImpresoraAndroidDTO resultado = impresoraService.crear(dto, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Actualizar impresora existente
     * PUT /api/impresoras-android/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ImpresoraAndroidDTO dto,
            Authentication authentication) {
        try {
            Long usuarioId = obtenerUsuarioId(authentication);
            ImpresoraAndroidDTO resultado = impresoraService.actualizar(id, dto, usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Eliminar impresora
     * DELETE /api/impresoras-android/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            impresoraService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Impresora eliminada correctamente",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Obtener impresora por ID
     * GET /api/impresoras-android/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            ImpresoraAndroidDTO resultado = impresoraService.obtenerPorId(id);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Listar todas las impresoras de una sucursal
     * GET /api/impresoras-android/sucursal/{sucursalId}
     */
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<?> listarPorSucursal(@PathVariable Long sucursalId) {
        try {
            List<ImpresoraAndroidDTO> resultado = impresoraService.listarPorSucursal(sucursalId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Listar solo impresoras activas de una sucursal
     * GET /api/impresoras-android/sucursal/{sucursalId}/activas
     */
    @GetMapping("/sucursal/{sucursalId}/activas")
    public ResponseEntity<?> listarActivasPorSucursal(@PathVariable Long sucursalId) {
        try {
            List<ImpresoraAndroidDTO> resultado = impresoraService.listarActivasPorSucursal(sucursalId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Listar impresoras por tipo de uso
     * GET /api/impresoras-android/sucursal/{sucursalId}/tipo-uso/{tipoUso}
     */
    @GetMapping("/sucursal/{sucursalId}/tipo-uso/{tipoUso}")
    public ResponseEntity<?> listarPorTipoUso(
            @PathVariable Long sucursalId,
            @PathVariable TipoUsoImpresora tipoUso) {
        try {
            List<ImpresoraAndroidDTO> resultado = impresoraService.listarPorTipoUso(sucursalId, tipoUso);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Obtener impresora predeterminada
     * GET /api/impresoras-android/sucursal/{sucursalId}/predeterminada?tipoUso=FACTURAS
     */
    @GetMapping("/sucursal/{sucursalId}/predeterminada")
    public ResponseEntity<?> obtenerPredeterminada(
            @PathVariable Long sucursalId,
            @RequestParam(required = false) TipoUsoImpresora tipoUso) {
        try {
            ImpresoraAndroidDTO resultado = impresoraService.obtenerPredeterminada(sucursalId, tipoUso);
            if (resultado == null) {
                return ResponseEntity.ok(Map.of(
                        "mensaje", "No hay impresora predeterminada configurada"
                ));
            }
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Establecer impresora como predeterminada
     * PATCH /api/impresoras-android/{id}/predeterminada
     */
    @PatchMapping("/{id}/predeterminada")
    public ResponseEntity<?> establecerComoPredeterminada(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long usuarioId = obtenerUsuarioId(authentication);
            ImpresoraAndroidDTO resultado = impresoraService.establecerComoPredeterminada(id, usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Activar/Desactivar impresora
     * PATCH /api/impresoras-android/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {
        try {
            Boolean activa = body.get("activa");
            if (activa == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "El campo 'activa' es requerido"
                ));
            }
            
            Long usuarioId = obtenerUsuarioId(authentication);
            ImpresoraAndroidDTO resultado = impresoraService.cambiarEstado(id, activa, usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    // ==================== MÉTODO AUXILIAR ====================

    private Long obtenerUsuarioId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Map) {
            Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
            Object userId = principal.get("id");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
        }
        return null;
    }
}