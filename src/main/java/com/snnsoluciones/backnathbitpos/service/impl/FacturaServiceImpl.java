package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.enums.mh.SituacionDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import com.snnsoluciones.backnathbitpos.util.GeneradorClaveUtil;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FacturaServiceImpl implements FacturaService {

  private final FacturaRepository facturaRepository;
  private final ProductoRepository productoRepository;
  private final ClienteRepository clienteRepository;
  private final TerminalService terminalService;
  private final FacturaJobService facturaJobService;
  private final OtroCargoRepository otroCargoRepository;
  private final FacturaDescuentoRepository facturaDescuentoRepository;
  private final SesionCajaRepository sesionCajaRepository;
  private final UsuarioRepository usuarioRepository;

  @Override
  public Factura crear(CrearFacturaRequest request) {
    log.info("Creando factura tipo: {} para cliente: {}",
        request.getTipoDocumento(), request.getClienteId());

    // Validaciones del request
    validarRequest(request);

    // Crear entidad Factura
    Factura factura = new Factura();
    factura.setTipoDocumento(request.getTipoDocumento());
    factura.setCondicionVenta(CondicionVenta.fromCodigo(request.getCondicionVenta()));
    factura.setPlazoCredito(request.getPlazoCredito());
    factura.setSituacion(SituacionDocumento.fromCodigo(request.getSituacionComprobante()));
    factura.setMoneda(request.getMoneda());
    factura.setTipoCambio(request.getTipoCambio());
    factura.setObservaciones(request.getObservaciones());

    // Asignar cliente si viene (opcional para TE)
    if (request.getClienteId() != null) {
      Cliente cliente = clienteRepository.findById(request.getClienteId())
          .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
      factura.setCliente(cliente);
    }

    // Terminal y sesión
    Terminal terminal = terminalService.buscarPorId(request.getTerminalId())
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
    factura.setTerminal(terminal);

    factura.setSucursal(terminal.getSucursal());
    sesionCajaRepository.findById(request.getSesionCajaId()).ifPresent(factura::setSesionCaja);
    usuarioRepository.findById(request.getUsuarioId()).ifPresent(factura::setCajero);

    // Generar consecutivo
    String consecutivo = terminalService.generarNumeroConsecutivo(
        request.getTerminalId(),
        request.getTipoDocumento()
    );
    factura.setConsecutivo(consecutivo);

    ZonedDateTime fechaEmisionCR = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
    factura.setFechaEmision(fechaEmisionCR.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    //generar cajero(usuario)

    // Generar clave y código seguridad si es electrónica
    if (factura.esElectronica()) {
      factura.generarCodigoSeguridad();
      String clave = generarClave(factura);
      factura.setClave(clave);
      log.info("Clave generada: {} para consecutivo: {}", clave, consecutivo);
    }

    // Procesar detalles con descuentos
    procesarDetalles(factura, request.getDetalles());

    // Aplicar descuento global si existe
    if (request.getDescuentoGlobalPorcentaje() != null) {
      factura.setDescuentoGlobalPorcentaje(request.getDescuentoGlobalPorcentaje());
      factura.aplicarDescuentoGlobal();
    } else if (request.getMontoDescuentoGlobal() != null) {
      factura.setMontoDescuentoGlobal(request.getMontoDescuentoGlobal());
    }
    factura.setMotivoDescuentoGlobal(request.getMotivoDescuentoGlobal());

    // Procesar otros cargos (servicio 10%)
    procesarOtrosCargos(factura, request.getOtrosCargos(), request.getDetalles());

    // Calcular totales finales
    calcularTotalesFinales(factura);

    // Procesar medios de pago
    procesarMediosPago(factura, request.getMediosPago());

    // Validar que totales coincidan
    validarTotales(factura);

    // Guardar factura
    factura.setEstado(EstadoFactura.GENERADA);
    Factura facturaGuardada = facturaRepository.save(factura);

    // Si es electrónica, crear job para procesamiento asíncrono
    if (factura.esElectronica() && factura.getClave() != null) {
      facturaJobService.crearJob(facturaGuardada.getId(), facturaGuardada.getClave());
      log.info("Job creado para procesar factura: {}", facturaGuardada.getClave());
    }

    return facturaGuardada;
  }

  /**
   * Validar el request completo
   */
  private void validarRequest(CrearFacturaRequest request) {
    // Validación general del request
    if (!request.isValid()) {
      throw new IllegalArgumentException("Request inválido");
    }

    // Validar tipo documento vs cliente
    if (request.getTipoDocumento() == TipoDocumento.FACTURA_ELECTRONICA
        && request.getClienteId() == null) {
      throw new IllegalArgumentException(
          "Factura Electrónica requiere cliente obligatorio"
      );
    }

    // Validar condición crédito
    if ("02".equals(request.getCondicionVenta())
        && (request.getPlazoCredito() == null || request.getPlazoCredito() <= 0)) {
      throw new IllegalArgumentException(
          "Condición crédito requiere plazo mayor a 0"
      );
    }

    // Validar moneda y tipo cambio
    if (!request.getMoneda().isMonedaLocal()
        && request.getTipoCambio().compareTo(BigDecimal.ONE) <= 0) {
      throw new IllegalArgumentException(
          "Moneda extranjera requiere tipo de cambio válido"
      );
    }
  }

  /**
   * Procesar detalles con sus descuentos
   */
  private void procesarDetalles(Factura factura, List<DetalleFacturaRequest> detallesRequest) {
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal totalImpuestos = BigDecimal.ZERO;
    int numeroLinea = 1;

    for (DetalleFacturaRequest detalleReq : detallesRequest) {
      Producto producto = productoRepository.findById(detalleReq.getProductoId())
          .orElseThrow(
              () -> new RuntimeException("Producto no encontrado: " + detalleReq.getProductoId()));

      FacturaDetalle detalle = new FacturaDetalle();
      detalle.setNumeroLinea(numeroLinea++);
      detalle.setProducto(producto);
      detalle.setCantidad(detalleReq.getCantidad());
      detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());
      detalle.setUnidadMedida(producto.getUnidadMedida().name());
      detalle.setCodigoCabys(producto.getEmpresaCabys().getCodigoCabys().getCodigo());

      // Descripción personalizada o del producto
      detalle.setDetalle(detalleReq.getDescripcionPersonalizada() != null
          ? detalleReq.getDescripcionPersonalizada()
          : producto.getNombre());

      // Código tarifa IVA (del request o del producto)
      if (detalleReq.getCodigoTarifaIVA() != null) {
        detalle.setCodigoTarifaIVA(detalleReq.getCodigoTarifaIVA());
      } else {
        // TODO: Obtener del producto o usar default
        detalle.setCodigoTarifaIVA("08"); // 13% por defecto
      }

      // Procesar descuentos de la línea
      procesarDescuentosLinea(detalle, detalleReq.getDescuentos());

      // Calcular totales de la línea
      detalle.calcularTotales();

      // Agregar a la factura
      factura.agregarDetalle(detalle);

      // Acumular totales
      subtotal = subtotal.add(detalle.getSubtotal());
      totalImpuestos = totalImpuestos.add(detalle.getImpuesto());
    }

    factura.setSubtotal(subtotal);
    factura.setImpuestos(totalImpuestos);
  }

  /**
   * Procesar descuentos de una línea
   */
  private void procesarDescuentosLinea(FacturaDetalle detalle,
      List<DescuentoRequest> descuentosReq) {
    if (descuentosReq == null || descuentosReq.isEmpty()) {
      return;
    }

    int orden = 1;
    for (DescuentoRequest descReq : descuentosReq) {
      FacturaDescuento descuento = new FacturaDescuento();
      descuento.setCodigoDescuento(descReq.getCodigoDescuento());
      descuento.setCodigoDescuentoOTRO(descReq.getCodigoDescuentoOTRO());
      descuento.setNaturalezaDescuento(descReq.getNaturalezaDescuento());
      descuento.setPorcentaje(descReq.getPorcentaje());
      descuento.setMontoDescuento(descReq.getMontoDescuento());
      descuento.setOrden(orden++);

      detalle.agregarDescuento(descuento);
    }
  }

  /**
   * Procesar otros cargos incluyendo servicio 10%
   */
  private void procesarOtrosCargos(Factura factura, List<OtroCargoRequest> otrosCargosReq,
      List<DetalleFacturaRequest> detalles) {
    // Primero, calcular si hay servicio 10% automático
    BigDecimal montoServicio = calcularMontoServicio(factura);

    if (montoServicio.compareTo(BigDecimal.ZERO) > 0) {
      // Crear otro cargo para servicio 10%
      OtroCargo servicioOC = new OtroCargo();
      servicioOC.setTipoDocumentoOC("06"); // Código para servicio 10%
      servicioOC.setNombreCargo("Impuesto de servicio 10%");
      servicioOC.setPorcentaje(new BigDecimal("10"));
      servicioOC.setMontoCargo(montoServicio);

      factura.agregarOtroCargo(servicioOC);
    }

    // Procesar otros cargos del request
    if (otrosCargosReq != null) {
      for (OtroCargoRequest ocReq : otrosCargosReq) {
        // Si ya agregamos servicio automático, saltar si viene duplicado
        if ("06".equals(ocReq.getTipoDocumentoOC())
            && montoServicio.compareTo(BigDecimal.ZERO) > 0) {
          continue;
        }

        OtroCargo otroCargo = new OtroCargo();
        otroCargo.setTipoDocumentoOC(ocReq.getTipoDocumentoOC());
        otroCargo.setTipoDocumentoOTROS(ocReq.getTipoDocumentoOTROS());
        otroCargo.setNombreCargo(ocReq.getNombreCargo());
        otroCargo.setPorcentaje(ocReq.getPorcentaje());
        otroCargo.setMontoCargo(ocReq.getMontoCargo());

        // Datos de tercero si aplica
        if ("04".equals(ocReq.getTipoDocumentoOC())) {
          otroCargo.setTerceroTipoIdentificacion(ocReq.getTerceroTipoIdentificacion());
          otroCargo.setTerceroNumeroIdentificacion(ocReq.getTerceroNumeroIdentificacion());
          otroCargo.setTerceroNombre(ocReq.getTerceroNombre());
        }

        factura.agregarOtroCargo(otroCargo);
      }
    }
  }

  /**
   * Calcular monto de servicio 10% basado en productos que aplican
   */
  private BigDecimal calcularMontoServicio(Factura factura) {
    BigDecimal montoServicio = BigDecimal.ZERO;

    for (FacturaDetalle detalle : factura.getDetalles()) {
      if (detalle.aplicaServicio()) {
        // Servicio se calcula sobre el subtotal (después de descuentos)
        BigDecimal servicioLinea = detalle.getSubtotal()
            .multiply(new BigDecimal("0.10"))
            .setScale(5, RoundingMode.HALF_UP);
        montoServicio = montoServicio.add(servicioLinea);
      }
    }

    return montoServicio;
  }

  /**
   * Calcular totales finales de la factura
   */
  private void calcularTotalesFinales(Factura factura) {
    // Total otros cargos
    factura.setTotalOtrosCargos(factura.calcularTotalOtrosCargos());

    // Total descuentos (líneas + global)
    factura.setTotalDescuentos(factura.calcularTotalDescuentos());

    // Calcular total final
    BigDecimal totalFinal = factura.getSubtotal()
        .add(factura.getTotalOtrosCargos())
        .add(factura.getImpuestos())
        .subtract(factura.getTotalDescuentos());

    factura.setTotal(totalFinal);

    // Convertir a moneda local si aplica
    factura.calcularTotalMonedaLocal();
  }

  /**
   * Procesar medios de pago
   */
  /**
   * Procesar medios de pago
   */
  private void procesarMediosPago(Factura factura, List<MedioPagoRequest> mediosPagoReq) {
    for (MedioPagoRequest mpReq : mediosPagoReq) {
      FacturaMedioPago medioPago = new FacturaMedioPago();

      // Usar fromCodigo en lugar de valueOf
      medioPago.setMedioPago(MedioPago.fromCodigo(mpReq.getMedioPago()));

      medioPago.setMonto(mpReq.getMonto());
      medioPago.setReferencia(mpReq.getReferencia());
      medioPago.setBanco(mpReq.getBanco());

      factura.agregarMedioPago(medioPago);
    }
  }

  /**
   * Validar que los totales coincidan
   */
  private void validarTotales(Factura factura) {
    BigDecimal totalCalculado = factura.getTotal();
    BigDecimal totalMediosPago = factura.getTotalMediosPago();

    // Permitir pequeña diferencia por redondeos (1 colón)
    if (totalCalculado.subtract(totalMediosPago).abs().compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException(
          String.format("Total factura (%.2f) no coincide con medios de pago (%.2f)",
              totalCalculado, totalMediosPago)
      );
    }
  }

  /**
   * Generar clave de 50 dígitos según Hacienda
   */
  private String generarClave(Factura factura) {
    Empresa empresa = factura.getSucursal().getEmpresa();

    // Convertir tipo identificación
    int tipoIdentificacion = switch (empresa.getTipoIdentificacion()) {
      case CEDULA_FISICA -> 1;
      case CEDULA_JURIDICA -> 2;
      case DIMEX -> 3;
      case NITE -> 4;
      case EXTRANJERO -> 5;
    };

    // Generar clave
    return GeneradorClaveUtil.generarClave(
        factura.getFechaEmision(),
        tipoIdentificacion,
        empresa.getIdentificacion(),
        factura.getConsecutivo(),
        Integer.parseInt(factura.getSituacionComprobante()),
        Long.parseLong(factura.getCodigoSeguridad())
    );
  }

  // Implementar otros métodos del servicio...

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
    LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    return facturaRepository.findBySesionCajaHoy(sesionCajaId, inicioDia);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Factura> listarFacturasConError(Long sucursalId) {
    return facturaRepository.findFacturasConError(sucursalId);
  }

  @Override
  public Factura anular(Long facturaId, String motivo) {
    Factura factura = facturaRepository.findById(facturaId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    if (!factura.getEstado().puedeAnularse()) {
      throw new RuntimeException(
          "La factura no puede ser anulada en estado: " + factura.getEstado());
    }

    factura.setEstado(EstadoFactura.ANULADA);
    // TODO: En fase 2, generar nota de crédito si es necesario

    log.info("Factura {} anulada. Motivo: {}", factura.getClave(), motivo);
    return facturaRepository.save(factura);
  }

  @Override
  public void reenviar(Long facturaId) {
    Factura factura = facturaRepository.findById(facturaId)
        .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

    if (!factura.getEstado().puedeReprocesarse()) {
      throw new RuntimeException(
          "La factura no puede ser reenviada en estado: " + factura.getEstado());
    }

    // Crear nuevo job para reintento
    if (factura.getClave() != null) {
      facturaJobService.crearJob(factura.getId(), factura.getClave());
      factura.setEstado(EstadoFactura.PROCESANDO);
      facturaRepository.save(factura);
      log.info("Factura {} marcada para reenvío", factura.getClave());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Factura> buscarPorId(Long id) {
    return facturaRepository.findById(id);
  }

  @Override
  public ValidacionTotalesResponse validarTotales(ValidacionTotalesRequest request) {
    try {
      // Simular el cálculo sin persistir
      BigDecimal subtotal = BigDecimal.ZERO;
      BigDecimal totalDescuentosLineas = BigDecimal.ZERO;
      BigDecimal totalImpuestos = BigDecimal.ZERO;
      BigDecimal totalServicio = BigDecimal.ZERO;

      // Calcular por cada línea
      for (DetalleFacturaRequest detalle : request.getDetalles()) {
        // Buscar producto para obtener información
        Producto producto = productoRepository.findById(detalle.getProductoId())
            .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProductoId()));

        // Monto línea
        BigDecimal montoLinea = detalle.getCantidad().multiply(detalle.getPrecioUnitario());

        // Aplicar descuentos en cascada
        BigDecimal montoConDescuentos = montoLinea;
        BigDecimal descuentosLinea = BigDecimal.ZERO;

        for (DescuentoRequest desc : detalle.getDescuentos()) {
          BigDecimal montoDesc;
          if (desc.getMontoDescuento() != null) {
            montoDesc = desc.getMontoDescuento();
          } else if (desc.getPorcentaje() != null) {
            montoDesc = montoConDescuentos
                .multiply(desc.getPorcentaje())
                .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);
          } else {
            continue;
          }
          montoConDescuentos = montoConDescuentos.subtract(montoDesc);
          descuentosLinea = descuentosLinea.add(montoDesc);
        }

        // Calcular servicio si aplica
        if (Boolean.TRUE.equals(producto.getAplicaServicio())) {
          BigDecimal servicioLinea = montoConDescuentos
              .multiply(new BigDecimal("0.10"))
              .setScale(5, RoundingMode.HALF_UP);
          totalServicio = totalServicio.add(servicioLinea);
        }

        // Calcular IVA
        String codigoTarifa = detalle.getCodigoTarifaIVA() != null
            ? detalle.getCodigoTarifaIVA()
            : "08"; // Default 13%

        BigDecimal tasaImpuesto = obtenerTasaImpuesto(codigoTarifa);
        BigDecimal impuestoLinea = montoConDescuentos
            .multiply(tasaImpuesto)
            .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);

        // Acumular totales
        subtotal = subtotal.add(montoConDescuentos);
        totalDescuentosLineas = totalDescuentosLineas.add(descuentosLinea);
        totalImpuestos = totalImpuestos.add(impuestoLinea);
      }

      // Aplicar descuento global
      BigDecimal montoDescuentoGlobal = BigDecimal.ZERO;
      if (request.getDescuentoGlobalPorcentaje() != null) {
        montoDescuentoGlobal = subtotal
            .multiply(request.getDescuentoGlobalPorcentaje())
            .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);
      } else if (request.getMontoDescuentoGlobal() != null) {
        montoDescuentoGlobal = request.getMontoDescuentoGlobal();
      }

      // Procesar otros cargos
      BigDecimal totalOtrosCargos = totalServicio; // Servicio ya calculado

      if (request.getOtrosCargos() != null) {
        for (OtroCargoRequest oc : request.getOtrosCargos()) {
          // Si ya incluimos servicio automático, no duplicar
          if (!"06".equals(oc.getTipoDocumentoOC()) || totalServicio.equals(BigDecimal.ZERO)) {
            totalOtrosCargos = totalOtrosCargos.add(oc.getMontoCargo());
          }
        }
      }

      // Calcular totales finales
      BigDecimal totalDescuentos = totalDescuentosLineas.add(montoDescuentoGlobal);
      BigDecimal totalCalculado = subtotal
          .add(totalOtrosCargos)
          .add(totalImpuestos)
          .subtract(montoDescuentoGlobal);

      // Validar contra medios de pago
      BigDecimal totalMediosPago = request.getMediosPago().stream()
          .map(MedioPagoRequest::getMonto)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      boolean esValido = totalCalculado.subtract(totalMediosPago).abs()
          .compareTo(BigDecimal.ONE) <= 0;

      String mensaje = esValido
          ? "Totales válidos"
          : String.format("Total calculado (%.2f) no coincide con medios de pago (%.2f)",
              totalCalculado, totalMediosPago);

      return ValidacionTotalesResponse.builder()
          .esValido(esValido)
          .mensaje(mensaje)
          .subtotalCalculado(subtotal)
          .totalDescuentosCalculado(totalDescuentos)
          .totalOtrosCargosCalculado(totalOtrosCargos)
          .totalImpuestosCalculado(totalImpuestos)
          .totalCalculado(totalCalculado)
          .desglosePorLinea(construirDesglosePorLinea(request))
          .build();

    } catch (Exception e) {
      log.error("Error al validar totales", e);
      return ValidacionTotalesResponse.builder()
          .esValido(false)
          .mensaje("Error al validar: " + e.getMessage())
          .build();
    }
  }

  /**
   * Construir desglose detallado por línea para validación
   */
  private List<DesglosePorLinea> construirDesglosePorLinea(ValidacionTotalesRequest request) {
    List<DesglosePorLinea> desglose = new ArrayList<>();

    for (int i = 0; i < request.getDetalles().size(); i++) {
      DetalleFacturaRequest detalle = request.getDetalles().get(i);

      try {
        Producto producto = productoRepository.findById(detalle.getProductoId())
            .orElse(null);

        if (producto != null) {
          BigDecimal montoLinea = detalle.getCantidad().multiply(detalle.getPrecioUnitario());
          BigDecimal subtotalConDescuentos = detalle.calcularSubtotalConDescuentos();

          desglose.add(DesglosePorLinea.builder()
              .numeroLinea(i + 1)
              .productoNombre(producto.getNombre())
              .cantidad(detalle.getCantidad())
              .precioUnitario(detalle.getPrecioUnitario())
              .montoTotal(montoLinea)
              .descuentos(montoLinea.subtract(subtotalConDescuentos))
              .subtotal(subtotalConDescuentos)
              .aplicaServicio(producto.getAplicaServicio())
              .build());
        }
      } catch (Exception e) {
        log.error("Error calculando línea {}", i, e);
      }
    }

    return desglose;
  }

  /**
   * Helper para obtener tasa de impuesto
   */
  private BigDecimal obtenerTasaImpuesto(String codigoTarifaIVA) {
    return switch (codigoTarifaIVA) {
      case "01", "05", "10", "11" -> BigDecimal.ZERO;
      case "09" -> new BigDecimal("0.5");
      case "02" -> BigDecimal.ONE;
      case "03" -> new BigDecimal("2");
      case "04", "06" -> new BigDecimal("4");
      case "07" -> new BigDecimal("8");
      case "08" -> new BigDecimal("13");
      default -> new BigDecimal("13");
    };
  }
}