package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.compra.CompraDto;
import com.snnsoluciones.backnathbitpos.dto.mr.*;
import com.snnsoluciones.backnathbitpos.entity.MensajeReceptorBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.repository.MensajeReceptorBitacoraRepository;
import com.snnsoluciones.backnathbitpos.service.MensajeReceptorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mensaje-receptor")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mensaje Receptor", description = "Gestión de mensajes receptor (Aceptación/Rechazo de facturas de compra)")
public class MensajeReceptorController extends BaseController {

  private final MensajeReceptorService mensajeReceptorService;
  private final MensajeReceptorBitacoraRepository bitacoraRepository;

  @Operation(summary = "Procesar XML de factura recibida",
      description = "Analiza un XML de factura del proveedor y retorna información para revisión")
  @PostMapping("/procesar-xml")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<FacturaRecibidaDto>> procesarXml(
      @Valid @RequestBody SubirXmlCompraRequest request) {

    log.info("Procesando XML para empresa: {}, sucursal: {}",
        request.empresaId(), request.sucursalId());

    try {
      FacturaRecibidaDto resultado = mensajeReceptorService.procesarXmlSubido(request);

      return ResponseEntity.ok(
          ApiResponse.success("XML procesado exitosamente", resultado)
      );

    } catch (Exception e) {
      log.error("Error procesando XML: ", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error procesando XML: " + e.getMessage()));
    }
  }

  @Operation(summary = "Aceptar factura de proveedor",
      description = "Acepta la factura, genera mensaje receptor y crea la compra")
  @PostMapping("/aceptar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<CompraDto>> aceptarFactura(
      @Valid @RequestBody AceptarFacturaRequest request) {

    log.info("Aceptando factura con clave: {}", request.claveHacienda());

    try {
      CompraDto compra = mensajeReceptorService.aceptarFactura(request);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(
              "Factura aceptada y compra creada exitosamente. " +
                  "El mensaje receptor será enviado automáticamente.",
              compra
          ));

    } catch (Exception e) {
      log.error("Error aceptando factura: ", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error aceptando factura: " + e.getMessage()));
    }
  }

  @Operation(summary = "Rechazar factura de proveedor",
      description = "Rechaza la factura y genera mensaje receptor de rechazo")
  @PostMapping("/rechazar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> rechazarFactura(
      @Valid @RequestBody RechazarFacturaRequest request) {

    log.info("Rechazando factura con clave: {}", request.claveHacienda());

    try {
      mensajeReceptorService.rechazarFactura(request);

      return ResponseEntity.ok(
          ApiResponse.success(
              "Factura rechazada. El mensaje receptor será enviado automáticamente.",
              null
          )
      );

    } catch (Exception e) {
      log.error("Error rechazando factura: ", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error rechazando factura: " + e.getMessage()));
    }
  }

  @Operation(summary = "Consultar estado de mensaje receptor por clave")
  @GetMapping("/consultar/{clave}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<MensajeReceptorEstadoDto>> consultarEstado(
      @PathVariable String clave) {

    log.info("Consultando estado de mensaje receptor para clave: {}", clave);

    try {
      // TODO: Implementar consulta de estado
      return ResponseEntity.ok(
          ApiResponse.success("Funcionalidad pendiente de implementación", null)
      );

    } catch (Exception e) {
      log.error("Error consultando estado: ", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error consultando estado: " + e.getMessage()));
    }
  }

  @Operation(summary = "Listar bitácora de mensajes receptor")
  @GetMapping("/bitacora")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Page<MensajeReceptorBitacoraDto>>> listarBitacora(
      @RequestParam Long empresaId,
      @RequestParam(required = false) Long sucursalId,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    log.info("Listando bitácora MR - Empresa: {}, Sucursal: {}", empresaId, sucursalId);

    try {
      EstadoBitacora estadoEnum = estado != null ? EstadoBitacora.valueOf(estado) : null;

      Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

      Page<MensajeReceptorBitacora> bitacoras = bitacoraRepository.buscarConFiltros(
          empresaId,
          sucursalId,
          "ACEPTADA",
          fechaInicio,
          fechaFin,
          pageable
      );

      Page<MensajeReceptorBitacoraDto> dtos = bitacoras.map(this::convertirADto);

      return ResponseEntity.ok(ApiResponse.success("Bitácora encontrada", dtos));

    } catch (Exception e) {
      log.error("Error listando bitácora: ", e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error listando bitácora: " + e.getMessage()));
    }
  }

  // Método helper para conversión
  private MensajeReceptorBitacoraDto convertirADto(MensajeReceptorBitacora bitacora) {
    return MensajeReceptorBitacoraDto.builder()
        .id(bitacora.getId())
        .claveHacienda(bitacora.getClave())
        .consecutivoMr(bitacora.getConsecutivo())
        .tipoMensaje(bitacora.getTipoMensaje())
        .tipoMensajeDescripcion(obtenerDescripcionTipoMensaje(bitacora.getTipoMensaje()))
        .estado(bitacora.getEstado().name())
        .estadoHacienda(bitacora.getHaciendaMensaje())
        .intentos(bitacora.getIntentos())
        .createdAt(bitacora.getCreatedAt())
        .proximoIntento(bitacora.getProximoIntento())
        .ultimoError(bitacora.getUltimoError())
        .compraId(bitacora.getCompraId())
        .tieneMensajeGenerado(bitacora.getXmlFirmadoPath() != null)
        .build();
  }

  private String obtenerDescripcionTipoMensaje(String tipo) {
    return switch (tipo) {
      case "05" -> "Aceptación";
      case "06" -> "Aceptación Parcial";
      case "07" -> "Rechazo";
      default -> "Desconocido";
    };
  }
}