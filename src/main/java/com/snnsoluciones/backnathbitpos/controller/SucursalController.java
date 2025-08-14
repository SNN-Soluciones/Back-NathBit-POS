package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sucursal.SucursalRequest;
import com.snnsoluciones.backnathbitpos.dto.sucursal.SucursalResponse;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import com.snnsoluciones.backnathbitpos.service.UbicacionService;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@Tag(name = "Sucursales", description = "Gestión de sucursales")
public class SucursalController {

  private final SucursalService sucursalService;
  private final EmpresaService empresaService;
  private final UsuarioService usuarioService;
  private final UsuarioEmpresaService usuarioEmpresaService;
  private final UbicacionService ubicacionService;

  @Operation(summary = "Listar sucursales por empresa")
  @GetMapping("/empresa/{empresaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<SucursalResponse>>> listarPorEmpresa(
      @PathVariable Long empresaId) {

    List<Sucursal> sucursales = sucursalService.listarPorEmpresa(empresaId);
    List<SucursalResponse> response = sucursales.stream()
        .map(this::convertirAResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(summary = "Obtener sucursal por ID")
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<SucursalResponse>> obtenerPorId(@PathVariable Long id) {
    Sucursal sucursal = sucursalService.buscarPorId(id)
        .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

    return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(sucursal)));
  }

  @Operation(summary = "Crear nueva sucursal")
  @PostMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<SucursalResponse>> crear(
      @Valid @RequestBody SucursalRequest request) {

    // Validar código único
    if (sucursalService.existsNumeroSucursalYEmpresaId(
        request.getNumeroSucursal(), request.getEmpresaId())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("El código ya existe"));
    }

    Sucursal sucursal = convertirAEntity(request);
    Sucursal nuevaSucursal = sucursalService.crear(sucursal);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok("Sucursal creada", convertirAResponse(nuevaSucursal)));
  }

  @Operation(summary = "Actualizar sucursal")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<SucursalResponse>> actualizar(
      @PathVariable Long id,
      @Valid @RequestBody SucursalRequest request) {

    Sucursal sucursal = convertirAEntity(request);
    Sucursal actualizada = sucursalService.actualizar(id, sucursal);

    return ResponseEntity.ok(
        ApiResponse.ok("Sucursal actualizada", convertirAResponse(actualizada)));
  }

  @Operation(summary = "Eliminar sucursal")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
    sucursalService.eliminar(id);
    return ResponseEntity.ok(ApiResponse.ok("Sucursal eliminada", null));
  }

  @Operation(summary = "Listar sucursales de una empresa accesibles por usuario")
  @GetMapping("/usuario/{usuarioId}/empresa/{empresaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<SucursalResponse>>> listarPorUsuarioYEmpresa(
      @PathVariable(name = "usuarioId") Long usuarioId,
      @PathVariable(name = "empresaId") Long empresaId) {

    // Validación de seguridad
    Long usuarioActualId = getCurrentUserId();
    Usuario usuarioActual = usuarioService.buscarPorId(usuarioActualId)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    // Si no es ROOT/SOPORTE, validar que sea su propio usuario
    if (!usuarioActual.esRolSistema() && !usuarioActualId.equals(usuarioId)) {/**/
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("Solo puede ver sus propias sucursales"));
    }

    // Validar que el usuario tenga acceso a esa empresa
    if (!usuarioActual.esRolSistema()) {
      boolean tieneAccesoEmpresa = usuarioEmpresaService.tieneAcceso(usuarioId, empresaId, null);
      if (!tieneAccesoEmpresa) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("No tiene acceso a esta empresa"));
      }
    }

    List<Sucursal> sucursales = sucursalService.listarPorUsuarioYEmpresa(usuarioId, empresaId);
    List<SucursalResponse> response = sucursales.stream()
        .map(this::convertirAResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  private Long getCurrentUserId() {
    return (Long) org.springframework.security.core.context.SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();
  }

  // Métodos de conversión
  private SucursalResponse convertirAResponse(Sucursal sucursal) {
    SucursalResponse response = new SucursalResponse();
    response.setId(sucursal.getId());
    response.setNombre(sucursal.getNombre());
    response.setTelefono(sucursal.getTelefono());
    response.setEmail(sucursal.getEmail());
    response.setActiva(sucursal.getActiva());
    response.setModoFacturacion(sucursal.getModoFacturacion());
    response.setEmpresaId(sucursal.getEmpresa().getId());
    response.setEmpresaNombre(sucursal.getEmpresa().getNombreComercial());
    response.setCreatedAt(sucursal.getCreatedAt());
    response.setUpdatedAt(sucursal.getUpdatedAt());
    return response;
  }

  private Sucursal convertirAEntity(SucursalRequest request) {
    Sucursal sucursal = new Sucursal();
    sucursal.setNombre(request.getNombre());
    sucursal.setTelefono(request.getTelefono());
    sucursal.setEmail(request.getEmail());
    sucursal.setModoFacturacion(request.getModoFacturacion());
    sucursal.setActiva(request.getActiva());

    if (request.getProvinciaId() != null && request.getCantonId() != null
        && request.getDistritoId() != null && request.getBarrioId() != null) {
      sucursal.setProvincia(
          ubicacionService.buscarProvinciaPorId(request.getProvinciaId()).orElseThrow(
              () -> new RuntimeException("Provincia no encontrada")
          ));
      sucursal.setCanton(
          ubicacionService.buscarCantonPorId(request.getCantonId()).orElseThrow(
              () -> new RuntimeException("Canton no encontrado")
          ));
      sucursal.setDistrito(
          ubicacionService.buscarDistritoPorId(request.getDistritoId()).orElseThrow(
              () -> new RuntimeException("Distrito no encontrado")
          ));
      sucursal.setBarrio(
          ubicacionService.buscarBarrioPorId(request.getBarrioId()).orElseThrow(
              () -> new RuntimeException("Barrio no encontrado")
          ));
    }
    sucursal.setNumeroSucursal(request.getNumeroSucursal());
    if (request.getEmpresaId() != null) {
      sucursal.setEmpresa(empresaService.buscarPorId(request.getEmpresaId()
      ));
    }
    sucursal.setOtrasSenas(request.getOtrasSenas());
    sucursal.setTerminales(mapTerminalesToEntity(request));

    // Establecer empresa
    Empresa empresa = empresaService.buscarPorId(request.getEmpresaId());
    if (empresa == null) {
      throw new RuntimeException("Empresa no encontrada");
    }
    sucursal.setEmpresa(empresa);

    return sucursal;
  }

  private List<Terminal> mapTerminalesToEntity(SucursalRequest request) {
    List<Terminal> terminales = new ArrayList<>();
    for (TerminalRequest terminal : request.getTerminales()) {
      Terminal terminalEntity = new Terminal();

      terminalEntity.setNumeroTerminal(terminal.getNumeroTerminal());
      terminalEntity.setNombre(terminal.getNombre());
      terminalEntity.setDescripcion(terminal.getDescripcion());
      terminalEntity.setActiva(terminal.getActiva());
      terminalEntity.setImprimirAutomatico(terminal.getImprimirAutomatico());
      terminalEntity.setTipoImpresion(terminal.getTipoImpresion());

      terminalEntity.setConsecutivoOrdenPedido(terminal.getConsecutivoOrdenPedido());
      terminalEntity.setConsecutivoFacturaElectronica(terminal.getConsecutivoFacturaElectronica());
      terminalEntity.setConsecutivoTiqueteElectronico(terminal.getConsecutivoTiqueteElectronico());
      terminalEntity.setConsecutivoNotaCredito(terminal.getConsecutivoNotaCredito());
      terminalEntity.setConsecutivoNotaDebito(terminal.getConsecutivoNotaDebito());
      terminalEntity.setConsecutivoFacturaCompra(terminal.getConsecutivoFacturaCompra());
      terminalEntity.setConsecutivoFacturaExportacion(terminal.getConsecutivoFacturaExportacion());
      terminalEntity.setConsecutivoReciboPago(terminal.getConsecutivoReciboPago());
      terminalEntity.setConsecutivoTiqueteInterno(terminal.getConsecutivoTiqueteInterno());
      terminalEntity.setConsecutivoFacturaInterna(terminal.getConsecutivoFacturaInterna());
      terminalEntity.setConsecutivoProforma(terminal.getConsecutivoProforma());

      terminales.add(terminalEntity);
    }
    return terminales;
  }
}