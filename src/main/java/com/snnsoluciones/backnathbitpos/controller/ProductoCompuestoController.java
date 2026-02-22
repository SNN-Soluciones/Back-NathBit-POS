package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ActualizarConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CrearConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.ConfiguracionFlujoDTO;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.OpcionSlotConSubConfigDTO;
import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos/{productoId}/compuesto")
@RequiredArgsConstructor
@Tag(name = "Productos Compuestos", description = "Gestión de productos personalizables")
public class ProductoCompuestoController {

  private final ProductoCompuestoService compuestoService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Configurar producto compuesto",
      description = "Crea la configuración de slots y opciones para un producto compuesto")
  public ResponseEntity<ApiResponse<ProductoCompuestoDto>> configurar(
      @RequestParam Long empresaId,
      @PathVariable Long productoId,
      @Valid @RequestBody ProductoCompuestoRequest request) {

    log.info("Configurando producto compuesto: {} para empresa: {}", productoId, empresaId);

    try {
      ProductoCompuestoDto resultado = compuestoService.crear(empresaId, productoId, request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.ok("Producto compuesto configurado exitosamente", resultado));
    } catch (Exception e) {
      log.error("Error configurando producto compuesto", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
  @Operation(summary = "Obtener configuración",
      description = "Obtiene la configuración completa del producto compuesto")
  public ResponseEntity<ApiResponse<ProductoCompuestoDto>> obtener(
      @RequestParam Long empresaId,
      @PathVariable Long productoId) {

    log.info("GET compuesto - productoId: {}, empresaId: {}", productoId, empresaId);

    try {
      ProductoCompuestoDto compuesto = compuestoService.buscarPorProductoId(empresaId, productoId);
      return ResponseEntity.ok(ApiResponse.ok("Configuración obtenida", compuesto));

    } catch (ResourceNotFoundException e) {
      log.info("Producto {} no tiene configuración de compuesto", productoId);
      return ResponseEntity.ok(
          ApiResponse.<ProductoCompuestoDto>builder()
              .success(true)
              .message("Sin configuración previa")
              .data(null)
              .build()
      );

    } catch (BusinessException e) {
      log.warn("Error de negocio: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));

    } catch (Exception e) {
      log.error("Error obteniendo configuración de compuesto", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al obtener configuración: " + e.getMessage()));
    }
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<ProductoCompuestoDto>> actualizar(
      @RequestParam Long empresaId,
      @PathVariable Long productoId,
      @Valid @RequestBody ProductoCompuestoRequest request) {

    log.info("Actualizando producto compuesto: {}", productoId);

    try {
      ProductoCompuestoDto resultado = compuestoService.actualizarCompleto(empresaId, productoId, request);
      return ResponseEntity.ok(ApiResponse.ok("Configuración actualizada", resultado));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @DeleteMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
  @Operation(summary = "Eliminar configuración",
      description = "Elimina toda la configuración del producto compuesto")
  public ResponseEntity<ApiResponse<Void>> eliminar(
      @RequestParam Long empresaId,
      @PathVariable Long productoId) {

    log.info("Eliminando configuración de producto compuesto: {}", productoId);

    try {
      compuestoService.eliminar(empresaId, productoId);
      return ResponseEntity.ok(ApiResponse.ok("Configuración eliminada", null));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @GetMapping("/slots/{slotId}/opciones")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
  @Operation(summary = "Obtener opciones de un slot",
      description = "Obtiene opciones dinámicas (familia) o manuales según configuración del slot")
  public ResponseEntity<ApiResponse<List<OpcionSlotDTO>>> obtenerOpcionesSlot(
      @PathVariable Long productoId,
      @PathVariable Long slotId,
      @RequestParam Long sucursalId) {

    log.info("Obteniendo opciones para slot {} en sucursal {}", slotId, sucursalId);

    try {
      List<OpcionSlotDTO> opciones = compuestoService.obtenerOpcionesSlot(slotId, sucursalId);
      return ResponseEntity.ok(ApiResponse.ok("Opciones obtenidas", opciones));
    } catch (Exception e) {
      log.error("Error obteniendo opciones del slot", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  /**
   * Obtiene el flujo de configuración inicial de un producto compuesto Decide si mostrar pregunta
   * inicial o configuración default
   * <p>
   * Casos de uso: - Birriamen: Retorna pregunta inicial "¿Combo o Sencillo?" - Fuze Tea: Retorna
   * configuración default directamente
   *
   * @param productoId ID del producto compuesto
   * @param sucursalId ID de la sucursal para filtrar disponibilidad
   * @return Flujo de configuración
   */
  @GetMapping("/flujo-configuracion")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<ConfiguracionFlujoDTO> obtenerFlujoConfiguracion(
      @PathVariable Long productoId,
      @RequestParam Long sucursalId
  ) {
    log.info("GET /api/productos/{}/flujo-configuracion?sucursalId={}",
        productoId, sucursalId);

    ConfiguracionFlujoDTO flujo = compuestoService
        .obtenerFlujoConfiguracion(productoId, sucursalId);

    return ResponseEntity.ok(flujo);
  }

  /**
   * Obtiene la configuración que se activa al seleccionar una opción específica
   *
   * Caso de uso:
   * - Usuario abre modal de Birriamen
   * - Ve pregunta: "¿Cómo deseas tu Birriamen?"
   * - Elige "COMBO" (opcionId = 123)
   * - Este endpoint retorna la configuración con slots: Proteína, Bebida, Acompañamiento, etc.
   *
   * @param productoId ID del producto compuesto
   * @param opcionId ID de la opción seleccionada (COMBO, SENCILLO, etc.)
   * @param sucursalId ID de la sucursal para filtrar disponibilidad
   * @return Configuración completa con slots y opciones
   */
  @GetMapping("/configuraciones/por-opcion/{opcionId}")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<ProductoCompuestoConfiguracionDTO> obtenerConfiguracionPorOpcion(
      @PathVariable Long productoId,
      @PathVariable Long opcionId,
      @RequestParam Long sucursalId
  ) {
    log.info("GET /api/productos/{}/configuraciones/por-opcion/{}?sucursalId={}",
        productoId, opcionId, sucursalId);

    ProductoCompuestoConfiguracionDTO configuracion = compuestoService
        .obtenerConfiguracionPorOpcion(productoId, opcionId, sucursalId);

    return ResponseEntity.ok(configuracion);
  }

  /**
   * ⭐ AGREGADO @PathVariable Long productoId
   */
  @GetMapping("/slots/{slotId}/opciones-con-subconfig")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<List<OpcionSlotConSubConfigDTO>> obtenerOpcionesConSubConfig(
      @PathVariable Long productoId,  // ⭐ AGREGADO
      @PathVariable Long slotId,
      @RequestParam Long sucursalId) {

    log.info("GET /productos/{}/compuesto/slots/{}/opciones-con-subconfig?sucursalId={}",
        productoId, slotId, sucursalId);

    List<OpcionSlotConSubConfigDTO> opciones = compuestoService
        .obtenerOpcionesSlotConSubConfig(slotId, sucursalId);

    return ResponseEntity.ok(opciones);
  }

  /**
   * ⭐ AGREGADO @PathVariable Long productoId
   */
  @GetMapping("/opciones/{opcionId}/sub-configuracion")
  @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<ProductoCompuestoConfiguracionDTO> cargarSubConfiguracion(
      @PathVariable Long productoId,  // ⭐ AGREGADO
      @PathVariable Long opcionId,
      @RequestParam Long sucursalId) {

    log.info("GET /productos/{}/compuesto/opciones/{}/sub-configuracion?sucursalId={}",
        productoId, opcionId, sucursalId);

    ProductoCompuestoConfiguracionDTO config = compuestoService
        .cargarSubConfiguracionPorOpcion(opcionId, sucursalId);

    return ResponseEntity.ok(config);
  }
}