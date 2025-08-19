package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Implementación del servicio de Factura con validación completa
 * "¡Piensa McFly, piensa!" - Doc Brown
 * Arquitectura La Jachuda 🚀
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
  private final FacturaJobService facturaJobService;

  @Override
  @Transactional
  public Factura crear(CrearFacturaRequest request) {
    log.info("Creando factura tipo: {}", request.getTipoDocumento());

    // 1. Validaciones de negocio básicas
    validarRequestBasico(request);

    // 2. Validación completa de totales (El checkeo del Doc)
    ValidacionTotalesResponse validacion = validarTotalesCompleto(request);
    if (!validacion.isEsValido()) {
      throw new IllegalArgumentException("Validación de totales falló: " + validacion.getMensaje());
    }

    // 3. Crear entidad factura
    Factura factura = new Factura();

    // Datos básicos
    factura.setTipoDocumento(request.getTipoDocumento());
    factura.setCondicionVenta(CondicionVenta.fromCodigo(request.getCondicionVenta()));
    factura.setPlazoCredito(request.getPlazoCredito());
    factura.setSituacion(SituacionDocumento.fromCodigo(request.getSituacionComprobante()));
    factura.setMoneda(request.getMoneda());
    factura.setTipoCambio(request.getTipoCambio());
    factura.setObservaciones(request.getObservaciones());

    // Cliente (opcional para TE)
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

    sesionCajaRepository.findById(request.getSesionCajaId())
        .ifPresent(factura::setSesionCaja);

    usuarioRepository.findById(request.getUsuarioId())
        .ifPresent(factura::setCajero);

    // Generar consecutivo
    String consecutivo = terminalService.generarNumeroConsecutivo(
        request.getTerminalId(),
        request.getTipoDocumento()
    );
    factura.setConsecutivo(consecutivo);

    // Fecha emisión
    ZonedDateTime fechaEmisionCR = ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
    factura.setFechaEmision(fechaEmisionCR.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    // Generar clave y código seguridad si es electrónica
    if (factura.esElectronica()) {
      factura.generarCodigoSeguridad();
      String clave = generarClave(factura);
      factura.setClave(clave);
      log.info("Clave generada: {} para consecutivo: {}", clave, consecutivo);
    }

    // 4. Asignar totales ya validados (confiamos en el frontend validado)
    asignarTotales(factura, request);

    // 5. Guardar descuento global si existe
    if (request.getDescuentoGlobalPorcentaje() != null) {
      factura.setDescuentoGlobalPorcentaje(request.getDescuentoGlobalPorcentaje());
      factura.setMontoDescuentoGlobal(request.getMontoDescuentoGlobal());
      factura.setMotivoDescuentoGlobal(request.getMotivoDescuentoGlobal());
    }

    // 6. Procesar detalles (sin calcular, solo guardar)
    procesarDetalles(factura, request.getDetalles());

    // 7. Procesar otros cargos
    procesarOtrosCargos(factura, request.getOtrosCargos());

    // 8. Procesar medios de pago
    procesarMediosPago(factura, request.getMediosPago());

    // 9. Procesar resumen de impuestos
    procesarResumenImpuestos(factura, request.getResumenImpuestos());

    // 10. Guardar factura
    factura.setEstado(EstadoFactura.GENERADA);
    Factura facturaGuardada = facturaRepository.save(factura);

    // 11. Si es electrónica, crear job para procesamiento asíncrono
    if (factura.esElectronica() && factura.getClave() != null) {
      facturaJobService.crearJob(facturaGuardada.getId(), facturaGuardada.getClave());
      log.info("Job creado para procesar factura: {}", facturaGuardada.getClave());
    }

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
   * VALIDACIÓN COMPLETA - El checkeo del Doc
   * Validamos TODOS los cálculos sin recalcular
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
          .add(request.getMontoDescuentoGlobal() != null ? request.getMontoDescuentoGlobal() : BigDecimal.ZERO);

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

      if (!sonIguales(totalVentaCalculado, request.getTotalVenta())) {
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
          .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleReq.getProductoId()));

      // Crear detalle
      FacturaDetalle detalle = new FacturaDetalle();
      detalle.setNumeroLinea(detalleReq.getNumeroLinea());
      detalle.setProducto(producto);
      detalle.setCantidad(detalleReq.getCantidad());
      detalle.setUnidadMedida(detalleReq.getUnidadMedida());
      detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());
      detalle.setCodigoCabys(detalleReq.getCodigoCabys() != null ?
          detalleReq.getCodigoCabys() : producto.getEmpresaCabys().getCodigoCabys().getCodigo());
      detalle.setDetalle(detalleReq.getDescripcionPersonalizada() != null ?
          detalleReq.getDescripcionPersonalizada() : producto.getNombre());

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
            impuesto.setTipoDocumentoExoneracion(exo.getTipoDocumentoExoneracion());
            impuesto.setNumeroDocumentoExoneracion(exo.getNumeroDocumentoExoneracion());
            impuesto.setNombreInstitucion(exo.getNombreInstitucion());
            impuesto.setFechaEmisionExoneracion(exo.getFechaEmisionExoneracion());
            impuesto.setTarifaExonerada(exo.getTarifaExonerada());
          }

          detalle.agregarImpuesto(impuesto);
        }
      }

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
      if ("04".equals(cargoReq.getTipoDocumentoOC()) && cargoReq.getTerceroTipoIdentificacion() != null) {
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
    clave.append(String.format("%02d%02d%04d",
        fecha.getDayOfMonth(),
        fecha.getMonthValue(),
        fecha.getYear()));

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
      throw new RuntimeException("La factura no puede ser anulada en estado: " + factura.getEstado());
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
      throw new RuntimeException("La factura no puede ser reenviada en estado: " + factura.getEstado());
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
  public ValidacionTotalesResponse validarTotales(ValidacionTotalesRequest request) {
    // Usar la misma validación completa
    return validarTotalesCompleto(request);
  }
}