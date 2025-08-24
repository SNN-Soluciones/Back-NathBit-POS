package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionDTO;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    private final ClienteExoneracionRepository clienteExoneracionRepository;
    
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

            if (Objects.isNull(cliente)) {
                return ResponseEntity.notFound().build();
            }

            // Convertir DTO a entidad
            ClienteExoneracion exoneracion = clienteMapper.toEntity(dto);

            // Extraer la lista de códigos CABYS del DTO
            List<String> codigosCabys = dto.getCodigosCabys();

            // Llamar al service con los códigos CABYS
            ClienteExoneracion exoneracionGuardada = clienteService.agregarExoneracion(clienteId, exoneracion, codigosCabys);

            // Convertir a DTO para respuesta
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
            // Convertir DTO a entidad
            ClienteExoneracion exoneracion = clienteMapper.toEntity(dto);

            // Extraer la lista de códigos CABYS del DTO
            List<String> codigosCabys = dto.getCodigosCabys();

            // Llamar al service con los códigos CABYS
            ClienteExoneracion exoneracionActualizada = clienteService.actualizarExoneracion(exoneracionId, exoneracion, codigosCabys);

            // Convertir a DTO para respuesta
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

    @Operation(summary = "Obtener códigos CABYS de una exoneración")
    @GetMapping("/exoneraciones/{exoneracionId}/cabys")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    public ResponseEntity<ApiResponse<Page<String>>> obtenerCodigosCabys(
        @PathVariable Long exoneracionId,
        @PageableDefault(size = 20) Pageable pageable,
        Authentication auth) {

        try {
            // Verificar que la exoneración existe
            ClienteExoneracion exoneracion = clienteService.obtenerExoneracionPorId(exoneracionId);

            // Obtener los códigos paginados
            List<String> todosLosCodigos = exoneracion.getCabysAutorizados().stream()
                .map(cec -> cec.getCabys().getCodigo())
                .sorted()
                .collect(Collectors.toList());

            // Crear página manualmente
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), todosLosCodigos.size());

            Page<String> page = new PageImpl<>(
                todosLosCodigos.subList(start, end),
                pageable,
                todosLosCodigos.size()
            );

            return ResponseEntity.ok(ApiResponse.ok(page));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener códigos CABYS", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error al obtener códigos CABYS"));
        }
    }
}