package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compra.*;
import com.snnsoluciones.backnathbitpos.service.CompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
@Tag(name = "Compras", description = "Gestión de compras y facturas de proveedores")
@Slf4j
public class CompraController {

  private final CompraService compraService;

  @Operation(summary = "Analizar XML de factura sin procesar")
  @PostMapping("/analizar-xml")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<AnalisisXmlResponse>> analizarXml(
      @RequestParam(required = false) Long empresaId,
      @RequestParam(required = false) Long sucursalId,
      @RequestBody String xmlContent) {

    log.info("Analizando XML de factura");
    AnalisisXmlResponse analisis = compraService.analizarXml(xmlContent, empresaId, sucursalId);

    if (analisis.isEsValido()) {
      return ResponseEntity.ok(ApiResponse.ok(
          "XML analizado correctamente", analisis
      ));
    } else {
      return ResponseEntity.ok(ApiResponse.error(
          "XML con errores de validación", analisis
      ));
    }
  }

  @Operation(summary = "Crear compra desde XML de factura")
  @PostMapping("/empresa/{empresaId}/sucursal/{sucursalId}/desde-xml")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<CompraDto>> crearDesdeXml(
      @PathVariable Long empresaId,
      @PathVariable Long sucursalId,
      @PathVariable Long terminalId,
      @RequestBody @Valid CrearCompraDesdeXmlRequest request) {

    log.info("Creando compra desde XML para empresa {} sucursal {}", empresaId, sucursalId);
    CompraDto compra = compraService.crearCompraDesdeXml(empresaId, sucursalId, request, terminalId);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created("Compra creada exitosamente", compra));
  }

  @Operation(summary = "Crear Factura Electrónica de Compra manual")
  @PostMapping("/empresa/{empresaId}/sucursal/{sucursalId}/factura-compra")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<CompraDto>> crearFacturaCompra(
      @PathVariable Long empresaId,
      @PathVariable Long sucursalId,
      @RequestBody @Valid CrearFacturaCompraRequest request) {

    log.info("Creando FEC manual para empresa {} sucursal {}", empresaId, sucursalId);
    CompraDto compra = compraService.crearFacturaCompraManual(empresaId, sucursalId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created("Factura de compra creada exitosamente", compra));
  }

  @Operation(summary = "Listar compras por empresa")
  @GetMapping("/empresa/{empresaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
  public ResponseEntity<ApiResponse<List<CompraDto>>> listarPorEmpresa(
      @PathVariable Long empresaId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

    List<CompraDto> compras = compraService.buscarPorEmpresa(empresaId);

    return ResponseEntity.ok(ApiResponse.ok(
        "Compras encontradas: " + compras.size(), compras
    ));
  }

  @Operation(summary = "Obtener compra por ID")
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
  public ResponseEntity<ApiResponse<CompraDto>> obtenerPorId(@PathVariable Long id) {

    CompraDto compra = compraService.obtenerPorId(id);
    return ResponseEntity.ok(ApiResponse.ok("Compra encontrada", compra));
  }
}