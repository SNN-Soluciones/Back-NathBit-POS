package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteBusquedaDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteCreditoDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUpdateDTO;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.repository.CuentaPorCobrarRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
  private final ClienteRepository clienteRepository;
  private final ClienteMapper clienteMapper;
  private final EmpresaService empresaService;
  private final CuentaPorCobrarRepository cuentaPorCobrarRepository;

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

  // En ClienteController.java agregar:

  @GetMapping("/{id}/estado-credito")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse> obtenerEstadoCredito(@PathVariable Long id) {
    Cliente cliente = clienteService.obtenerPorId(id);

    Map<String, Object> estadoCredito = new HashMap<>();
    estadoCredito.put("puedeComprar", clienteService.puedeComprarACredito(id, BigDecimal.ZERO));
    estadoCredito.put("permiteCredito", cliente.getPermiteCredito());
    estadoCredito.put("limiteCredito", cliente.getLimiteCredito());
    estadoCredito.put("saldoActual", cliente.getSaldoActual());
    estadoCredito.put("disponible", cliente.getLimiteCredito().subtract(cliente.getSaldoActual()));
    estadoCredito.put("bloqueadoPorMora", cliente.getBloqueadoPorMora());
    estadoCredito.put("estadoCredito", cliente.getEstadoCredito());
    estadoCredito.put("diasCredito", cliente.getDiasCredito());

    return ResponseEntity.ok(ApiResponse.success("Estado de crédito", estadoCredito));
  }

  @PostMapping("/{id}/desbloquear-credito")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse> desbloquearCredito(
      @PathVariable Long id,
      @RequestBody Map<String, String> request) {

    String motivo = request.getOrDefault("motivo", "Sin especificar");
    clienteService.desbloquearCredito(id, motivo);

    return ResponseEntity.ok(ApiResponse.success(
        "Cliente desbloqueado exitosamente", null
    ));
  }

  // En ClienteController.java agregar:

  @GetMapping("/bloqueados-mora")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse> obtenerClientesBloqueados(
      @PageableDefault(size = 20, sort = "fechaUltimoPago,desc") Pageable pageable) {

    Page<Cliente> clientesBloqueados = clienteRepository.findByBloqueadoPorMora(true, pageable);

    // Mapear a DTO con información relevante
    Page<Map<String, Object>> resultado = clientesBloqueados.map(cliente -> {
      Map<String, Object> clienteInfo = new HashMap<>();
      clienteInfo.put("id", cliente.getId());
      clienteInfo.put("nombre", cliente.getRazonSocial());
      clienteInfo.put("identificacion", cliente.getNumeroIdentificacion());
      clienteInfo.put("telefono", cliente.getTelefonoNumero());
      clienteInfo.put("saldoActual", cliente.getSaldoActual());
      clienteInfo.put("limiteCredito", cliente.getLimiteCredito());
      clienteInfo.put("fechaUltimoPago", cliente.getFechaUltimoPago());
      clienteInfo.put("diasCredito", cliente.getDiasCredito());

      // Obtener la cuenta más vencida
      List<CuentaPorCobrar> cuentasVencidas = cuentaPorCobrarRepository
          .findByClienteIdOrderByFechaVencimientoAsc(cliente.getId())
          .stream()
          .filter(c -> c.getEstado() == EstadoCuenta.VENCIDA)
          .collect(Collectors.toList());

      if (!cuentasVencidas.isEmpty()) {
        CuentaPorCobrar masMorosa = cuentasVencidas.get(0);
        clienteInfo.put("diasMoraMayor", masMorosa.getDiasMora());
        clienteInfo.put("facturaMasMorosa", masMorosa.getFactura().getConsecutivo());
      }

      return clienteInfo;
    });

    return ResponseEntity.ok(ApiResponse.success(
        "Clientes bloqueados por mora",
        resultado
    ));
  }

  // En ClienteController.java agregar también:

  @GetMapping("/credito/buscar/{empresaId}")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse> buscarClientesCredito(
      @PathVariable Long empresaId,
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) Boolean bloqueados,
      @RequestParam(required = false) Boolean conSaldo,
      @PageableDefault(size = 20) Pageable pageable) {

    Page<Cliente> clientes;


    // Construir query según filtros
    if (bloqueados != null && bloqueados) {
      clientes = clienteRepository.findByBloqueadoPorMora(true, pageable);
    } else if (conSaldo != null && conSaldo) {
      // Necesitas crear este método en el repository
      clientes = clienteRepository.findBySaldoActualGreaterThan(BigDecimal.ZERO, pageable);
    } else if (busqueda != null && !busqueda.trim().isEmpty()) {
      clientes = clienteRepository.buscarPorEmpresa(
          empresaId,
          busqueda,
          pageable
      );
    } else {
      clientes = clienteRepository.findByPermiteCredito(true, pageable);
    }

    // Mapear con info de crédito
    Page<ClienteCreditoDTO> resultado = clientes.map(cliente -> {
      ClienteCreditoDTO dto = new ClienteCreditoDTO();
      dto.setId(cliente.getId());
      dto.setNombre(cliente.getRazonSocial());
      dto.setIdentificacion(cliente.getNumeroIdentificacion());
      dto.setPermiteCredito(cliente.getPermiteCredito());
      dto.setLimiteCredito(cliente.getLimiteCredito());
      dto.setSaldoActual(cliente.getSaldoActual());
      dto.setCreditoDisponible(
          cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0 ?
              cliente.getLimiteCredito().subtract(cliente.getSaldoActual()) :
              new BigDecimal("999999999") // Sin límite
      );
      dto.setBloqueadoPorMora(cliente.getBloqueadoPorMora());
      dto.setEstadoCredito(cliente.getEstadoCredito());
      return dto;
    });

    return ResponseEntity.ok(ApiResponse.success("Clientes", resultado));
  }
}