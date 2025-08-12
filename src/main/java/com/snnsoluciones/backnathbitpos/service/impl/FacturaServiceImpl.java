package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.SituacionDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import com.snnsoluciones.backnathbitpos.util.GeneradorClaveUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FacturaServiceImpl implements FacturaService {

  private final FacturaRepository facturaRepository;
  private final TerminalService terminalService;
  private final FacturaJobService facturaJobService;

  @Override
  public Factura crear(Factura factura, List<FacturaDetalle> detalles,
      List<FacturaMedioPago> mediosPago) {
    log.info("Creando factura para cliente: {}", factura.getCliente().getId());

    // Validaciones básicas
    validarFactura(factura, detalles, mediosPago);

    // Generar consecutivo
    String consecutivo = terminalService.generarNumeroConsecutivo(
        factura.getTerminal().getId(),
        factura.getTipoDocumento()
    );
    factura.setConsecutivo(consecutivo);

    // Generar clave SOLO si es documento electrónico
    if (factura.esElectronica()) {
      String clave = generarClave(factura);
      factura.setClave(clave);
      log.info("Clave generada: {} para consecutivo: {}", clave, consecutivo);
    }

    // Establecer fecha emisión si no viene
    if (factura.getFechaEmision() == null) {
      factura.setFechaEmision(LocalDateTime.now());
    }

    // Agregar detalles y calcular totales
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal totalImpuestos = BigDecimal.ZERO;

    for (FacturaDetalle detalle : detalles) {
      factura.agregarDetalle(detalle);
      detalle.calcularTotales(); // Método @PrePersist se ejecutará
      subtotal = subtotal.add(detalle.getSubtotal());
      totalImpuestos = totalImpuestos.add(detalle.getImpuesto());
    }

    factura.setSubtotal(subtotal);
    factura.setImpuestos(totalImpuestos);
    factura.setTotal(subtotal.add(totalImpuestos).subtract(factura.getDescuentos()));

    // Agregar medios de pago
    for (FacturaMedioPago medioPago : mediosPago) {
      factura.agregarMedioPago(medioPago);
    }

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

  // Métodos privados de apoyo

  private void validarFactura(Factura factura, List<FacturaDetalle> detalles,
      List<FacturaMedioPago> mediosPago) {
    if (detalles == null || detalles.isEmpty()) {
      throw new IllegalArgumentException("La factura debe tener al menos un detalle");
    }

    if (mediosPago == null || mediosPago.isEmpty()) {
      throw new IllegalArgumentException("La factura debe tener al menos un medio de pago");
    }

    // Validar que la suma de medios de pago coincida con el total
    BigDecimal totalDetalles = detalles.stream()
        .map(d -> {
          d.calcularTotales();
          return d.getTotal();
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalMediosPago = mediosPago.stream()
        .map(FacturaMedioPago::getMonto)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Permitir pequeña diferencia por redondeos
    if (totalDetalles.subtract(totalMediosPago).abs().compareTo(new BigDecimal("1")) > 0) {
      throw new IllegalArgumentException(
          String.format("Total detalles (%.2f) no coincide con medios de pago (%.2f)",
              totalDetalles, totalMediosPago)
      );
    }
  }

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
        Integer.parseInt(factura.getSituacion().getCodigo()),
        factura.getId() != null ? factura.getId() : System.currentTimeMillis()
    );
  }
}