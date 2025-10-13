package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ActualizarConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CrearConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
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

    try {
      ProductoCompuestoDto compuesto = compuestoService.buscarPorProductoId(empresaId, productoId);
      return ResponseEntity.ok(ApiResponse.ok("Configuración obtenida", compuesto));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("No encontrado: " + e.getMessage()));
    }
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Actualizar configuración",
      description = "Actualiza slots y opciones del producto compuesto")
  public ResponseEntity<ApiResponse<ProductoCompuestoDto>> actualizar(
      @RequestParam Long empresaId,
      @PathVariable Long productoId,
      @Valid @RequestBody ProductoCompuestoRequest request) {

    log.info("Actualizando producto compuesto: {}", productoId);

    try {
      // Primero eliminar configuración existente
      compuestoService.eliminar(empresaId, productoId);
      // Recrear con nueva configuración
      ProductoCompuestoDto resultado = compuestoService.crear(empresaId, productoId, request);
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
   * Obtener todas las configuraciones de un producto compuesto
   */
  @GetMapping("/configuraciones")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Listar configuraciones",
      description = "Obtiene todas las configuraciones condicionales de un producto compuesto")
  public ResponseEntity<ApiResponse<List<ProductoCompuestoConfiguracionDTO>>> obtenerConfiguraciones(
      @PathVariable Long productoId) {

    log.info("Obteniendo configuraciones del producto: {}", productoId);

    try {
      List<ProductoCompuestoConfiguracionDTO> configuraciones =
          compuestoService.obtenerConfiguraciones(productoId);

      return ResponseEntity.ok(
          ApiResponse.ok(
              "Configuraciones obtenidas: " + configuraciones.size(),
              configuraciones
          )
      );
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error obteniendo configuraciones: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al obtener configuraciones: " + e.getMessage()));
    }
  }

  /**
   * Crear una nueva configuración condicional
   */
  @PostMapping("/configuraciones")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Crear configuración",
      description = "Crea una nueva configuración condicional para un producto compuesto")
  public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> crearConfiguracion(
      @PathVariable Long productoId,
      @Valid @RequestBody CrearConfiguracionRequest request) {

    log.info("Creando configuración '{}' para producto: {}", request.getNombre(), productoId);

    try {
      ProductoCompuestoConfiguracionDTO resultado =
          compuestoService.crearConfiguracion(productoId, request);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.ok("Configuración creada exitosamente", resultado));

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (BusinessException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error creando configuración: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al crear configuración: " + e.getMessage()));
    }
  }

  /**
   * Actualizar una configuración existente
   */
  @PutMapping("/configuraciones/{configId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Actualizar configuración",
      description = "Actualiza una configuración condicional existente")
  public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> actualizarConfiguracion(
      @PathVariable Long configId,
      @Valid @RequestBody ActualizarConfiguracionRequest request) {

    log.info("Actualizando configuración: {}", configId);

    try {
      ProductoCompuestoConfiguracionDTO resultado =
          compuestoService.actualizarConfiguracion(configId, request);

      return ResponseEntity.ok(
          ApiResponse.ok("Configuración actualizada exitosamente", resultado)
      );

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (BusinessException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error actualizando configuración: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al actualizar configuración: " + e.getMessage()));
    }
  }

  /**
   * Eliminar una configuración
   */
  @DeleteMapping("/configuraciones/{configId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN')")
  @Operation(summary = "Eliminar configuración",
      description = "Elimina una configuración condicional (solo ROOT y SUPER_ADMIN)")
  public ResponseEntity<ApiResponse<Void>> eliminarConfiguracion(
      @PathVariable Long configId) {

    log.info("Eliminando configuración: {}", configId);

    try {
      compuestoService.eliminarConfiguracion(configId);

      return ResponseEntity.ok(
          ApiResponse.ok("Configuración eliminada exitosamente", null)
      );

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error eliminando configuración: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al eliminar configuración: " + e.getMessage()));
    }
  }

  /**
   * Obtener configuración que se activa con una opción específica
   * ENDPOINT CLAVE PARA EL FRONTEND
   */
  @GetMapping("/configuraciones/por-opcion/{opcionId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
  @Operation(summary = "Obtener configuración por opción",
      description = "Obtiene la configuración que se activa al seleccionar una opción específica (KEY para frontend)")
  public ResponseEntity<ApiResponse<ProductoCompuestoConfiguracionDTO>> obtenerConfiguracionPorOpcion(
      @PathVariable Long productoId,
      @PathVariable Long opcionId) {

    log.info("Obteniendo configuración para producto {} con opción {}", productoId, opcionId);

    try {
      ProductoCompuestoConfiguracionDTO configuracion =
          compuestoService.obtenerConfiguracionPorOpcion(productoId, opcionId);

      return ResponseEntity.ok(
          ApiResponse.ok("Configuración encontrada", configuracion)
      );

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (BusinessException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error obteniendo configuración por opción: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al obtener configuración: " + e.getMessage()));
    }
  }
}