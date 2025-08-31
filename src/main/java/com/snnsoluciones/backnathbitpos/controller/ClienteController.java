package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteBusquedaDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUpdateDTO;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
@Tag(name = "Clientes", description = "Gestión de clientes del sistema POS")
public class ClienteController {

  private final ModelMapper modelMapper;

  private final ClienteService clienteService;
  private final ClienteMapper clienteMapper;
  private final EmpresaService empresaService;

  // Helper para obtener el userId del token JWT
  private Long getCurrentUserId(Authentication auth) {
    return (Long) auth.getPrincipal();
  }

  // Helper para verificar si es rol de sistema
  private boolean esRolSistema(Authentication auth) {
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ROOT") ||
            a.getAuthority().equals("ROLE_SOPORTE"));
  }

  @Operation(summary = "Crear nuevo cliente (vía POS DTO)")
  @PostMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<?>> crear(
      @RequestBody ClientePOSDto dto,
      @RequestParam(name = "empresaId") Long empresaId) {
    Cliente cliente = this.clienteService.crear(dto, empresaId);
    return ResponseEntity.ok(ApiResponse.ok(clienteMapper.toDTO(cliente)));
  }

  @Operation(summary = "Buscar clientes por identificación")
  @GetMapping("/buscar-identificacion/{numeroIdentificacion}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<ClienteBusquedaDTO>> buscarPorIdentificacion(
      @PathVariable String numeroIdentificacion,
      @RequestParam(name = "empresaId") Long empresaId,
      Authentication auth) {

    try {
      Empresa empresa = empresaService.buscarPorId(empresaId);
      // Validar que la empresa existe
      if (Optional.ofNullable(empresa).isEmpty() && !esRolSistema(auth)) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Empresa no encontrada"));
      }

      List<Cliente> clientes = clienteService.buscarPorIdentificacion(empresaId,
          numeroIdentificacion);

      ClienteBusquedaDTO response = ClienteBusquedaDTO.builder()
          .numeroIdentificacion(numeroIdentificacion)
          .opciones(clientes.stream()
              .map(clienteMapper::toOpcionDTO)
              .collect(Collectors.toList()))
          .build();

      return ResponseEntity.ok(ApiResponse.ok(response));

    } catch (Exception e) {
      log.error("Error al buscar por identificación", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error al buscar cliente"));
    }
  }

  @Operation(summary = "Obtener cliente por ID")
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<ClienteDTO>> obtenerPorId(
      @PathVariable Long id,
      Authentication auth) {

    try {
      Cliente cliente = clienteService.obtenerPorId(id);

      // Los roles ROOT y SOPORTE pueden ver cualquier cliente
      // Los demás roles se validan por contexto en el frontend

      ClienteDTO response = clienteMapper.toDTO(cliente);
      return ResponseEntity.ok(ApiResponse.ok(response));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error al obtener cliente", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error al obtener cliente"));
    }
  }

  @Operation(summary = "Actualizar cliente")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
  public ResponseEntity<ApiResponse<ClienteDTO>> actualizar(
      @PathVariable Long id,
      @Valid @RequestBody ClientePOSDto dto,
      Authentication auth) {

    try {
      Cliente clienteActualizado = clienteService.actualizar(id, dto);

      ClienteDTO response = clienteMapper.toDTO(clienteActualizado);
      return ResponseEntity.ok(
          ApiResponse.ok("Cliente actualizado exitosamente", response)
      );

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error al actualizar cliente", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error al actualizar cliente"));
    }
  }

  @Operation(summary = "Eliminar cliente (desactivar)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> eliminar(
      @PathVariable Long id,
      Authentication auth) {

    try {
      Cliente cliente = clienteService.obtenerPorId(id);

      if (Objects.isNull(cliente)) {
        return ResponseEntity.notFound().build();
      }

      // Solo ROOT, SOPORTE, SUPER_ADMIN y ADMIN pueden eliminar

      clienteService.eliminar(id);
      return ResponseEntity.ok(
          ApiResponse.ok("Cliente eliminado exitosamente", null)
      );

    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error al eliminar cliente", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error al eliminar cliente"));
    }
  }

  @Operation(summary = "Obtener resumen de clientes por empresa")
  @GetMapping("/resumen")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<ClienteResumenDTO>> obtenerResumen(
      @RequestParam(name = "empresaId") Long empresaId,
      Authentication auth) {

    try {
      long totalClientes = clienteService.contarClientesPorEmpresa(empresaId);
      List<Cliente> clientesConExoneracion = clienteService.obtenerClientesConExoneracion(
          empresaId);

      ClienteResumenDTO resumen = ClienteResumenDTO.builder()
          .totalClientes(totalClientes)
          .clientesActivos(totalClientes) // Por ahora asumimos todos activos
          .clientesConExoneracion((long) clientesConExoneracion.size())
          .build();

      return ResponseEntity.ok(ApiResponse.ok(resumen));

    } catch (Exception e) {
      log.error("Error al obtener resumen", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error al obtener resumen"));
    }
  }

  @PostMapping("/{id}/emails")
  @Operation(summary = "Agregar email a cliente")
  public ResponseEntity<ApiResponse<ClienteDTO>> agregarEmail(
      @PathVariable Long id,
      @RequestBody Map<String, String> request) {

    String email = request.get("email");

    try {
      Cliente cliente = clienteService.agregarEmail(id, email);
      ClienteDTO response = clienteMapper.toDTO(cliente);

      return ResponseEntity.ok(
          ApiResponse.ok("Email agregado exitosamente", response)
      );
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @DeleteMapping("/{id}/emails")
  @Operation(summary = "Quitar email de cliente")
  public ResponseEntity<ApiResponse<ClienteDTO>> quitarEmail(
      @PathVariable Long id,
      @RequestParam String email) {

    try {
      Cliente cliente = clienteService.quitarEmail(id, email);
      ClienteDTO response = clienteMapper.toDTO(cliente);

      return ResponseEntity.ok(
          ApiResponse.ok("Email eliminado exitosamente", response)
      );
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    }
  }
}