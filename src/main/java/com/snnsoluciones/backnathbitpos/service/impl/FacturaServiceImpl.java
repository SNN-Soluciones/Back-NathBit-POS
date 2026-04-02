package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.CuentaPorCobrarService;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.FacturaVentaExcelService;
import com.snnsoluciones.backnathbitpos.service.MetricaProductoVendidoService;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import com.snnsoluciones.backnathbitpos.service.VentaInventarioService;
import io.hypersistence.utils.common.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementación del servicio de Factura con validación completa "¡Piensa McFly, piensa!" - Doc
 * Brown Arquitectura La Jachuda 🚀
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

  private final FacturaRepository facturaRepository;
  private final ProductoRepository productoRepository;
  private final ClienteRepository clienteRepository;
  private final TerminalService terminalService;
  private final SesionCajaRepository sesionCajaRepository;
  private final UsuarioRepository usuarioRepository;
  private final FacturaBitacoraRepository bitacoraRepository;
  private final ClienteService clienteService;
  private final CuentaPorCobrarService cuentaPorCobrarService;
  private final FacturaVentaExcelService facturaVentaExcelService;
  private final MetricaProductoVendidoService metricaProductoService;
  private final PlataformaDigitalConfigRepository plataformaDigitalConfigRepository;
  private final StringRedisTemplate redisTemplate;
  private final SesionCajaUsuarioRepository sesionCajaUsuarioRepository;
  private final SesionCajaService sesionCajaService;
//  private final VentaInventarioService ventaInventarioService;

  @Override
  @Transactional
  public Factura crear(CrearFacturaRequest request) {
    log.info("Creando factura tipo: {}", request.getTipoDocumento());

    // PASO 1: Validar estructura según v4.4 de Hacienda
    // Verifica que todos los campos requeridos estén presentes y con formato correcto
    validarEstructuraV44(request);

    // PASO 2: Recalcular totales para asegurar consistencia
    // Recalcula totalImpuesto, totalOtrosCargos y totalComprobante
    recomputarTotalesAutoritativo(request);

    // PASO 3: Validaciones básicas de negocio
    // Verifica que FE tenga cliente, que haya detalles y medios de pago
    validarRequestBasico(request);

    // PASO 4: Validación exhaustiva de totales
    // Verifica que todos los cálculos matemáticos sean correctos
    ValidacionTotalesResponse validacion = validarTotalesCompleto(request);
    if (!validacion.isEsValido()) {
      throw new IllegalArgumentException("Validación de totales falló: " + validacion.getMensaje());
    }

    // PASO 5: Crear entidad Factura vacía
    Factura factura = new Factura();

    // PASO 6: Establecer datos básicos del documento
    factura.setTipoDocumento(request.getTipoDocumento());
    factura.setCondicionVenta(CondicionVenta.fromCodigo(request.getCondicionVenta()));
    factura.setPlazoCredito(request.getPlazoCredito());
    factura.setSituacion(SituacionDocumento.valueOf(request.getSituacionComprobante()));
    factura.setMoneda(request.getMoneda());
    factura.setObservaciones(request.getObservaciones());

    if (request.getV2SesionId() != null) factura.setV2SesionId(request.getV2SesionId());
    if (request.getV2TurnoId()  != null) factura.setV2TurnoId(request.getV2TurnoId());

    // Actividad del receptor (solo para FE con cliente persona jurídica)
    if (request.getActividadReceptor() != null) {
      factura.setActividadReceptor(request.getActividadReceptor());
    }

    // Vuelto (para pagos en efectivo)
    if (request.getVuelto() != null) {
      factura.setVuelto(request.getVuelto());
    }

    // PASO 7: Establecer cliente (opcional para TE)
    if (request.getClienteId() != null) {
      Cliente cliente = clienteRepository.findById(request.getClienteId())
          .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
      factura.setCliente(cliente);
    }

    if (request.getNombreReceptor() != null && !request.getNombreReceptor().trim().isEmpty()) {
      factura.setNombreReceptor(request.getNombreReceptor().trim());
    }

    // PASO 8: Manejar tipo de cambio
    BigDecimal tc = request.getTipoCambio() != null ? request.getTipoCambio() : BigDecimal.ONE;
    factura.setTipoCambio(tc);

    // PASO 9: Manejar email del receptor
    if (request.getEmailReceptor() != null && !request.getEmailReceptor().isBlank()) {
      factura.setEmailReceptor(request.getEmailReceptor());
    } else if (request.getClienteId() != null) {
      // Si no viene email, usar el del cliente
      Cliente cliente = clienteService.obtenerPorId(request.getClienteId());
      if (cliente != null) {
        String emailPrincipal = clienteService.obtenerEmailSugerido(cliente.getId());
        factura.setEmailReceptor(emailPrincipal);
      }
    }

    // Versión de catálogos (v4.4)
    factura.setVersionCatalogos(request.getVersionCatalogos());


    Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    // PASO 10: Establecer terminal, sucursal y sesión
    Terminal terminal;
    if (request.getTerminalId() != null && request.getTerminalId() > 0) {
      terminal = terminalService.buscarPorId(request.getTerminalId())
          .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
    } else {
      if (!usuario.getRol().equals(RolNombre.SUPER_ADMIN)) {
        throw new BusinessException("Se requiere especificar una terminal");
      }

      // Buscar terminales de la sucursal
      List<Terminal> terminales = terminalService.listarPorSucursal(request.getSucursalId());

      if (terminales.isEmpty()) {
        throw new RuntimeException("No hay terminales disponibles en la sucursal");
      }

      terminal = terminales.get(0); // Primera terminal disponible
      log.info("⚠️ SUPER_ADMIN usando terminal automática: {}", terminal.getNumeroTerminal());
    }

    factura.setTerminal(terminal);
    factura.setSucursal(terminal.getSucursal());


    if (request.getSesionCajaId() != null) {
      // Si viene sesión, la asignamos normalmente
      sesionCajaRepository.findById(request.getSesionCajaId())
          .ifPresent(factura::setSesionCaja);

      sesionCajaUsuarioRepository
          .findTurnoActivoUsuario(request.getUsuarioId())
          .ifPresent(factura::setSesionCajaUsuario);
    } else {
      // Si NO viene sesión, solo permitimos si es SUPER_ADMIN
      if (!usuario.getRol().equals(RolNombre.SUPER_ADMIN)) {
        throw new BusinessException(
            "Se requiere una sesión de caja abierta para crear facturas. " +
                "Solo usuarios SUPER_ADMIN pueden facturar sin sesión de caja."
        );
      }
      log.info("Usuario SUPER_ADMIN {} facturando sin sesión de caja", usuario.getUsername());
    }

    SesionCajaUsuario turnoActivo = null;
    if (factura.getSesionCaja() != null) {
      final SesionCaja sesionCajaFinal = factura.getSesionCaja();

      // Buscar turno activo del usuario en ESTA sesión
      turnoActivo = sesionCajaUsuarioRepository
          .findTurnoActivoUsuarioEnSesion(usuario.getId(), sesionCajaFinal.getId())
          .orElseGet(() -> {
            if ("SHARED".equals(sesionCajaFinal.getModoCaja())) {
              // Antes de auto-unir, verificar si tiene turno activo en otra sesión
              // Si ya tiene uno activo en CUALQUIER sesión, usar ese
              Optional<SesionCajaUsuario> turnoEnOtraSesion =
                  sesionCajaUsuarioRepository.findTurnoActivoUsuario(usuario.getId());

              if (turnoEnOtraSesion.isPresent()) {
                // Tiene turno activo pero en sesión diferente — no auto-unir
                throw new RuntimeException(
                    "El usuario tiene un turno activo en: "
                        + turnoEnOtraSesion.get().getSesionCaja().getTerminal().getNombre()
                        + ". Verificá que estás facturando en la terminal correcta."
                );
              }

              // No tiene turno en ninguna sesión → auto-join válido
              log.info("Auto-join: creando turno para usuario {} en sesión {}",
                  usuario.getId(), sesionCajaFinal.getId());
              return sesionCajaService.unirseATurno(usuario.getId(), sesionCajaFinal.getId());
            }
            return null;
          });
      factura.setSesionCajaUsuario(turnoActivo);
    }


// Asignar el usuario (cajero)
    factura.setCajero(usuario);


    // PASO 11: Generar consecutivo único para este tipo de documento
    String consecutivo = terminalService.generarNumeroConsecutivo(
        terminal.getId(),
        request.getTipoDocumento()
    );
    factura.setConsecutivo(consecutivo);
    factura.setNumeroViper(request.getNumeroViper());

    // PASO 12: Establecer fecha de emisión (hora de Costa Rica)
    ZonedDateTime fechaEmisionCR = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
    factura.setFechaEmision(fechaEmisionCR.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    // PASO 13: Si es electrónica, generar código seguridad y clave
    if (factura.esElectronica()) {
      factura.generarCodigoSeguridad(); // Genera 8 dígitos aleatorios
      String clave = generarClave(factura); // Genera clave de 50 dígitos
      factura.setClave(clave);
      log.info("Clave generada: {} para consecutivo: {}", clave, consecutivo);
    }

    // PASO 14: Asignar todos los totales ya validados
    asignarTotales(factura, request);

    // PASO 15: Guardar descuento global si existe
    if (request.getDescuentoGlobalPorcentaje() != null) {
      factura.setDescuentoGlobalPorcentaje(request.getDescuentoGlobalPorcentaje());
      factura.setMontoDescuentoGlobal(request.getMontoDescuentoGlobal());
      factura.setMotivoDescuentoGlobal(request.getMotivoDescuentoGlobal());
    }

    // PASO 16: Procesar líneas de detalle (productos/servicios)
    procesarDetalles(factura, request.getDetalles());

    // PASO 17: Procesar otros cargos (ej: 10% servicio)
    procesarOtrosCargos(factura, request.getOtrosCargos());

    // PASO 18: Procesar medios de pago
    procesarMediosPago(factura, request.getMediosPago());

    // PASO 19: Procesar resumen de impuestos
    procesarResumenImpuestos(factura, request.getResumenImpuestos());

    // PASO 20: Guardar factura por primera vez
    factura.setEstado(EstadoFactura.GENERADA);
    Factura facturaGuardada = facturaRepository.save(factura);

    // PASO 21: Si es NOTA DE CRÉDITO, procesar información de referencia
    if (request.getTipoDocumento() == TipoDocumento.NOTA_CREDITO) {
      if (request.getInformacionReferencia() != null && !request.getInformacionReferencia()
          .isEmpty()) {
        InformacionReferenciaDto ref = request.getInformacionReferencia().get(0);

        // IMPORTANTE: usar getTipoDoc() NO getCodigo()
        // getTipoDoc() = "01" (Factura Electrónica), "04" (Tiquete), etc.
        facturaGuardada.setTipoDocReferencia(
            TipoDocumento.fromCodigo(ref.getTipoDoc())
        );

        // Número puede ser clave (50 dig) o consecutivo
        facturaGuardada.setNumeroReferencia(ref.getNumero());

        // Fecha del documento original
        facturaGuardada.setFechaEmisionReferencia(ref.getFechaEmision());

        // Código de motivo: 01=Anula, 02=Corrige monto, 06=Devolución
        facturaGuardada.setCodigoReferencia(ref.getCodigo());

        // Descripción textual del motivo
        facturaGuardada.setRazonReferencia(ref.getRazon());

        // Establecer relación con factura original si viene el ID
        if (request.getFacturaReferenciaId() != null) {
          Factura facturaOriginal = facturaRepository.findById(request.getFacturaReferenciaId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Factura de referencia no encontrada"));
          facturaGuardada.setFacturaReferencia(facturaOriginal);
        }
      }

      // PASO 22: Guardar cambios de la nota de crédito
      facturaGuardada = facturaRepository.save(facturaGuardada);
    }

    // PASO 23: Si es electrónica, crear job para envío asíncrono a Hacienda
    if (factura.esElectronica() && factura.getClave() != null) {
      try {
        FacturaBitacora bitacora = FacturaBitacora.builder()
            .facturaId(facturaGuardada.getId())
            .clave(facturaGuardada.getClave())
            .estado(EstadoBitacora.PENDIENTE)
            .intentos(0)
            .build();

        bitacoraRepository.save(bitacora);

        log.info("Bitácora creada para procesar factura electrónica: {} - ID Bitácora: {}",
            facturaGuardada.getClave(), bitacora.getId());

        // ---------------------------------------------------------------
        // 🚀 NUEVO: EL DISPARO A REDIS (Cero Lag)
        // ---------------------------------------------------------------
        // Usamos TransactionSynchronization para enviar el mensaje SOLO
        // cuando la transacción de la DB haya hecho commit exitoso.
        // Esto evita que el microservicio busque la factura antes de que exista.
        final Long facturaIdFinal = facturaGuardada.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              // Usamos la variable final 'facturaIdFinal'
              String mensaje = String.valueOf(facturaIdFinal);
              redisTemplate.convertAndSend("facturacion_queue", mensaje);
              log.info("🚀 [REDIS] Evento enviado para Factura ID: {}", facturaIdFinal);
            } catch (Exception e) {
              log.error("⚠️ [REDIS] Falló el envío inmediato: {}", e.getMessage());
            }
          }
        });

      } catch (Exception e) {
        // No fallar si hay error creando bitácora

        log.error("Error gestionando bitácora/redis para factura {}: {}",
            facturaGuardada.getClave(), e.getMessage());
      }
    }

    if (factura.getCondicionVenta() == CondicionVenta.CREDITO &&
        factura.getCliente() != null) {

      // Validar que cliente pueda comprar a crédito
      if (!clienteService.puedeComprarACredito(
          factura.getCliente().getId(),
          factura.getTotalComprobante())) {
        throw new BusinessException("Cliente no puede comprar a crédito");
      }

      // Crear la cuenta por cobrar
      cuentaPorCobrarService.crearDesdeFactura(factura);
    }

//    try {
//      ventaInventarioService.descontarInventarioFactura(factura);
//    } catch (Exception e) {
//      log.error("Error descontando inventario de factura {}: {}",
//          factura.getConsecutivo(), e.getMessage());
//      // NO lanzamos el error, la factura ya se guardó
//      // Se registró el warning en los logs
//    }

    // PASO 24: Retornar la factura creada
    metricaProductoService.actualizarDesdeFactura(facturaGuardada);
    return facturaGuardada;
  }

  /**
   * Validaciones básicas del request
   */
  private void validarRequestBasico(CrearFacturaRequest request) {
    // Tipo documento vs cliente
    if (request.getTipoDocumento() == TipoDocumento.FACTURA_ELECTRONICA
        && request.getClienteId() == null) {
      throw new IllegalArgumentException("Factura Electrónica requiere cliente");
    }

    // Validar al menos un detalle
    if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
      throw new IllegalArgumentException("La factura debe tener al menos un detalle");
    }

    // Validar medios de pago
    if (request.getMediosPago() == null || request.getMediosPago().isEmpty()) {
      throw new IllegalArgumentException("La factura debe tener al menos un medio de pago");
    }
  }

  /**
   * VALIDACIÓN COMPLETA - El checkeo del Doc Validamos TODOS los cálculos sin recalcular
   */
  private ValidacionTotalesResponse validarTotalesCompleto(CrearFacturaRequest request) {
    List<String> advertencias = new ArrayList<>();
    boolean esValido = true;

    try {
      // 1. Validar totales de líneas
      BigDecimal sumaTotalLineas = BigDecimal.ZERO;
      BigDecimal sumaDescuentosLineas = BigDecimal.ZERO;
      BigDecimal sumaImpuestosLineas = BigDecimal.ZERO;

      for (DetalleFacturaRequest detalle : request.getDetalles()) {
        // Validar que precio * cantidad = montoTotal
        BigDecimal montoCalculado = detalle.getCantidad()
            .multiply(detalle.getPrecioUnitario())
            .setScale(5, RoundingMode.HALF_UP);

        if (!sonIguales(montoCalculado, detalle.getMontoTotal())) {
          advertencias.add(String.format(
              "Línea %d: Monto total no cuadra. Esperado: %.2f, Recibido: %.2f",
              detalle.getNumeroLinea(), montoCalculado, detalle.getMontoTotal()
          ));
          esValido = false;
        }

        // Validar descuentos no excedan el monto
        if (detalle.getMontoDescuento().compareTo(detalle.getMontoTotal()) > 0) {
          advertencias.add(String.format(
              "Línea %d: Descuento (%.2f) excede monto total (%.2f)",
              detalle.getNumeroLinea(), detalle.getMontoDescuento(), detalle.getMontoTotal()
          ));
          esValido = false;
        }

        // Validar subtotal = montoTotal - descuentos
        BigDecimal subtotalCalculado = detalle.getMontoTotal()
            .subtract(detalle.getMontoDescuento());

        if (!sonIguales(subtotalCalculado, detalle.getSubtotal())) {
          advertencias.add(String.format(
              "Línea %d: Subtotal no cuadra. Esperado: %.2f, Recibido: %.2f",
              detalle.getNumeroLinea(), subtotalCalculado, detalle.getSubtotal()
          ));
          esValido = false;
        }

        // Validar total línea = subtotal + impuestos
        BigDecimal totalLineaCalculado = detalle.getSubtotal()
            .add(detalle.getMontoImpuesto());

        if (!sonIguales(totalLineaCalculado, detalle.getMontoTotalLinea())) {
          advertencias.add(String.format(
              "Línea %d: Total línea no cuadra. Esperado: %.2f, Recibido: %.2f",
              detalle.getNumeroLinea(), totalLineaCalculado, detalle.getMontoTotalLinea()
          ));
          esValido = false;
        }

        // Acumular para validación general
        sumaTotalLineas = sumaTotalLineas.add(detalle.getMontoTotalLinea());
        sumaDescuentosLineas = sumaDescuentosLineas.add(detalle.getMontoDescuento());
        sumaImpuestosLineas = sumaImpuestosLineas.add(detalle.getMontoImpuesto());
      }

      // 2. Validar descuento global
      if (request.getMontoDescuentoGlobal() != null &&
          request.getMontoDescuentoGlobal().compareTo(BigDecimal.ZERO) > 0) {
        // Si hay porcentaje, validar cálculo
        if (request.getDescuentoGlobalPorcentaje() != null) {
          // El descuento global se aplica sobre el subtotal después de descuentos de línea
          BigDecimal baseDescuentoGlobal = request.getTotalVenta();
          BigDecimal descuentoCalculado = baseDescuentoGlobal
              .multiply(request.getDescuentoGlobalPorcentaje())
              .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);

          if (!sonIguales(descuentoCalculado, request.getMontoDescuentoGlobal())) {
            advertencias.add(String.format(
                "Descuento global no cuadra. Esperado: %.2f, Recibido: %.2f",
                descuentoCalculado, request.getMontoDescuentoGlobal()
            ));
            esValido = false;
          }
        }
      }

      // 3. Validar total descuentos
      BigDecimal totalDescuentosCalculado = sumaDescuentosLineas
          .add(request.getMontoDescuentoGlobal() != null ? request.getMontoDescuentoGlobal()
              : BigDecimal.ZERO);

      if (!sonIguales(totalDescuentosCalculado, request.getTotalDescuentos())) {
        advertencias.add(String.format(
            "Total descuentos no cuadra. Esperado: %.2f, Recibido: %.2f",
            totalDescuentosCalculado, request.getTotalDescuentos()
        ));
        esValido = false;
      }

      // 4. Validar otros cargos (incluye servicio 10%)
      BigDecimal sumaOtrosCargos = BigDecimal.ZERO;
      if (request.getOtrosCargos() != null) {
        sumaOtrosCargos = request.getOtrosCargos().stream()
            .map(OtroCargoRequest::getMontoCargo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
      }

      if (!sonIguales(sumaOtrosCargos, request.getTotalOtrosCargos())) {
        advertencias.add(String.format(
            "Total otros cargos no cuadra. Esperado: %.2f, Recibido: %.2f",
            sumaOtrosCargos, request.getTotalOtrosCargos()
        ));
        esValido = false;
      }

      // 5. Validar resumen de impuestos
      if (request.getResumenImpuestos() != null) {
        BigDecimal sumaResumenImpuestos = request.getResumenImpuestos().stream()
            .map(ResumenImpuestoRequest::getTotalImpuestoNeto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!sonIguales(sumaResumenImpuestos, request.getTotalImpuesto())) {
          advertencias.add(String.format(
              "Total impuestos no cuadra. Esperado: %.2f, Recibido: %.2f",
              sumaResumenImpuestos, request.getTotalImpuesto()
          ));
          esValido = false;
        }
      }

      // 6. Validar total venta neta = totalVenta - totalDescuentos
      BigDecimal totalVentaNetaCalculado = request.getTotalVenta()
          .subtract(request.getTotalDescuentos());

      if (!sonIguales(totalVentaNetaCalculado, request.getTotalVentaNeta())) {
        advertencias.add(String.format(
            "Total venta neta no cuadra. Esperado: %.2f, Recibido: %.2f",
            totalVentaNetaCalculado, request.getTotalVentaNeta()
        ));
        esValido = false;
      }

      // 7. Validar total comprobante
      BigDecimal totalComprobanteCalculado = request.getTotalVentaNeta()
          .add(request.getTotalImpuesto())
          .add(request.getTotalOtrosCargos());

      if (!sonIguales(totalComprobanteCalculado, request.getTotalComprobante())) {
        advertencias.add(String.format(
            "Total comprobante no cuadra. Esperado: %.2f, Recibido: %.2f",
            totalComprobanteCalculado, request.getTotalComprobante()
        ));
        esValido = false;
      }

      // 8. Validar total medios de pago
      BigDecimal totalMediosPago = request.getMediosPago().stream()
          .map(MedioPagoRequest::getMonto)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (!sonIguales(totalMediosPago, request.getTotalComprobante())) {
        advertencias.add(String.format(
            "Total medios de pago (%.2f) no coincide con total comprobante (%.2f)",
            totalMediosPago, request.getTotalComprobante()
        ));
        esValido = false;
      }

      // 9. Validaciones específicas de Hacienda
      // Validar que si hay gravados, hay impuestos
      if (request.getTotalGravado().compareTo(BigDecimal.ZERO) > 0 &&
          request.getTotalImpuesto().compareTo(BigDecimal.ZERO) == 0) {
        advertencias.add("Hay montos gravados pero no hay impuestos");
        esValido = false;
      }

      // Validar totales de servicios y mercancías
      BigDecimal totalServiciosCalculado = request.getTotalServiciosGravados()
          .add(request.getTotalServiciosExentos())
          .add(request.getTotalServiciosExonerados())
          .add(request.getTotalServiciosNoSujetos());

      BigDecimal totalMercanciasCalculado = request.getTotalMercanciasGravadas()
          .add(request.getTotalMercanciasExentas())
          .add(request.getTotalMercanciasExoneradas())
          .add(request.getTotalMercanciasNoSujetas());

      BigDecimal totalVentaCalculado = totalServiciosCalculado.add(totalMercanciasCalculado);

      // Justo antes de la validación de totales de servicios y mercancías
      log.info("=== VALIDACIÓN DE TOTALES ===");
      log.info("totalServiciosGravados: {}", request.getTotalServiciosGravados());
      log.info("totalServiciosExentos: {}", request.getTotalServiciosExentos());
      log.info("totalServiciosExonerados: {}", request.getTotalServiciosExonerados());
      log.info("totalServiciosNoSujetos: {}", request.getTotalServiciosNoSujetos());
      log.info("totalServiciosCalculado: {}", totalServiciosCalculado);

      log.info("totalMercanciasGravadas: {}", request.getTotalMercanciasGravadas());
      log.info("totalMercanciasExentas: {}", request.getTotalMercanciasExentas());
      log.info("totalMercanciasExoneradas: {}", request.getTotalMercanciasExoneradas());
      log.info("totalMercanciasNoSujetas: {}", request.getTotalMercanciasNoSujetas());
      log.info("totalMercanciasCalculado: {}", totalMercanciasCalculado);

      log.info("totalVentaCalculado: {}", totalVentaCalculado);
      log.info("totalVenta request: {}", request.getTotalVenta());
      log.info("Son iguales?: {}", sonIguales(totalVentaCalculado, request.getTotalVenta()));

      BigDecimal totalVentas = request.getTotalVenta();

      if (!sonIguales(totalVentaCalculado, totalVentas)) {
        advertencias.add(String.format(
            "Total venta (%.2f) no coincide con suma de servicios+mercancías (%.2f)",
            request.getTotalVenta(), totalVentaCalculado
        ));
        esValido = false;
      }

    } catch (Exception e) {
      log.error("Error en validación de totales", e);
      advertencias.add("Error interno en validación: " + e.getMessage());
      esValido = false;
    }

    return ValidacionTotalesResponse.builder()
        .esValido(esValido)
        .mensaje(esValido ? "Validación exitosa" : "Validación falló")
        .advertencias(advertencias)
        .build();
  }

  /**
   * Helper para comparar BigDecimals con tolerancia
   */
  private boolean sonIguales(BigDecimal valor1, BigDecimal valor2) {
    if (valor1 == null || valor2 == null) {
      return valor1 == valor2;
    }
    // Tolerancia de 1 centavo
    return valor1.subtract(valor2).abs().compareTo(new BigDecimal("0.01")) <= 0;
  }

  /**
   * Asignar totales ya validados a la factura
   */
  private void asignarTotales(Factura factura, CrearFacturaRequest request) {
    // Totales por tipo
    factura.setTotalServiciosGravados(request.getTotalServiciosGravados());
    factura.setTotalServiciosExentos(request.getTotalServiciosExentos());
    factura.setTotalServiciosExonerados(request.getTotalServiciosExonerados());
    factura.setTotalServiciosNoSujetos(request.getTotalServiciosNoSujetos());

    factura.setTotalMercanciasGravadas(request.getTotalMercanciasGravadas());
    factura.setTotalMercanciasExentas(request.getTotalMercanciasExentas());
    factura.setTotalMercanciasExoneradas(request.getTotalMercanciasExoneradas());
    factura.setTotalMercanciasNoSujetas(request.getTotalMercanciasNoSujetas());

    // Totales generales
    factura.setTotalGravado(request.getTotalGravado());
    factura.setTotalExento(request.getTotalExento());
    factura.setTotalExonerado(request.getTotalExonerado());
    factura.setTotalNoSujeto(request.getTotalNoSujeto());

    factura.setTotalVenta(request.getTotalVenta());
    factura.setTotalDescuentos(request.getTotalDescuentos());
    factura.setTotalVentaNeta(request.getTotalVentaNeta());
    factura.setTotalImpuesto(request.getTotalImpuesto());
    factura.setTotalIVADevuelto(request.getTotalIVADevuelto());
    factura.setTotalOtrosCargos(request.getTotalOtrosCargos());
    factura.setTotalComprobante(request.getTotalComprobante());
  }

  /**
   * Procesar detalles sin calcular, solo guardar
   */
  private void procesarDetalles(Factura factura, List<DetalleFacturaRequest> detallesReq) {
    for (DetalleFacturaRequest detalleReq : detallesReq) {
      // Buscar producto
      Producto producto = productoRepository.findById(detalleReq.getProductoId())
          .orElseThrow(
              () -> new RuntimeException("Producto no encontrado: " + detalleReq.getProductoId()));

      // Crear detalle
      FacturaDetalle detalle = new FacturaDetalle();
      detalle.setNumeroLinea(detalleReq.getNumeroLinea());
      detalle.setProducto(producto);
      detalle.setCantidad(detalleReq.getCantidad());
      detalle.setUnidadMedida(detalleReq.getUnidadMedida());
      detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());
      detalle.setCodigoCabys(detalleReq.getCodigoCabys() != null ?
          detalleReq.getCodigoCabys() : producto.getEmpresaCabys().getCodigoCabys().getCodigo());
      detalle.setDetalle(producto.getNombre());

      if (detalleReq.getDescripcionPersonalizada() != null
          && !detalleReq.getDescripcionPersonalizada().trim().isEmpty()) {
        detalle.setDescripcionPersonalizada(detalleReq.getDescripcionPersonalizada().trim());
      }

      // Asignar flags
      detalle.setEsServicio(detalleReq.getEsServicio());
      detalle.setAplicaImpuestoServicio(detalleReq.getAplicaImpuestoServicio());

      // Asignar montos ya calculados
      detalle.setMontoTotal(detalleReq.getMontoTotal());
      detalle.setMontoDescuento(detalleReq.getMontoDescuento());
      detalle.setSubtotal(detalleReq.getSubtotal());
      detalle.setMontoImpuesto(detalleReq.getMontoImpuesto());
      detalle.setMontoTotalLinea(detalleReq.getMontoTotalLinea());

      // Procesar descuentos de la línea
      if (detalleReq.getDescuentos() != null) {
        for (DescuentoRequest descReq : detalleReq.getDescuentos()) {
          FacturaDescuento descuento = new FacturaDescuento();
          descuento.setCodigoDescuento(descReq.getCodigoDescuento());
          descuento.setCodigoDescuentoOTRO(descReq.getCodigoDescuentoOTRO());
          descuento.setNaturalezaDescuento(descReq.getNaturalezaDescuento());
          descuento.setPorcentaje(descReq.getPorcentaje());
          descuento.setMontoDescuento(descReq.getMontoDescuento());
          descuento.setOrden(descReq.getOrden());

          detalle.agregarDescuento(descuento);
        }
      }

      // Procesar impuestos de la línea
      if (detalleReq.getImpuestos() != null) {
        for (ImpuestoLineaRequest impReq : detalleReq.getImpuestos()) {
          FacturaDetalleImpuesto impuesto = FacturaDetalleImpuesto.builder()
              .codigoImpuesto(impReq.getCodigoImpuesto())
              .codigoTarifaIVA(impReq.getCodigoTarifaIVA())
              .tarifa(impReq.getTarifa())
              .montoImpuesto(impReq.getMontoImpuesto())
              .baseImponible(impReq.getBaseImponible())
              .tieneExoneracion(impReq.getTieneExoneracion())
              .montoExoneracion(impReq.getMontoExoneracion())
              .impuestoNeto(impReq.getImpuestoNeto())
              .build();

          // Si tiene exoneración, agregar datos
          if (impReq.getTieneExoneracion() && impReq.getExoneracion() != null) {
            ExoneracionRequest exo = impReq.getExoneracion();
            impuesto.setTipoDocumentoExoneracion(exo.getTipoDocumentoEX());
            impuesto.setNumeroDocumentoExoneracion(exo.getNumeroDocumentoEX());
            impuesto.setNombreInstitucion(exo.getInstitucionOtorgante());
            impuesto.setFechaEmisionExoneracion(exo.getFechaEmisionExoneracion().toString());
            impuesto.setTarifaExonerada(exo.getPorcentajeExonerado());
            impuesto.setCodigoInstitucion(exo.getCodigoInstitucion());
          }

          detalle.agregarImpuesto(impuesto);
        }
      }

      detalle.setOpcionesSeleccionadas(detalleReq.getOpcionesSeleccionadas());

      factura.agregarDetalle(detalle);
    }
  }

  /**
   * Procesar otros cargos
   */
  private void procesarOtrosCargos(Factura factura, List<OtroCargoRequest> otrosCargosReq) {
    if (otrosCargosReq == null || otrosCargosReq.isEmpty()) {
      return;
    }

    for (OtroCargoRequest cargoReq : otrosCargosReq) {
      OtroCargo cargo = new OtroCargo();
      cargo.setTipoDocumentoOC(cargoReq.getTipoDocumentoOC());
      cargo.setTipoDocumentoOTROS(cargoReq.getTipoDocumentoOTROS());
      cargo.setNombreCargo(cargoReq.getNombreCargo());
      cargo.setPorcentaje(cargoReq.getPorcentaje());
      cargo.setMontoCargo(cargoReq.getMontoCargo());

      // Si es cobro de tercero (04)
      if ("04".equals(cargoReq.getTipoDocumentoOC())
          && cargoReq.getTerceroTipoIdentificacion() != null) {
        cargo.setTerceroTipoIdentificacion(cargoReq.getTerceroTipoIdentificacion());
        cargo.setTerceroNumeroIdentificacion(cargoReq.getTerceroNumeroIdentificacion());
        cargo.setTerceroNombre(cargoReq.getTerceroNombre());
      }

      factura.agregarOtroCargo(cargo);
    }
  }

  /**
   * Procesar medios de pago
   */
  private void procesarMediosPago(Factura factura, List<MedioPagoRequest> mediosPagoReq) {
    for (MedioPagoRequest mpReq : mediosPagoReq) {
      FacturaMedioPago medioPago = new FacturaMedioPago();
      medioPago.setMedioPago(MedioPago.fromCodigo(mpReq.getMedioPago()));
      medioPago.setMonto(mpReq.getMonto());
      medioPago.setReferencia(mpReq.getReferencia());
      medioPago.setBanco(mpReq.getBanco());

      if (mpReq.getPlataformaDigitalId() != null) {
        PlataformaDigitalConfig plataforma = plataformaDigitalConfigRepository
            .findById(mpReq.getPlataformaDigitalId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Plataforma digital no encontrada: " + mpReq.getPlataformaDigitalId()));

        medioPago.setPlataformaDigital(plataforma);
      }

      factura.agregarMedioPago(medioPago);
    }
  }

  /**
   * Procesar resumen de impuestos
   */
  private void procesarResumenImpuestos(Factura factura, List<ResumenImpuestoRequest> resumenReq) {
    if (resumenReq == null || resumenReq.isEmpty()) {
      return;
    }

    for (ResumenImpuestoRequest resumen : resumenReq) {
      FacturaResumenImpuesto resumenImpuesto = FacturaResumenImpuesto.builder()
          .codigoImpuesto(resumen.getCodigoImpuesto())
          .codigoTarifaIVA(resumen.getCodigoTarifaIVA())
          .totalMontoImpuesto(resumen.getTotalMontoImpuesto())
          .totalBaseImponible(resumen.getTotalBaseImponible())
          .totalMontoExoneracion(resumen.getTotalMontoExoneracion())
          .totalImpuestoNeto(resumen.getTotalImpuestoNeto())
          .cantidadLineas(resumen.getCantidadLineas())
          .build();

      factura.agregarResumenImpuesto(resumenImpuesto);
    }
  }

  /**
   * Generar clave de 50 dígitos según Hacienda
   */
  private String generarClave(Factura factura) {
    // Implementación de generación de clave
    // [PAÍS(3)] + [FECHA(8)] + [IDENTIFICACIÓN(12)] + [CONSECUTIVO(20)] + [SITUACIÓN(1)] + [SEGURIDAD(8)]

    StringBuilder clave = new StringBuilder();

    // País (Costa Rica)
    clave.append("506");

    // Fecha DDMMAAAA
    LocalDateTime fecha = LocalDateTime.now();
    clave.append(String.format("%02d%02d%02d",
        fecha.getDayOfMonth(),
        fecha.getMonthValue(),
        fecha.getYear() % 100));

    // Identificación del emisor (12 dígitos)
    String identificacion = factura.getSucursal().getEmpresa().getIdentificacion();
    identificacion = identificacion.replaceAll("[^0-9]", "");
    clave.append(String.format("%012d", Long.parseLong(identificacion)));

    // Consecutivo (20 dígitos)
    clave.append(factura.getConsecutivo());

    // Situación (1=Normal, 2=Contingencia, 3=Sin Internet)
    clave.append(factura.getSituacion().getCodigo());

    // Código seguridad (8 dígitos)
    clave.append(factura.getCodigoSeguridad());

    return clave.toString();
  }

  // Implementar otros métodos de la interfaz...

  @Override
  @Transactional(readOnly = true)
  public Optional<Factura> buscarPorId(Long id) {
    return facturaRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Factura> buscarPorClave(String clave) {
    return facturaRepository.findByClave(clave);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Factura> buscarPorConsecutivo(String consecutivo) {
    return facturaRepository.findByConsecutivo(consecutivo);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Factura> listarPorSesionCaja(Long sesionCajaId) {
    return facturaRepository.findBySesionCajaId(sesionCajaId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Factura> listarFacturasConError(Long sucursalId) {
    return facturaRepository.findBySucursalIdAndEstado(sucursalId, EstadoFactura.ERROR);
  }

  @Override
  @Transactional
  public Factura anular(Long facturaId, String motivo) {
    Factura factura = facturaRepository.findById(facturaId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    if (!factura.getEstado().puedeAnularse()) {
      throw new RuntimeException(
          "La factura no puede ser anulada en estado: " + factura.getEstado());
    }

    factura.setEstado(EstadoFactura.ANULADA);
    factura.setObservaciones(factura.getObservaciones() + " | ANULADA: " + motivo);

    log.info("Factura {} anulada. Motivo: {}", factura.getClave(), motivo);
    return facturaRepository.save(factura);
  }

  @Override
  @Transactional
  public void reenviar(Long facturaId) {
    Factura factura = facturaRepository.findById(facturaId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    if (!factura.getEstado().puedeReprocesarse()) {
      throw new RuntimeException(
          "La factura no puede ser reenviada en estado: " + factura.getEstado());
    }
  }

  @Override
  public ValidacionTotalesResponse validarTotales(ValidacionTotalesRequest request) {
    // Usar la misma validación completa
    return validarTotalesCompleto(request);
  }

  @Override
  public Page<FacturaReferenciaDto> buscarParaReferencia(BuscarFacturaReferenciaRequest request) {
    Specification<Factura> spec = crearEspecificacionBusquedaReferencia(request);

    Pageable pageable = PageRequest.of(
        request.getPagina(),
        request.getTamanio(),
        Sort.by(Sort.Direction.DESC, "fechaEmision")
    );

    Page<Factura> facturas = facturaRepository.findAll(spec, pageable);

    return facturas.map(this::convertirAFacturaReferenciaDto);
  }

  /**
   * Crear especificación para búsqueda de facturas para referencia
   */
  private Specification<Factura> crearEspecificacionBusquedaReferencia(
      BuscarFacturaReferenciaRequest request) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Filtro por empresa (obligatorio)
      if (request.getEmpresaId() != null) {
        predicates.add(cb.equal(root.get("empresa").get("id"), request.getEmpresaId()));
      }

      // Filtro por sucursal (opcional)
      if (request.getSucursalId() != null) {
        predicates.add(cb.equal(root.get("sucursal").get("id"), request.getSucursalId()));
      }

      // Solo facturas aceptadas/procesadas
      predicates.add(cb.in(root.get("estado")).value(Arrays.asList("ACEPTADA", "PROCESADA")));

      // Búsqueda por término libre
      if (request.getTermino() != null && !request.getTermino().trim().isEmpty()) {
        String termino = "%" + request.getTermino().toLowerCase().trim() + "%";

        Predicate clavePredicate = cb.like(cb.lower(root.get("clave")), termino);
        Predicate consecutivoPredicate = cb.like(cb.lower(root.get("consecutivo")), termino);
        Predicate clientePredicate = cb.like(cb.lower(root.get("clienteNombre")), termino);

        predicates.add(cb.or(clavePredicate, consecutivoPredicate, clientePredicate));
      }

      // Filtro por rango de fechas
      if (request.getFechaDesde() != null) {
        predicates.add(cb.greaterThanOrEqualTo(
            root.get("fechaEmision").as(LocalDate.class),
            request.getFechaDesde()
        ));
      }

      if (request.getFechaHasta() != null) {
        predicates.add(cb.lessThanOrEqualTo(
            root.get("fechaEmision").as(LocalDate.class),
            request.getFechaHasta()
        ));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Convertir Factura a FacturaReferenciaDto
   */
  private FacturaReferenciaDto convertirAFacturaReferenciaDto(Factura factura) {
    return FacturaReferenciaDto.builder()
        .id(factura.getId())
        .clave(factura.getClave())
        .consecutivo(factura.getConsecutivo())
        .fechaEmision(LocalDateTime.parse(factura.getFechaEmision()))
        .tipoDocumento("01") // Siempre será Factura Electrónica
        .clienteNombre(factura.getCliente().getRazonSocial())
        .clienteIdentificacion(factura.getCliente().getNumeroIdentificacion())
        .totalComprobante(factura.getTotalComprobante())
        .moneda(factura.getMoneda().name())
        .estado(factura.getEstado().name())
        .empresaNombre("")
        .sucursalNombre(factura.getSucursal().getNombre())
        .build();
  }

  private void validarFacturaParaNotaCredito(Factura factura) {
    // Validar estado
    if (factura.getEstado() == EstadoFactura.TOTALMENTE_ACREDITADA) {
      throw new IllegalStateException("Esta factura ya fue acreditada completamente");
    }

    if (factura.getEstado() != EstadoFactura.ACEPTADA &&
        factura.getEstado() != EstadoFactura.PARCIALMENTE_ACREDITADA) {

      if (factura.getEstado() == EstadoFactura.PROCESANDO ||
          factura.getEstado() == EstadoFactura.ENVIADA) {
        throw new IllegalStateException(
            "La factura aún está en proceso. Por favor espere a que sea aceptada por Hacienda"
        );
      }

      if (factura.getEstado() == EstadoFactura.RECHAZADA) {
        throw new IllegalStateException(
            "No se puede generar nota de crédito para una factura rechazada"
        );
      }

      throw new IllegalStateException(
          "Solo se pueden generar notas de crédito para facturas aceptadas. Estado actual: "
              + factura.getEstado()
      );
    }

    // Validar antigüedad (30 días)
    LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
    LocalDateTime fechaFactura;
    try {
      // Si la fecha tiene zona horaria, usar OffsetDateTime
      if (factura.getFechaEmision().contains("+") || factura.getFechaEmision().contains("-")) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(factura.getFechaEmision());
        fechaFactura = offsetDateTime.toLocalDateTime();
      } else {
        // Si no tiene zona horaria, parsear como LocalDateTime
        fechaFactura = LocalDateTime.parse(factura.getFechaEmision());
      }
    } catch (DateTimeParseException e) {
      log.warn("No se pudo parsear la fecha de emisión: {}. Asumiendo fecha válida.",
          factura.getFechaEmision());
      // Si no podemos parsear, asumimos que está dentro del rango válido
      fechaFactura = LocalDateTime.now();
    }

    if (fechaFactura.isBefore(fechaLimite)) {
      log.warn("ADVERTENCIA: Factura {} tiene más de 30 días. " +
              "Esto puede afectar los reportes del Ministerio de Hacienda",
          factura.getConsecutivo());
    }

    // Validar que tenga monto disponible
    if (factura.getMontoAcreditado().compareTo(factura.getTotalComprobante()) >= 0) {
      throw new IllegalStateException("Esta factura no tiene monto disponible para acreditar");
    }
  }

  /**
   * Valida estructura de la factura según v4.4 (sin armar XML): - Catálogos/códigos presentes y con
   * formato válido - Exoneraciones completas cuando se marcan en una línea - Servicio 10% NO como
   * impuesto; debe venir en Otros Cargos (código 06) - Descuento 99 OTROS con código OTRO y
   * naturaleza obligatoria - Medios de pago válidos y suma = totalComprobante (con tolerancia) -
   * Campos de No Sujetos presentes - versionCatalogos presente
   */
  private void validarEstructuraV44(CrearFacturaRequest request) {
    final Pattern P_UNIDAD = Pattern.compile("^[0-9A-Z._-]{1,30}$");
    final Pattern P_CABYS = Pattern.compile("^[0-9]{13}$");
    final Set<String> MEDIOS_VALIDOS = Set.of("01", "02", "03", "04", "05", "06", "07", "99");
    final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    // ------ Encabezado mínimo ------
    requireNonBlank(request.getMoneda().name(), "Moneda es requerida");
    if (Objects.equals(request.getMoneda().getCodigo(), "USD")) {
      requireNotNull(request.getTipoCambio(), "Tipo de cambio es requerido para USD");
    }
    requireNonBlank(request.getCondicionVenta(), "Condición de venta es requerida");
    // Si condición = 99 (OTROS), idealmente validar detalleCondicion (si tienes ese campo en DTO)

    // ------ versionCatalogos ------
    requireNonBlank(request.getVersionCatalogos(), "versionCatalogos es requerido");

    // ------ No Sujetos ------
    requireNotNull(request.getTotalServiciosNoSujetos(),
        "totalServiciosNoSujetos es requerido (puede ser 0)");
    requireNotNull(request.getTotalMercanciasNoSujetas(),
        "totalMercanciasNoSujetos es requerido (puede ser 0)");
    requireNotNull(request.getTotalNoSujeto(), "totalNoSujeto es requerido (puede ser 0)");

    // ------ Detalles ------
    if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
      throw new IllegalArgumentException("Debe incluir al menos un detalle de línea");
    }

    for (int i = 0; i < request.getDetalles().size(); i++) {
      var d = request.getDetalles().get(i);
      final String ctx = " (línea " + (i + 1) + ")";

      // Unidad de medida
      requireNonBlank(d.getUnidadMedida(), "Unidad de medida es requerida" + ctx);
      requireRegex(P_UNIDAD, d.getUnidadMedida(), "Unidad de medida inválida" + ctx);

      // CAByS
      requireNonBlank(d.getCodigoCabys(), "Código CAByS es requerido" + ctx);
      requireRegex(P_CABYS, d.getCodigoCabys(), "CAByS debe tener 13 dígitos" + ctx);

      // Descuentos de línea
      if (d.getDescuentos() != null) {
        for (var desc : d.getDescuentos()) {
          requireNonBlank(desc.getCodigoDescuento(), "Código de descuento es requerido" + ctx);
          if ("99".equals(desc.getCodigoDescuento())) {
            requireNonBlank(desc.getCodigoDescuentoOTRO(),
                "Para código 99 (OTROS) se requiere 'codigoDescuentoOTRO'" + ctx);
            requireNonBlank(desc.getNaturalezaDescuento(),
                "Para código 99 (OTROS) se requiere 'naturalezaDescuento'" + ctx);
          }
        }
      }

      // Impuestos de línea
      if (d.getImpuestos() != null) {
        for (var imp : d.getImpuestos()) {
          // Bloqueo: servicio 10% no puede venir como impuesto
          if ("12".equals(imp.getCodigoImpuesto())) { // ese 12 lo usaban en FE para "servicio"
            throw new IllegalArgumentException(
                "El 10% de servicio NO debe enviarse como impuesto; use Otros Cargos (código 06) en el resumen"
                    + ctx);
          }
          requireNonBlank(imp.getCodigoImpuesto(), "codigoImpuesto requerido" + ctx);
          requireNonBlank(imp.getCodigoTarifaIVA(), "codigoTarifaIVA requerido" + ctx);

          // Exoneración coherente
          if (Boolean.TRUE.equals(imp.getTieneExoneracion())) {
            if (imp.getExoneracion() == null) {
              throw new IllegalArgumentException(
                  "Se marcó tieneExoneracion pero falta ExoneracionRequest" + ctx);
            }
            var ex = imp.getExoneracion();
            requireNonBlank(ex.getTipoDocumentoEX(),
                "Exoneración: tipoDocumentoEX requerido" + ctx);
            requireNonBlank(ex.getNumeroDocumentoEX(),
                "Exoneración: numeroDocumentoEX requerido" + ctx);
            requireNotNull(ex.getFechaEmisionExoneracion(),
                "Exoneración: fechaEmisionExoneracion requerida" + ctx);
            requireNonBlank(ex.getInstitucionOtorgante(),
                "Exoneración: institucionOtorgante requerida" + ctx);
            requireNotNull(ex.getPorcentajeExonerado(),
                "Exoneración: porcentajeExonerado requerido" + ctx);
            if (ex.getPorcentajeExonerado().compareTo(BigDecimal.ZERO) < 0
                || ex.getPorcentajeExonerado().compareTo(new BigDecimal("100")) > 0) {
              throw new IllegalArgumentException(
                  "Exoneración: porcentajeExonerado debe estar entre 0 y 100" + ctx);
            }
          }

          // Impuesto asumido (si marcas la bandera, debe traer monto)
          if (Boolean.TRUE.equals(imp.getImpuestoAsumidoPorEmisor())) {
            if (imp.getMontoImpuestoAsumido() == null
                || imp.getMontoImpuestoAsumido().compareTo(BigDecimal.ZERO) < 0) {
              throw new IllegalArgumentException(
                  "Impuesto asumido por emisor marcado pero sin monto válido" + ctx);
            }
          }
        }
      }
    }

    // ------ Otros Cargos (Servicio 10%) ------
    if (request.getOtrosCargos() != null) {
      for (var oc : request.getOtrosCargos()) {
        // Si tu catálogo usa "06" como código para servicio 10%, aquí podrías reforzarlo:
        // if ("06".equals(oc.getTipoDocumentoOC())) { ...validar porcentaje 10... }
        // Al menos, validar estructura básica:
        requireNonBlank(oc.getTipoDocumentoOC(), "Otros Cargos: tipoDocumentoOC requerido");
        requireNotNull(oc.getMontoCargo(), "Otros Cargos: monto requerido");
        if (oc.getMontoCargo().compareTo(BigDecimal.ZERO) < 0) {
          throw new IllegalArgumentException("Otros Cargos: monto no puede ser negativo");
        }
      }
    }

    // ------ Resumen impuestos (shape) ------
    if (request.getResumenImpuestos() != null) {
      for (var r : request.getResumenImpuestos()) {
        requireNonBlank(r.getCodigoImpuesto(), "ResumenImpuesto: codigoImpuesto requerido");
        requireNonBlank(r.getCodigoTarifaIVA(), "ResumenImpuesto: codigoTarifaIVA requerido");
        // No validamos totales aquí; el recálculo authoritative se encargará
      }
    }

    // ------ Medios de pago ------
    if (request.getMediosPago() == null || request.getMediosPago().isEmpty()) {
      throw new IllegalArgumentException("Debe incluir al menos un medio de pago");
    }
    BigDecimal sumaPagos = BigDecimal.ZERO;
    for (var mp : request.getMediosPago()) {
      requireNonBlank(mp.getMedioPago(), "Medio de pago: código requerido");
      if (!MEDIOS_VALIDOS.contains(mp.getMedioPago())) {
        throw new IllegalArgumentException("Medio de pago: código inválido: " + mp.getMedioPago());
      }
      requireNotNull(mp.getMonto(), "Medio de pago: monto requerido");
      if (mp.getMonto().compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Medio de pago: monto no puede ser negativo");
      }
      sumaPagos = sumaPagos.add(mp.getMonto());
    }

    // Cuadre de pagos vs. totalComprobante (lo que venga en request)
    requireNotNull(request.getTotalComprobante(), "totalComprobante es requerido");
    if (!casiIgual(sumaPagos, request.getTotalComprobante(), TOLERANCIA)) {
      throw new IllegalArgumentException(
          "La sumatoria de medios de pago (" + sumaPagos + ") no cuadra con totalComprobante (" +
              request.getTotalComprobante() + ")");
    }
  }

  private void recomputarTotalesAutoritativo(CrearFacturaRequest req) {
    // Esqueleto mínimo: sumar impuestos netos, otros cargos y cerrar totalComprobante
    BigDecimal totalImpuestoNeto = BigDecimal.ZERO;
    BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    if (req.getDetalles() != null) {
      for (var d : req.getDetalles()) {
        if (d.getImpuestos() != null) {
          for (var imp : d.getImpuestos()) {
            BigDecimal neto = defaultZero(imp.getImpuestoNeto());
            totalImpuestoNeto = totalImpuestoNeto.add(neto);
          }
        }
      }
    }
    if (req.getOtrosCargos() != null) {
      for (var oc : req.getOtrosCargos()) {
        totalOtrosCargos = totalOtrosCargos.add(defaultZero(oc.getMontoCargo()));
      }
    }

    // Aquí podrías recalcular ventaNeta según tus reglas; como mínimo, cierra totalComprobante:
    BigDecimal ventaNeta = defaultZero(req.getTotalVentaNeta()); // o recálcúlala si tienes la base

    BigDecimal totalComprobante = ventaNeta
        .add(totalImpuestoNeto)
        .add(totalOtrosCargos)
        .setScale(5, RoundingMode.HALF_UP);

    // Sobreescribe los campos del request (o del entity) con estos valores
    req.setTotalImpuesto(totalImpuestoNeto.setScale(5, RoundingMode.HALF_UP));
    req.setTotalOtrosCargos(totalOtrosCargos.setScale(5, RoundingMode.HALF_UP));
    req.setTotalComprobante(totalComprobante);
  }

  private static BigDecimal defaultZero(BigDecimal x) {
    return x == null ? BigDecimal.ZERO : x;
  }

// ====== Helpers (pegar también dentro de FacturaServiceImpl) ======

  private static void requireNonBlank(String val, String msg) {
    if (StringUtils.isBlank(val)) {
      throw new IllegalArgumentException(msg);
    }
  }

  private static void requireNotNull(Object val, String msg) {
    if (val == null) {
      throw new IllegalArgumentException(msg);
    }
  }

  private static void requireRegex(Pattern p, String val, String msg) {
    if (val == null || !p.matcher(val).matches()) {
      throw new IllegalArgumentException(msg);
    }
  }

  private static boolean casiIgual(BigDecimal a, BigDecimal b, BigDecimal tolerancia) {
    if (a == null || b == null) {
      return false;
    }
    return a.subtract(b).abs().compareTo(tolerancia) <= 0;
  }

  /**
   * Genera un reporte Excel de ventas para Hacienda en un rango de fechas
   *
   * @param empresaId ID de la empresa
   * @param sucursalId ID de la sucursal
   * @param fechaInicio Fecha inicio del rango (LocalDate)
   * @param fechaFin Fecha fin del rango (LocalDate)
   * @return Archivo Excel como byte array
   */
  @Override
  public byte[] generarReporteHacienda(Long empresaId, Long sucursalId, LocalDate fechaInicio, LocalDate fechaFin) {
    log.info("✅ Generando reporte de ventas para Hacienda - Empresa: {}, Sucursal: {}, Rango: {} a {}",
        empresaId, sucursalId, fechaInicio, fechaFin);

    // Convertir LocalDate a LocalDateTime (inicio y fin del día)
    LocalDateTime inicio = fechaInicio.atStartOfDay();
    LocalDateTime fin = fechaFin.atTime(23, 59, 59);

    // 1️⃣ Obtener facturas con detalles (SIN impuestos todavía)
    List<Factura> facturas = facturaRepository.findVentasParaReporte(empresaId, sucursalId, inicio, fin);
    log.info("📦 Se encontraron {} facturas en el rango", facturas.size());

    // 2️⃣ Si hay facturas, cargar impuestos en query separada
    if (!facturas.isEmpty()) {
      List<Long> facturaIds = facturas.stream()
          .map(Factura::getId)
          .collect(Collectors.toList());

      log.info("🔍 Cargando impuestos para {} facturas...", facturaIds.size());
      facturaRepository.cargarImpuestosDeDetalles(facturaIds);
      log.info("✅ Impuestos cargados en sesión Hibernate");
    }

    // 3️⃣ Transformar a DTO con datos YA cargados
    List<FacturaVentaReporteDTO> datos = facturas.stream()
        .map(this::toReporteVentaDTO)
        .collect(Collectors.toList());

    // 4️⃣ Llamar al generador de Excel
    return facturaVentaExcelService.generarExcel(datos, fechaInicio, fechaFin);
  }

  /**
   * Mapea una Factura a DTO para reporte
   * Calcula el signo según el tipo de documento
   * Calcula el desglose de impuestos por tipo
   */
  private FacturaVentaReporteDTO toReporteVentaDTO(Factura factura) {
    // Determinar signo según tipo
    int signo = calcularSignoVenta(factura.getTipoDocumento());

    // Calcular desglose de impuestos
    Map<String, BigDecimal> desglose = calcularDesglosImpuestosVenta(factura);

    return FacturaVentaReporteDTO.builder()
        .tipoDocumento(getTipoAbreviadoVenta(factura.getTipoDocumento()))
        .cedulaCliente(factura.getCliente().getNumeroIdentificacion() != null ? factura.getCliente().getNumeroIdentificacion() : "")
        .nombreCliente(factura.getCliente().getRazonSocial() != null ? factura.getCliente().getRazonSocial() : "Cliente General")
        .fechaEmision(LocalDateTime.parse(factura.getFechaEmision()))
        .clave(factura.getClave())
        .consecutivo(factura.getConsecutivo())
        // IVA POR TARIFA
        .iva0(desglose.getOrDefault("0", BigDecimal.ZERO))
        .iva1(desglose.getOrDefault("1", BigDecimal.ZERO))
        .iva2(desglose.getOrDefault("2", BigDecimal.ZERO))
        .iva4(desglose.getOrDefault("4", BigDecimal.ZERO))
        .iva8(desglose.getOrDefault("8", BigDecimal.ZERO))
        .iva13(desglose.getOrDefault("13", BigDecimal.ZERO))
        .otrosImpuestos(desglose.getOrDefault("OTROS", BigDecimal.ZERO))
        // SERVICIOS
        .totalServiciosGravados(factura.getTotalServiciosGravados() != null ? factura.getTotalServiciosGravados() : BigDecimal.ZERO)
        .totalServiciosExentos(factura.getTotalServiciosExentos() != null ? factura.getTotalServiciosExentos() : BigDecimal.ZERO)
        .totalServiciosNoSujetos(factura.getTotalServiciosNoSujetos() != null ? factura.getTotalServiciosNoSujetos() : BigDecimal.ZERO)
        // MERCANCÍAS
        .totalMercanciasGravadas(factura.getTotalMercanciasGravadas() != null ? factura.getTotalMercanciasGravadas() : BigDecimal.ZERO)
        .totalMercanciasExentas(factura.getTotalMercanciasExentas() != null ? factura.getTotalMercanciasExentas() : BigDecimal.ZERO)
        .totalMercanciasNoSujetas(factura.getTotalMercanciasNoSujetas() != null ? factura.getTotalMercanciasNoSujetas() : BigDecimal.ZERO)
        // TOTALES
        .totalVentaNeta(factura.getTotalVentaNeta())
        .totalImpuesto(factura.getTotalImpuesto())
        // OTROS TOTALES
        .totalDescuentos(factura.getTotalDescuentos() != null ? factura.getTotalDescuentos() : BigDecimal.ZERO)
        .totalOtrosCargos(factura.getTotalOtrosCargos() != null ? factura.getTotalOtrosCargos() : BigDecimal.ZERO)
        .totalIVADevuelto(factura.getTotalIVADevuelto() != null ? factura.getTotalIVADevuelto() : BigDecimal.ZERO)
        .totalExonerado(factura.getTotalExonerado() != null ? factura.getTotalExonerado() : BigDecimal.ZERO)
        .totalComprobante(factura.getTotalComprobante())
        .signo(signo)
        .build();
  }

  /**
   * Calcula el desglose completo de impuestos desde los detalles
   * - IVA por tarifa (0%, 1%, 2%, 4%, 8%, 13%)
   * - Otros impuestos (ISC, Combustibles, Tabaco, etc.)
   */
  private Map<String, BigDecimal> calcularDesglosImpuestosVenta(Factura factura) {
    Map<String, BigDecimal> desglose = new HashMap<>();

    // Inicializar todas las categorías en 0
    desglose.put("0", BigDecimal.ZERO);
    desglose.put("1", BigDecimal.ZERO);
    desglose.put("2", BigDecimal.ZERO);
    desglose.put("4", BigDecimal.ZERO);
    desglose.put("8", BigDecimal.ZERO);
    desglose.put("13", BigDecimal.ZERO);
    desglose.put("OTROS", BigDecimal.ZERO);

    // Recorrer detalles
    for (FacturaDetalle detalle : factura.getDetalles()) {
      // Recorrer impuestos del detalle
      for (FacturaDetalleImpuesto impuesto : detalle.getImpuestos()) {

        // Obtener el monto a usar (considerar exoneraciones)
        BigDecimal montoImpuesto = impuesto.getMontoExoneracion() != null
            && impuesto.getMontoExoneracion().compareTo(BigDecimal.ZERO) > 0
            ? impuesto.getImpuestoNeto() // Si hay exoneración, usar neto
            : impuesto.getMontoImpuesto(); // Si no hay exoneración, usar monto total

        // Clasificar según código de impuesto
        if ("01".equals(impuesto.getCodigoImpuesto())) {
          // IVA - clasificar por tarifa
          String tarifaKey = mapearCodigoTarifaVenta(impuesto.getCodigoTarifaIVA());
          BigDecimal montoActual = desglose.get(tarifaKey);
          desglose.put(tarifaKey, montoActual.add(montoImpuesto));

        } else {
          // Otros impuestos (ISC, Combustibles, Tabaco, etc.)
          BigDecimal otrosActual = desglose.get("OTROS");
          desglose.put("OTROS", otrosActual.add(montoImpuesto));
        }
      }
    }

    return desglose;
  }

  /**
   * Mapea el código de tarifa de Hacienda al porcentaje usado en el reporte
   */
  private String mapearCodigoTarifaVenta(String codigoTarifa) {
    if (codigoTarifa == null) return "13"; // Default

    return switch (codigoTarifa) {
      case "01", "05", "10", "11" -> "0";   // 0%
      case "02" -> "1";                      // 1%
      case "03" -> "2";                      // 2%
      case "04", "06" -> "4";                // 4%
      case "07" -> "8";                      // 8%
      case "08" -> "13";                     // 13% (Tarifa general)
      default -> {
        log.warn("⚠️ Código de tarifa desconocido: {}. Asignando a 13%", codigoTarifa);
        yield "13";
      }
    };
  }

  /**
   * Calcula el signo según el tipo de documento
   */
  private int calcularSignoVenta(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO -> 1;  // Se suma
      case NOTA_CREDITO -> -1;  // Se resta
      case NOTA_DEBITO -> 1;    // Se suma
      default -> 1;
    };
  }

  /**
   * Obtiene la abreviatura del tipo de documento
   */
  private String getTipoAbreviadoVenta(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA -> "FE";
      case TIQUETE_ELECTRONICO -> "TE";
      case NOTA_CREDITO -> "NC";
      case NOTA_DEBITO -> "ND";
      default -> tipo.name();
    };
  }
}