package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionDTO;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Cliente Exoneración", description = "Gestión de exoneraciones de clientes")
public class ClienteExoneracionController {
    
    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;
    
    @Operation(summary = "Obtener exoneraciones del cliente")
    @GetMapping("/{clienteId}/exoneraciones")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<ClienteExoneracionDTO>>> obtenerExoneraciones(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "false") boolean soloVigentes,
            Authentication auth) {
        
        try {
            // Verificar que el cliente existe
            Cliente cliente = clienteService.obtenerPorId(clienteId);
            
            List<ClienteExoneracion> exoneraciones;
            if (soloVigentes) {
                exoneraciones = clienteService.obtenerExoneracionesVigentes(clienteId);
            } else {
                // Obtener todas las activas
                exoneraciones = cliente.getExoneraciones().stream()
                    .filter(ClienteExoneracion::getActivo)
                    .collect(Collectors.toList());
            }
            
            List<ClienteExoneracionDTO> response = exoneraciones.stream()
                .map(clienteMapper::toDTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener exoneraciones", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener exoneraciones"));
        }
    }

    @Operation(summary = "Agregar exoneración al cliente")
    @PostMapping("/{clienteId}/exoneraciones")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<ClienteExoneracionDTO>> agregarExoneracion(
        @PathVariable Long clienteId,
        @Valid @RequestBody ClienteExoneracionCreateDTO dto,
        Authentication auth) {

        try {
            // Verificar que el cliente existe
            Cliente cliente = clienteService.obtenerPorId(clienteId);

            ClienteExoneracion exoneracion = clienteMapper.toEntity(dto);
            ClienteExoneracion exoneracionGuardada = clienteService.agregarExoneracion(clienteId, exoneracion);
            ClienteExoneracionDTO response = clienteMapper.toDTO(exoneracionGuardada);

            return new ResponseEntity<>(
                ApiResponse.ok("Exoneración agregada exitosamente", response),
                HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al agregar exoneración", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al agregar exoneración"));
        }
    }

    @Operation(summary = "Actualizar exoneración")
    @PutMapping("/exoneraciones/{exoneracionId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClienteExoneracionDTO>> actualizarExoneracion(
        @PathVariable Long exoneracionId,
        @Valid @RequestBody ClienteExoneracionCreateDTO dto,
        Authentication auth) {

        try {
            ClienteExoneracion exoneracion = clienteMapper.toEntity(dto);
            ClienteExoneracion exoneracionActualizada = clienteService.actualizarExoneracion(exoneracionId, exoneracion);
            ClienteExoneracionDTO response = clienteMapper.toDTO(exoneracionActualizada);

            return ResponseEntity.ok(
                ApiResponse.ok("Exoneración actualizada exitosamente", response)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al actualizar exoneración", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al actualizar exoneración"));
        }
    }

    @Operation(summary = "Desactivar exoneración")
    @DeleteMapping("/exoneraciones/{exoneracionId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> desactivarExoneracion(
        @PathVariable Long exoneracionId,
        Authentication auth) {

        try {
            clienteService.desactivarExoneracion(exoneracionId);
            return ResponseEntity.ok(
                ApiResponse.ok("Exoneración desactivada exitosamente", null)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al desactivar exoneración", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al desactivar exoneración"));
        }
    }

    @Operation(summary = "Procesar exoneraciones vencidas")
    @PostMapping("/exoneraciones/procesar-vencidas")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> procesarExoneracionesVencidas(Authentication auth) {

        try {
            clienteService.procesarExoneracionesVencidas();
            return ResponseEntity.ok(
                ApiResponse.ok("Exoneraciones vencidas procesadas exitosamente", null)
            );

        } catch (Exception e) {
            log.error("Error al procesar exoneraciones vencidas", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al procesar exoneraciones vencidas"));
        }
    }
}