package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionDTO;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Barrio;
import com.snnsoluciones.backnathbitpos.entity.Canton;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import com.snnsoluciones.backnathbitpos.entity.Distrito;
import com.snnsoluciones.backnathbitpos.entity.Provincia;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.UbicacionService;
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

@Slf4j
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Cliente Ubicación", description = "Gestión de ubicaciones de clientes")
public class ClienteUbicacionController {
    
    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;
    private final UbicacionService ubicacionService;
    
    @Operation(summary = "Obtener ubicación del cliente")
    @GetMapping("/{clienteId}/ubicacion")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<ClienteUbicacionDTO>> obtenerUbicacion(
            @PathVariable Long clienteId,
            Authentication auth) {
        
        try {
            // Verificar que el cliente existe
            Cliente cliente = clienteService.obtenerPorId(clienteId);
            
            ClienteUbicacion ubicacion = clienteService.obtenerUbicacion(clienteId);
            if (ubicacion == null) {
                return ResponseEntity.ok(
                    ApiResponse.ok("El cliente no tiene ubicación registrada", null)
                );
            }

            ClienteUbicacionDTO response = clienteMapper.toDTO(ubicacion);
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener ubicación del cliente", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener ubicación"));
        }
    }
    
    @Operation(summary = "Crear o actualizar ubicación del cliente")
    @PostMapping("/{clienteId}/ubicacion")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    public ResponseEntity<ApiResponse<ClienteUbicacionDTO>> guardarUbicacion(
            @PathVariable Long clienteId,
            @Valid @RequestBody ClienteUbicacionCreateDTO dto,
            Authentication auth) {
        
        try {
            // Verificar que el cliente existe
            Cliente cliente = clienteService.obtenerPorId(clienteId);
            
            // Obtener entidades de ubicación
            Provincia provincia = ubicacionService.buscarProvinciaPorId(
                    Integer.valueOf(dto.getProvinciaId()))
                .orElseThrow(() -> new IllegalArgumentException("Provincia no válida"));
            
            Canton canton = ubicacionService.buscarCantonPorId(Integer.valueOf(dto.getCantonId()))
                .orElseThrow(() -> new IllegalArgumentException("Cantón no válido"));
            
            Distrito distrito = ubicacionService.buscarDistritoPorId(
                    Integer.valueOf(dto.getDistritoId()))
                .orElseThrow(() -> new IllegalArgumentException("Distrito no válido"));
            
            Barrio barrio = null;
            if (dto.getBarrioId() != null) {
                barrio = ubicacionService.buscarBarrioPorId(Math.toIntExact(dto.getBarrioId()))
                    .orElse(null);
            }
            
            // Crear entidad de ubicación
            ClienteUbicacion ubicacion = clienteMapper.toEntity(dto);
            ubicacion.setProvincia(provincia);
            ubicacion.setCanton(canton);
            ubicacion.setDistrito(distrito);
            ubicacion.setBarrio(barrio);
            
            ClienteUbicacion ubicacionGuardada = clienteService.guardarUbicacion(clienteId, ubicacion);
            ClienteUbicacionDTO response = clienteMapper.toDTO(ubicacionGuardada);
            
            return new ResponseEntity<>(
                ApiResponse.ok("Ubicación guardada exitosamente", response),
                HttpStatus.CREATED
            );
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al guardar ubicación", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al guardar ubicación"));
        }
    }
    
    @Operation(summary = "Eliminar ubicación del cliente")
    @DeleteMapping("/{clienteId}/ubicacion")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarUbicacion(
            @PathVariable Long clienteId,
            Authentication auth) {
        
        try {
            // Verificar que el cliente existe
            Cliente cliente = clienteService.obtenerPorId(clienteId);
            
            clienteService.eliminarUbicacion(clienteId);
            return ResponseEntity.ok(
                ApiResponse.ok("Ubicación eliminada exitosamente", null)
            );
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al eliminar ubicación", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al eliminar ubicación"));
        }
    }
}