package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraActionResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraDetailResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraEstadisticasResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraFilterRequest;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraListResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaResumenDto;
import com.snnsoluciones.backnathbitpos.dto.bitacora.ReintentarProcesamientoRequest;
import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.DetalleFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.EmisorDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.ImpuestoDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.ReceptorDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.ResumenFacturaDto;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import com.snnsoluciones.backnathbitpos.service.FacturaBitacoraService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de bitácora de facturación Arquitectura limpia y práctica para MVP
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturaBitacoraServiceImpl implements FacturaBitacoraService {

  private final FacturaBitacoraRepository bitacoraRepository;
  private final FacturaRepository facturaRepository;
  private final StorageService storageService;
  private final FacturaPdfService facturaPdfService;
  private final EmailAuditLogRepository emailAuditLogRepository;
  private final EmailService emailService;

  // FacturaBitacoraServiceImpl.java

  @Override
  public Page<FacturaBitacoraListResponse> buscarSimple(
      String busqueda, String fechaDesde, String fechaHasta, String tipoDocumento,
      int page, int size, Long empresaId, Long sucursalId // 👈 NUEVOS
  ) {
    log.info(
        "Búsqueda simple bitácora - busqueda: {}, fechaDesde: {}, fechaHasta: {}, empresaId: {}, sucursalId: {}",
        busqueda, fechaDesde, fechaHasta, empresaId, sucursalId);

    boolean esBusquedaGeneral = (busqueda == null || busqueda.trim().isEmpty());
    boolean noHayFechas = (fechaDesde == null || fechaDesde.isEmpty()) &&
        (fechaHasta == null || fechaHasta.isEmpty());

    if (esBusquedaGeneral && noHayFechas) {
      LocalDateTime hace15Dias = LocalDateTime.now().minusDays(15);
      fechaDesde = hace15Dias.format(DateTimeFormatter.ISO_LOCAL_DATE);
      log.info("✅ Límite por defecto: últimos 15 días desde {}", fechaDesde);
    }

    Specification<FacturaBitacora> spec = crearEspecificacionSimple(
        busqueda, fechaDesde, fechaHasta, tipoDocumento, empresaId, sucursalId // 👈 pasa ids
    );

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return bitacoraRepository.findAll(spec, pageable).map(this::convertirAListResponse);
  }

  private Specification<FacturaBitacora> crearEspecificacionSimple(
      String busqueda, String fechaDesde, String fechaHasta, String tipoDocumento,
      Long empresaId, Long sucursalId
  ) {
    return (root, query, cb) -> {
      query.distinct(true);
      List<Predicate> ps = new ArrayList<>();

      // Fechas (sobre bitácora)
      if (fechaDesde != null && !fechaDesde.isEmpty()) {
        try {
          ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
              LocalDate.parse(fechaDesde).atStartOfDay()));
        } catch (Exception ignored) {
        }
      }
      if (fechaHasta != null && !fechaHasta.isEmpty()) {
        try {
          ps.add(cb.lessThanOrEqualTo(root.get("createdAt"),
              LocalDate.parse(fechaHasta).atTime(23, 59, 59)));
        } catch (Exception ignored) {
        }
      }

      // Root secundario: Factura
      var facturaRoot = query.from(Factura.class);
      ps.add(cb.equal(facturaRoot.get("id"), root.get("facturaId"))); // b.facturaId = f.id

      // Filtros por tipo/empresa/sucursal (sobre factura)
      if (tipoDocumento != null && !tipoDocumento.isEmpty()) {
        ps.add(cb.equal(facturaRoot.get("tipoDocumento"), tipoDocumento));
      }
      if (empresaId != null || sucursalId != null) {
        var sucursalJoin = facturaRoot.join("sucursal", JoinType.LEFT);
        if (empresaId != null) {
          ps.add(cb.equal(sucursalJoin.get("empresa").get("id"), empresaId));
        }
        if (sucursalId != null) {
          ps.add(cb.equal(sucursalJoin.get("id"), sucursalId));
        }
      }

      // Búsqueda libre
      if (busqueda != null && !busqueda.trim().isEmpty()) {
        String like = "%" + busqueda.toLowerCase() + "%";
        List<Predicate> ors = new ArrayList<>();
        // clave en bitácora
        ors.add(cb.like(cb.lower(root.get("clave")), like));
        // consecutivo y receptor “sueltos” en factura
        ors.add(cb.like(cb.lower(facturaRoot.get("consecutivo")), like));
        ors.add(cb.like(cb.lower(facturaRoot.get("nombreReceptor")), like));
        ors.add(cb.like(cb.lower(facturaRoot.get("emailReceptor")), like));

        // Cliente (root adicional)
        var clienteJoin = facturaRoot.join("cliente", JoinType.LEFT);
        ors.add(cb.like(cb.lower(clienteJoin.get("razonSocial")), like));
        ors.add(cb.like(cb.lower(clienteJoin.get("numeroIdentificacion")), like));

        ps.add(cb.or(ors.toArray(new Predicate[0])));
      }

      return cb.and(ps.toArray(new Predicate[0]));
    };
  }

  /**
   * Mapea tipo de documento a código en clave
   */
  private String mapearTipoDocumentoACodigo(String tipoDocumento) {
    return switch (tipoDocumento.toUpperCase()) {
      case "FE" -> "01";  // Factura Electrónica
      case "ND" -> "02";  // Nota de Débito
      case "NC" -> "03";  // Nota de Crédito
      case "TE" -> "04";  // Tiquete Electrónico
      case "FEE" -> "05"; // Factura Electrónica de Exportación
      case "FEC" -> "06"; // Factura Electrónica de Compra
      default -> null;
    };
  }

  @Override
  public Page<FacturaBitacoraListResponse> buscarConFiltros(FacturaBitacoraFilterRequest filtros) {
    log.info("Buscando bitácoras con filtros: {}", filtros);

    // Crear especificación para búsqueda dinámica
    Specification<FacturaBitacora> spec = crearEspecificacion(filtros);

    // Configurar paginación
    Pageable pageable = PageRequest.of(
        filtros.getPage(),
        filtros.getSize(),
        Sort.by(Sort.Direction.fromString(filtros.getSortDirection()), filtros.getSortBy())
    );

    // Ejecutar búsqueda
    Page<FacturaBitacora> bitacoras = bitacoraRepository.findAll(spec, pageable);

    // Convertir a DTOs
    return bitacoras.map(this::convertirAListResponse);
  }

  @Override
  public FacturaBitacoraDetailResponse obtenerDetalle(Long id) {
    log.info("Obteniendo detalle de bitácora ID: {}", id);

    FacturaBitacora bitacora = bitacoraRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Bitácora no encontrada: " + id));

    return convertirADetailResponse(bitacora);
  }

  @Override
  public FacturaBitacoraDetailResponse buscarPorClave(String clave) {
    log.info("Buscando bitácora por clave: {}", clave);

    FacturaBitacora bitacora = bitacoraRepository.findByClave(clave)
        .orElseThrow(
            () -> new ResourceNotFoundException("Bitácora no encontrada con clave: " + clave));

    return convertirADetailResponse(bitacora);
  }

  @Override
  public FacturaBitacoraDetailResponse buscarPorFacturaId(Long facturaId) {
    log.info("Buscando bitácora para factura ID: {}", facturaId);

    FacturaBitacora bitacora = bitacoraRepository.findByFacturaId(facturaId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Bitácora no encontrada para factura: " + facturaId));

    return convertirADetailResponse(bitacora);
  }

  @Override
  @Transactional
  public FacturaBitacoraActionResponse reintentarProcesamiento(
      ReintentarProcesamientoRequest request) {
    log.info("Reintentando procesamiento de bitácora ID: {}", request.getBitacoraId());

    FacturaBitacora bitacora = bitacoraRepository.findById(request.getBitacoraId())
        .orElseThrow(() -> new ResourceNotFoundException("Bitácora no encontrada"));

    // Validar que se puede reintentar
    if (!puedeReintentar(bitacora)) {
      return FacturaBitacoraActionResponse.builder()
          .exitoso(false)
          .mensaje("No se puede reintentar. Estado actual: " + bitacora.getEstado())
          .bitacoraId(bitacora.getId())
          .build();
    }

    // Actualizar para reintento
    if (request.getReiniciarContador()) {
      bitacora.setIntentos(0);
    }

    bitacora.setEstado(EstadoBitacora.PENDIENTE);
    bitacora.setProximoIntento(LocalDateTime.now());
    bitacora.setUltimoError(null);

    if (request.getMotivo() != null) {
      bitacora.setHaciendaMensaje("Reintento manual: " + request.getMotivo());
    }

    bitacoraRepository.save(bitacora);

    return FacturaBitacoraActionResponse.builder()
        .exitoso(true)
        .mensaje("Reintento programado exitosamente")
        .bitacoraId(bitacora.getId())
        .nuevoEstado(bitacora.getEstado().name())
        .proximoIntento(bitacora.getProximoIntento())
        .build();
  }

  @Override
  @Transactional
  public FacturaBitacoraActionResponse cancelarProcesamiento(Long bitacoraId, String motivo) {
    log.info("Cancelando procesamiento de bitácora ID: {}", bitacoraId);

    FacturaBitacora bitacora = bitacoraRepository.findById(bitacoraId)
        .orElseThrow(() -> new ResourceNotFoundException("Bitácora no encontrada"));

    // Solo se puede cancelar si está pendiente o en error
    if (bitacora.getEstado() != EstadoBitacora.PENDIENTE &&
        bitacora.getEstado() != EstadoBitacora.ERROR) {
      return FacturaBitacoraActionResponse.builder()
          .exitoso(false)
          .mensaje("Solo se pueden cancelar bitácoras pendientes o con error")
          .bitacoraId(bitacora.getId())
          .build();
    }

    // Marcar como cancelada
    bitacora.setEstado(EstadoBitacora.CANCELADO);
    bitacora.setHaciendaMensaje("Cancelado manualmente" + (motivo != null ? ": " + motivo : ""));
    bitacora.setProximoIntento(null);

    bitacoraRepository.save(bitacora);

    // Actualizar estado de la factura
    Factura factura = facturaRepository.findById(bitacora.getFacturaId()).orElse(null);
    if (factura != null) {
      factura.setEstado(EstadoFactura.CANCELADA);
      facturaRepository.save(factura);
    }

    return FacturaBitacoraActionResponse.builder()
        .exitoso(true)
        .mensaje("Procesamiento cancelado exitosamente")
        .bitacoraId(bitacora.getId())
        .nuevoEstado(bitacora.getEstado().name())
        .build();
  }

  @Override
  public FacturaBitacoraEstadisticasResponse obtenerEstadisticas(Long empresaId, Long sucursalId) {
    log.info("Calculando estadísticas - Empresa: {}, Sucursal: {}", empresaId, sucursalId);

    // Crear query base con filtros opcionales
    Specification<FacturaBitacora> spec = (root, query, cb) -> cb.conjunction();

    if (empresaId != null || sucursalId != null) {
      spec = spec.and((root, query, cb) -> {
        Join<FacturaBitacora, Factura> facturaJoin = root.join("factura", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();

        if (empresaId != null) {
          Join<Factura, Sucursal> sucursalJoin = facturaJoin.join("sucursal", JoinType.INNER);
          predicates.add(cb.equal(sucursalJoin.get("empresa").get("id"), empresaId));
        }

        if (sucursalId != null) {
          predicates.add(cb.equal(facturaJoin.get("sucursal").get("id"), sucursalId));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
      });
    }

    List<FacturaBitacora> todas = bitacoraRepository.findAll(spec);

    // Calcular estadísticas
    return calcularEstadisticas(todas);
  }

  @Override
  public ResponseEntity<?> descargarArchivo(Long bitacoraId, String tipoArchivo) {
    log.info("Descargando archivo {} de bitácora ID: {}", tipoArchivo, bitacoraId);

    FacturaBitacora bitacora = bitacoraRepository.findById(bitacoraId)
        .orElseThrow(() -> new ResourceNotFoundException("Bitácora no encontrada"));

    Factura factura = facturaRepository.findById(bitacora.getFacturaId())
        .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

    try {
      byte[] contenido;
      String nombreArchivo;
      MediaType mediaType;

      switch (tipoArchivo.toLowerCase()) {
        case "carta":
          contenido = facturaPdfService.generarFacturaCarta(factura.getClave());
          nombreArchivo = bitacora.getClave() + "_factura.pdf";
          mediaType = MediaType.APPLICATION_PDF;
          break;

        case "ticket":
          contenido = facturaPdfService.generarFacturaTicket(factura.getClave());
          nombreArchivo = bitacora.getClave() + "_ticket.pdf";
          mediaType = MediaType.APPLICATION_PDF;
          break;

        case "xml":
          contenido = storageService.obtenerArchivo(bitacora.getXmlPath());
          nombreArchivo = bitacora.getClave() + "_factura.xml";
          mediaType = MediaType.APPLICATION_XML;
          break;

        case "xml_firmado":
          contenido = storageService.obtenerArchivo(bitacora.getXmlFirmadoPath());
          nombreArchivo = bitacora.getClave() + "_factura_firmada.xml";
          mediaType = MediaType.APPLICATION_XML;
          break;

        case "xml_respuesta":
          contenido = storageService.obtenerArchivo(bitacora.getXmlRespuestaPath());
          nombreArchivo = bitacora.getClave() + "_respuesta.xml";
          mediaType = MediaType.APPLICATION_XML;
          break;

        default:
          throw new IllegalArgumentException("Tipo no válido: " + tipoArchivo);
      }

      return ResponseEntity.ok()
          .contentType(mediaType)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + nombreArchivo + "\"")
          .body(contenido);

    } catch (Exception e) {
      log.error("Error al descargar archivo", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error al descargar archivo: " + e.getMessage());
    }
  }

  // Método auxiliar para generar PDF on-demand
  private ResponseEntity<?> generarPdfOnDemand(FacturaBitacora bitacora) {
    try {
      // Obtener la factura
      Factura factura = facturaRepository.findById(bitacora.getFacturaId())
          .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

      // Generar PDF usando el servicio existente
      byte[] pdfBytes = facturaPdfService.generarFacturaTicket(factura.getClave());

      String nombreArchivo = bitacora.getClave() + ".pdf";

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + nombreArchivo + "\"")
          .body(pdfBytes);
    } catch (Exception e) {
      log.error("Error generando PDF", e);
      throw new RuntimeException("Error al generar PDF: " + e.getMessage());
    }
  }

  @Transactional
  public String reenviarCorreo(Long bitacoraId, String emailOverride) {
    log.info("Reenviando correo para bitácora ID: {}", bitacoraId);

    FacturaBitacora bitacora = bitacoraRepository.findById(bitacoraId)
        .orElseThrow(() -> new EntityNotFoundException("Bitácora no encontrada"));

    // Validar que esté aceptada
    if (bitacora.getEstado() != EstadoBitacora.ACEPTADA) {
      throw new IllegalStateException("Solo se pueden reenviar facturas aceptadas");
    }

    Factura factura = facturaRepository.findById(bitacora.getFacturaId())
        .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada"));

    try {
      // 1. Determinar email destino
      String emailDestino = emailOverride != null && !emailOverride.trim().isEmpty()
          ? emailOverride.trim()
          : factura.getEmailReceptor();

      if (emailDestino == null || emailDestino.isEmpty()) {
        throw new IllegalArgumentException("No hay email de destino disponible");
      }

      // 3. Descargar archivos de S3
      byte[] pdfBytes = facturaPdfService.generarFactura(bitacora.getClave(), "carta");

      byte[] xmlFirmadoBytes = null;
      if (bitacora.getXmlFirmadoPath() != null) {
        xmlFirmadoBytes = storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath());
      }

      byte[] respuestaHaciendaBytes = null;
      if (bitacora.getXmlRespuestaPath() != null) {
        respuestaHaciendaBytes = storageService.downloadFileAsBytes(bitacora.getXmlRespuestaPath());
      }

      // 4. Armar el DTO para el EmailService (igual que en el Job)
      EmailFacturaDto dto = EmailFacturaDto.builder()
          // Identificadores
          .facturaId(factura.getId())
          .clave(factura.getClave())
          .consecutivo(factura.getConsecutivo())

          // Destinatario
          .emailDestino(emailDestino)

          // Datos para asunto/cuerpo
          .tipoDocumento(factura.getTipoDocumento().getDescripcion())
          .nombreComercial(factura.getSucursal().getEmpresa().getNombreComercial())
          .razonSocial(factura.getSucursal().getEmpresa().getNombreRazonSocial())
          .cedulaJuridica(factura.getSucursal().getEmpresa().getIdentificacion())
          .fechaEmision(formatearFecha(factura.getFechaEmision()))

          // Cliente
          .nombreComercial(factura.getCliente() != null
              ? factura.getCliente().getRazonSocial()
              : factura.getNombreReceptor())
          .cedulaJuridica(factura.getCliente() != null
              ? factura.getCliente().getNumeroIdentificacion()
              : null)

          // Montos

          // Archivos adjuntos
          .pdfBytes(pdfBytes)
          .xmlFirmadoBytes(xmlFirmadoBytes)
          .respuestaHaciendaBytes(respuestaHaciendaBytes)

          .build();

      // 5. Enviar email
      emailService.enviarFacturaElectronica(dto);

      // 6. Registrar en log de emails (si tienes tabla de auditoría)
//            EmailAuditLog emailLog = EmailAuditLog.builder()
//                .facturaId(factura.getId())
//                .emailDestino(emailDestino)
//                .estado(EstadoEmail.ENVIADO)
//                .fechaEnvio(LocalDateTime.now())
//                .build();
//
//            emailAuditLogRepository.save(emailLog);

      log.info("Correo reenviado exitosamente a: {} para factura: {}",
          emailDestino, factura.getConsecutivo());

      return "Correo reenviado exitosamente a: " + emailDestino;

    } catch (Exception e) {
      log.error("Error al reenviar correo para factura: {}", factura.getConsecutivo(), e);

      // Registrar el fallo si tienes auditoría
//            EmailAuditLog emailLog = EmailAuditLog.builder()
//                .facturaId(factura.getId())
//                .emailDestino(emailOverride != null ? emailOverride : factura.getEmailReceptor())
//                .estado(EstadoEmail.ERROR)
//                .errorMensaje(e.getMessage())
//                .fechaEnvio(LocalDateTime.now())
//                .build();
//
//            emailAuditLogRepository.save(emailLog);

      throw new RuntimeException("Error al reenviar correo: " + e.getMessage(), e);
    }
  }

  // Método auxiliar para formatear fecha
  private String formatearFecha(String fechaStr) {
    try {
      if (fechaStr == null || fechaStr.isEmpty()) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
      }

      // Parsear y formatear según tu formato
      ZonedDateTime zdt = ZonedDateTime.parse(fechaStr);
      return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    } catch (DateTimeParseException e) {
      log.warn("No se pudo parsear la fecha: {}. Usando fecha actual.", fechaStr);
      return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
  }

  // ========== MÉTODOS AUXILIARES ==========

  /**
   * Crea especificación para búsqueda dinámica
   */
  private Specification<FacturaBitacora> crearEspecificacion(FacturaBitacoraFilterRequest filtros) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro por clave, consecutivo o nombre del cliente
      if (filtros.getClave() != null && !filtros.getClave().isEmpty()) {
        String searchTerm = "%" + filtros.getClave().toLowerCase() + "%";

        // Crear predicados para buscar en múltiples campos
        List<Predicate> searchPredicates = new ArrayList<>();

        // Buscar por clave
        searchPredicates.add(cb.like(cb.lower(root.get("clave")), searchTerm));

        // Buscar por consecutivo y nombre del cliente (requiere JOIN)
        Join<FacturaBitacora, Factura> facturaJoin = root.join("factura", JoinType.LEFT);
        searchPredicates.add(cb.like(cb.lower(facturaJoin.get("consecutivo")), searchTerm));

        // Buscar por nombre del cliente
        Join<Factura, Cliente> clienteJoin = facturaJoin.join("cliente", JoinType.LEFT);
        searchPredicates.add(cb.like(cb.lower(clienteJoin.get("nombreRazonSocial")), searchTerm));

        // Combinar con OR
        predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
      }

      // Mantener el resto de los filtros igual...
      // Filtro por estados
      if (filtros.getEstados() != null && !filtros.getEstados().isEmpty()) {
        predicates.add(root.get("estado").in(filtros.getEstados()));
      }

      // Filtro por fechas
      if (filtros.getFechaDesde() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filtros.getFechaDesde()));
      }
      if (filtros.getFechaHasta() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filtros.getFechaHasta()));
      }

      // Filtro por errores
      if (Boolean.TRUE.equals(filtros.getSoloConError())) {
        predicates.add(cb.equal(root.get("estado"), EstadoBitacora.ERROR));
      }

      // Filtro por reintentables
      if (Boolean.TRUE.equals(filtros.getSoloReintentables())) {
        predicates.add(cb.and(
            root.get("estado").in(EstadoBitacora.ERROR, EstadoBitacora.PENDIENTE),
            cb.lessThan(root.get("intentos"), 3)
        ));
      }

      // Filtro por intentos
      if (filtros.getIntentosMinimos() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("intentos"), filtros.getIntentosMinimos()));
      }
      if (filtros.getIntentosMaximos() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("intentos"), filtros.getIntentosMaximos()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Convierte entidad a DTO de lista
   */
  private FacturaBitacoraListResponse convertirAListResponse(FacturaBitacora bitacora) {
    // Obtener factura para datos adicionales
    Factura factura = facturaRepository.findById(bitacora.getFacturaId()).orElse(null);

    FacturaBitacoraListResponse.FacturaBitacoraListResponseBuilder builder = FacturaBitacoraListResponse.builder()
        .id(bitacora.getId())
        .facturaId(bitacora.getFacturaId())
        .clave(bitacora.getClave())
        .estado(bitacora.getEstado())
        .intentos(bitacora.getIntentos())
        .proximoIntento(bitacora.getProximoIntento())
        .createdAt(bitacora.getCreatedAt())
        .updatedAt(bitacora.getUpdatedAt())
        .puedeReintentar(puedeReintentar(bitacora))
        .tieneError(bitacora.getEstado() == EstadoBitacora.ERROR)
        .mensajeError(bitacora.getUltimoError());

    if (factura != null) {
      builder.consecutivoFactura(factura.getConsecutivo())
          .montoTotal(factura.getTotalComprobante())
          .empresaNombre(factura.getSucursal().getEmpresa().getNombreComercial())
          .sucursalNombre(factura.getSucursal().getNombre());

      if (factura.getCliente() != null) {
        Cliente cliente = factura.getCliente();
        builder.nombreCliente(cliente.getRazonSocial())
            .numeroIdentificacionCliente(cliente.getNumeroIdentificacion());

        // Obtener el correo usado para esta factura
        if (factura.getEmailReceptor() != null) {
          builder.correoCliente(factura.getEmailReceptor());
        }

        // Obtener código de actividad (asumiendo que hay una actividad principal)
        if (factura.getActividadReceptor() != null && !factura.getActividadReceptor().isEmpty()) {
          builder.codigoActividadCliente(
              factura.getActividadReceptor()
          );
        }
      }

      if (factura.getMediosPago() != null && !factura.getMediosPago().isEmpty()) {
        // Concatenar todos los medios de pago
        String mediosPago = factura.getMediosPago().stream()
            .map(mp -> mp.getMedioPago().getDescripcion())
            .collect(Collectors.joining(" || "));

        String referencias = factura.getMediosPago().stream()
            .map(mp -> mp.getReferencia() != null && !mp.getReferencia().trim().isEmpty()
                ? mp.getReferencia()
                : "N/A")
            .collect(Collectors.joining(" || "));

        builder.medioPagoPrincipal(mediosPago);

        builder.referenciasPago(referencias);

        // Si queremos también los montos por medio de pago (opcional)
        String montosRecibidos = factura.getMediosPago().stream()
            .map(mp -> formatearMontoBigDecimal(mp.getMonto()))
            .collect(Collectors.joining(" - "));

        // Guardar en un campo nuevo o reusar montoRecibido
        builder.montosDetalle(montosRecibidos); // Necesitarías agregar este campo
      }
    }

    emailAuditLogRepository.findFirstByFacturaIdAndEstadoOrderByCreatedAtDesc(
        bitacora.getFacturaId(),
        EstadoEmail.ENVIADO
    ).ifPresent(emailLog -> builder.fechaEnvioEmail(emailLog.getFechaEnvio()));

    return builder.build();
  }

  private String formatearMontoBigDecimal(BigDecimal monto) {
    if (monto == null) {
      return "₡0";
    }
    DecimalFormat df = new DecimalFormat("₡#,###.##");
    return df.format(monto);
  }

  /**
   * Convierte entidad a DTO detallado CON TODA LA INFO NECESARIA PARA EL TICKET
   */
  private FacturaBitacoraDetailResponse convertirADetailResponse(FacturaBitacora bitacora) {
    // Obtener factura completa
    Factura factura = facturaRepository.findById(bitacora.getFacturaId())
        .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

    // Construir resumen de factura
    FacturaResumenDto facturaResumen = FacturaResumenDto.builder()
        .id(factura.getId())
        .consecutivo(factura.getConsecutivo())
        .tipoDocumento(factura.getTipoDocumento())
        .fechaEmision(factura.getFechaEmision())
        .montoTotal(factura.getTotalComprobante())
        .moneda(factura.getMoneda().name())
        .empresaNombre(factura.getSucursal().getEmpresa().getNombreComercial())
        .sucursalNombre(factura.getSucursal().getNombre())
        .build();

    if (factura.getCliente() != null) {
      facturaResumen.setClienteNombre(factura.getCliente().getRazonSocial());
      facturaResumen.setClienteIdentificacion(factura.getCliente().getNumeroIdentificacion());
    }

    // 🔥 CONSTRUIR EMISOR
    FacturaResponse.EmisorDto emisor = FacturaResponse.EmisorDto.builder()
        .nombre(factura.getSucursal().getEmpresa().getNombreRazonSocial())
        .numeroIdentificacion(factura.getSucursal().getEmpresa().getIdentificacion())
        .tipoIdentificacion(factura.getSucursal().getEmpresa().getTipoIdentificacion())
        .correoElectronico(
            factura.getSucursal().getEmail() != null && !factura.getSucursal().getEmail().isEmpty()
                ?
                factura.getSucursal().getEmail() :
                factura.getSucursal().getEmpresa().getEmail())
        .build();

    String telefonoEmisor;
    // Teléfono del emisor
    if (factura.getSucursal().getTelefono() != null && !factura.getSucursal().getTelefono()
        .isEmpty()) {
      telefonoEmisor = factura.getSucursal().getTelefono();

      emisor.setTelefono(FacturaResponse.TelefonoDto.builder()
          .codigoPais("506")
          .numTelefono(telefonoEmisor)
          .build());
    } else {
      telefonoEmisor = factura.getSucursal().getEmpresa().getTelefono();
    }

    emisor.setTelefono(FacturaResponse.TelefonoDto.builder()
        .codigoPais("506")
        .numTelefono(telefonoEmisor)
        .build());

    // Dirección del emisor
    String direccionEmisor = hacerDirreccion(emisor.getUbicacion().getProvincia(),
        emisor.getUbicacion().getCanton(),
        emisor.getUbicacion().getDistrito(), emisor.getUbicacion().getDistrito(),
        emisor.getUbicacion().getOtrasSenas());

    // 🔥 CONSTRUIR RECEPTOR
    FacturaResponse.ReceptorDto receptor = null;
    if (factura.getCliente() != null || factura.getNombreReceptor() != null) {
      FacturaResponse.ReceptorDto.ReceptorDtoBuilder receptorBuilder = FacturaResponse.ReceptorDto.builder()
          .nombre(factura.getCliente() != null ?
              factura.getCliente().getRazonSocial() :
              factura.getNombreReceptor())
          .numeroIdentificacion(factura.getCliente() != null ?
              factura.getCliente().getNumeroIdentificacion() : null)
          .correoElectronico(factura.getEmailReceptor())
          .codigoActividadReceptor(factura.getActividadReceptor());

      if (factura.getCliente() != null) {
        receptorBuilder
            .clienteId(factura.getCliente().getId())
            .tipoIdentificacion(factura.getCliente().getTipoIdentificacion());

        // Teléfono del cliente
        if (factura.getCliente().getTelefonoNumero() != null) {
          receptorBuilder.telefono(FacturaResponse.TelefonoDto.builder()
              .codigoPais(factura.getCliente().getTelefonoCodigoPais() != null ?
                  factura.getCliente().getTelefonoCodigoPais() : "506")
              .numTelefono(factura.getCliente().getTelefonoNumero())
              .build());
        }

        // Dirección del cliente
        if (factura.getCliente().getUbicacion() != null &&
            factura.getCliente().getUbicacion().getOtrasSenas() != null) {
          receptorBuilder.ubicacion(FacturaResponse.UbicacionDto.builder()
              .otrasSenas(factura.getCliente().getUbicacion().getOtrasSenas())
              .build());
        }
      }

      receptor = receptorBuilder.build();
    }

    // 🔥 CONSTRUIR LÍNEAS/DETALLES
    List<FacturaResponse.DetalleFacturaDto> lineas = new ArrayList<>();
    if (factura.getDetalles() != null) {
      lineas = factura.getDetalles().stream()
          .map(detalle -> {
            // Construir lista de impuestos
            List<FacturaResponse.ImpuestoDto> impuestos = new ArrayList<>();
            if (detalle.getImpuestos() != null) {
              impuestos = detalle.getImpuestos().stream()
                  .map(imp -> FacturaResponse.ImpuestoDto.builder()
                      .codigo(imp.getProductoImpuesto() != null ?
                          imp.getProductoImpuesto().getTipoImpuesto() : null)
                      .codigoTarifaIVA(imp.getProductoImpuesto() != null ?
                          imp.getProductoImpuesto().getCodigoTarifaIVA() : null)
                      .tarifa(imp.getTarifa())
                      .monto(imp.getMontoImpuesto())
                      .build())
                  .collect(Collectors.toList());
            }

            return FacturaResponse.DetalleFacturaDto.builder()
                .numeroLinea(detalle.getNumeroLinea())
                .detalle(detalle.getDetalle())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .montoTotal(detalle.getMontoTotal())
                .montoTotalLinea(detalle.getMontoTotalLinea())
                .subTotal(detalle.getSubtotal())
                .impuestos(impuestos)
                .build();
          })
          .collect(Collectors.toList());
    }

    // 🔥 CONSTRUIR RESUMEN TOTALES
    FacturaResponse.ResumenFacturaDto resumenFactura = FacturaResponse.ResumenFacturaDto.builder()
        .totalVenta(factura.getTotalVenta())
        .totalDescuentos(factura.getTotalDescuentos())
        .totalVentaNeta(factura.getTotalVentaNeta())
        .totalImpuesto(factura.getTotalImpuesto())
        .totalExonerado(factura.getTotalExonerado() != null ?
            factura.getTotalExonerado() : BigDecimal.ZERO)
        .totalOtrosCargos(factura.getTotalOtrosCargos() != null ?
            factura.getTotalOtrosCargos() : BigDecimal.ZERO)
        .totalComprobante(factura.getTotalComprobante())
        .build();

    // 🔥 MEDIOS DE PAGO
    BigDecimal efectivo = BigDecimal.ZERO;
    BigDecimal tarjeta = BigDecimal.ZERO;
    BigDecimal cheque = BigDecimal.ZERO;
    BigDecimal transferencia = BigDecimal.ZERO;
    BigDecimal vuelto = factura.getVuelto() != null ? factura.getVuelto() : BigDecimal.ZERO;

    if (factura.getMediosPago() != null) {
      for (FacturaMedioPago mp : factura.getMediosPago()) {
        switch (mp.getMedioPago()) {
          case EFECTIVO -> efectivo = efectivo.add(mp.getMonto());
          case TARJETA -> tarjeta = tarjeta.add(mp.getMonto());
          case CHEQUE -> cheque = cheque.add(mp.getMonto());
          case TRANSFERENCIA, SINPE_MOVIL -> transferencia = transferencia.add(mp.getMonto());
        }
      }
    }

    // Construir respuesta completa
    return FacturaBitacoraDetailResponse.builder()
        .id(bitacora.getId())
        .facturaId(bitacora.getFacturaId())
        .clave(bitacora.getClave())
        .estado(bitacora.getEstado())
        .intentos(bitacora.getIntentos())
        .proximoIntento(bitacora.getProximoIntento())
        .createdAt(bitacora.getCreatedAt())
        .updatedAt(bitacora.getUpdatedAt())
        .rutaXml(bitacora.getXmlPath())
        .rutaXmlFirmado(bitacora.getXmlFirmadoPath())
        .rutaXmlRespuesta(bitacora.getXmlRespuestaPath())
        .ultimoError(bitacora.getUltimoError())
        .haciendaMensaje(bitacora.getHaciendaMensaje())
        .factura(facturaResumen)

        // 🔥 DATOS PARA EL TICKET
        .emisor(emisor)
        .receptor(receptor)
        .lineas(lineas)
        .resumenFactura(resumenFactura)
        .condicionVenta(factura.getCondicionVenta().name())
        .plazoCreditoDias(factura.getPlazoCredito())
        .numeroInterno(factura.getId().toString())
        .vendedor(factura.getCajero() != null ?
            factura.getCajero().getNombre() + " " + factura.getCajero().getApellidos() : null)
        .observaciones(factura.getObservaciones())

        // Medios de pago
        .efectivo(efectivo)
        .tarjeta(tarjeta)
        .cheque(cheque)
        .transferencia(transferencia)
        .vuelto(vuelto)

        // URLs para descargar archivos
        .urlDescargaXml(bitacora.getXmlPath() != null ?
            "/api/factura-bitacora/" + bitacora.getId() + "/archivo/xml" : null)
        .urlDescargaXmlFirmado(bitacora.getXmlFirmadoPath() != null ?
            "/api/factura-bitacora/" + bitacora.getId() + "/archivo/xml_firmado" : null)
        .urlDescargaXmlRespuesta(bitacora.getXmlRespuestaPath() != null ?
            "/api/factura-bitacora/" + bitacora.getId() + "/archivo/xml_respuesta" : null)
        .build();
  }

  /**
   * Calcula estadísticas de las bitácoras
   */
  private FacturaBitacoraEstadisticasResponse calcularEstadisticas(
      List<FacturaBitacora> bitacoras) {
    // Contadores por estado
    Map<EstadoBitacora, Long> porEstado = bitacoras.stream()
        .collect(Collectors.groupingBy(FacturaBitacora::getEstado, Collectors.counting()));

    long totalPendientes = porEstado.getOrDefault(EstadoBitacora.PENDIENTE, 0L);
    long totalProcesando = porEstado.getOrDefault(EstadoBitacora.PROCESANDO, 0L);
    long totalCompletadas = porEstado.getOrDefault(EstadoBitacora.ACEPTADA, 0L);
    long totalConError = porEstado.getOrDefault(EstadoBitacora.ERROR, 0L);
    long totalRechazadas = porEstado.getOrDefault(EstadoBitacora.RECHAZADA, 0L);

    // Calcular porcentaje de éxito
    long totalProcesadas = totalCompletadas + totalRechazadas;
    double porcentajeExito = totalProcesadas > 0 ?
        (totalCompletadas * 100.0 / totalProcesadas) : 0.0;

    // Tiempo promedio de procesamiento (solo las completadas)
    double tiempoPromedioMinutos = bitacoras.stream()
        .filter(b -> b.getEstado() == EstadoBitacora.ACEPTADA)
        .mapToLong(b -> ChronoUnit.MINUTES.between(b.getCreatedAt(), b.getUpdatedAt()))
        .average()
        .orElse(0.0);

    // Promedio de intentos
    double promedioIntentos = bitacoras.stream()
        .mapToInt(FacturaBitacora::getIntentos)
        .average()
        .orElse(0.0);

    // Facturas por periodo
    LocalDateTime ahora = LocalDateTime.now();
    Map<String, Long> porPeriodo = new HashMap<>();
    porPeriodo.put("ultimas24h", bitacoras.stream()
        .filter(b -> b.getCreatedAt().isAfter(ahora.minusHours(24)))
        .count());
    porPeriodo.put("ultimos7dias", bitacoras.stream()
        .filter(b -> b.getCreatedAt().isAfter(ahora.minusDays(7)))
        .count());
    porPeriodo.put("ultimos30dias", bitacoras.stream()
        .filter(b -> b.getCreatedAt().isAfter(ahora.minusDays(30)))
        .count());

    // Errores más frecuentes
    Map<String, Long> erroresFrecuentes = bitacoras.stream()
        .filter(b -> b.getUltimoError() != null)
        .collect(Collectors.groupingBy(
            b -> extraerTipoError(b.getUltimoError()),
            Collectors.counting()
        ));

    return FacturaBitacoraEstadisticasResponse.builder()
        .totalPendientes(totalPendientes)
        .totalProcesando(totalProcesando)
        .totalCompletadas(totalCompletadas)
        .totalConError(totalConError)
        .totalRechazadas(totalRechazadas)
        .porcentajeExito(
            BigDecimal.valueOf(porcentajeExito).setScale(2, RoundingMode.HALF_UP).doubleValue())
        .tiempoPromedioProcesamientoMinutos(
            BigDecimal.valueOf(tiempoPromedioMinutos).setScale(2, RoundingMode.HALF_UP)
                .doubleValue())
        .promedioIntentos((int) Math.round(promedioIntentos))
        .facturasPorPeriodo(porPeriodo)
        .erroresFrecuentes(erroresFrecuentes)
        .build();
  }

  /**
   * Determina si una bitácora puede ser reintentada
   */
  private boolean puedeReintentar(FacturaBitacora bitacora) {
    return (bitacora.getEstado() == EstadoBitacora.ERROR ||
        bitacora.getEstado() == EstadoBitacora.PENDIENTE) &&
        bitacora.getIntentos() < 3;
  }

  /**
   * Extrae el tipo de error del mensaje completo
   */
  private String extraerTipoError(String mensajeError) {
    if (mensajeError == null) {
      return "Error desconocido";
    }

    // Buscar patrones comunes
    if (mensajeError.contains("timeout") || mensajeError.contains("Timeout")) {
      return "Timeout de conexión";
    } else if (mensajeError.contains("401") || mensajeError.contains("authentication")) {
      return "Error de autenticación";
    } else if (mensajeError.contains("400") || mensajeError.contains("Bad Request")) {
      return "Datos inválidos";
    } else if (mensajeError.contains("500") || mensajeError.contains("Internal Server")) {
      return "Error del servidor";
    } else if (mensajeError.contains("connection") || mensajeError.contains("Connection")) {
      return "Error de conexión";
    } else if (mensajeError.contains("firma") || mensajeError.contains("sign")) {
      return "Error de firma digital";
    } else {
      // Tomar las primeras palabras del error
      String[] palabras = mensajeError.split(" ");
      return palabras.length > 3 ?
          String.join(" ", Arrays.copyOfRange(palabras, 0, 3)) + "..." :
          mensajeError;
    }
  }

  private String hacerDirreccion(String provincia, String canton, String distrito,
      String barrio, String direccion) {
    String dir = "";
    if (provincia != null) {
      dir.concat(provincia);
    }
    if (canton != null) {
      dir.concat(", ").concat(canton);
    }
    if (distrito != null) {
      dir.concat(", ").concat(distrito);
    }
    if (barrio != null) {
      dir.concat(", ").concat(barrio);
    }
    if (direccion != null) {
      dir.concat(", ").concat(direccion);
    }

    return dir;
  }
}