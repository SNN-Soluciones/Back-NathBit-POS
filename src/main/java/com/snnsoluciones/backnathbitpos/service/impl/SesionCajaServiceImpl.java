package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.repository.MovimientoCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SesionCajaServiceImpl implements SesionCajaService {

  private final SesionCajaRepository sesionCajaRepository;
  private final UsuarioRepository usuarioRepository;
  private final TerminalRepository terminalRepository;
  private final MovimientoCajaRepository movimientoCajaRepository;
  private final SecurityContextService securityContext;
  private final FacturaRepository facturaRepository;

  @Override
  public SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial) {
    log.info("Abriendo sesión de caja para usuario: {} en terminal: {}", usuarioId, terminalId);

    if (!puedeAbrirCaja()) {
      throw new RuntimeException("No tiene permisos para abrir caja");
    }

    // Validar que no tenga sesión abierta
    if (usuarioTieneSesionAbierta(usuarioId)) {
      throw new RuntimeException("El usuario ya tiene una sesión de caja abierta");
    }

    // Validar que la terminal no esté ocupada
    if (terminalTieneSesionAbierta(terminalId)) {
      throw new RuntimeException("La terminal ya tiene una sesión abierta");
    }

    // Obtener entidades
    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    Terminal terminal = terminalRepository.findById(terminalId)
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

    if (!terminal.getActiva()) {
      throw new RuntimeException("La terminal no está activa");
    }

    // Crear nueva sesión
    SesionCaja sesion = new SesionCaja();
    sesion.setUsuario(usuario);
    sesion.setTerminal(terminal);
    sesion.setFechaHoraApertura(LocalDateTime.now());
    sesion.setMontoInicial(montoInicial != null ? montoInicial : BigDecimal.ZERO);
    sesion.setEstado(EstadoSesion.ABIERTA);
    sesion.setObservacionesApertura("");

    // Inicializar contadores
    sesion.setTotalVentas(BigDecimal.ZERO);
    sesion.setTotalDevoluciones(BigDecimal.ZERO);
    sesion.setTotalEfectivo(BigDecimal.ZERO);
    sesion.setTotalTarjeta(BigDecimal.ZERO);
    sesion.setTotalTransferencia(BigDecimal.ZERO);
    sesion.setTotalOtros(BigDecimal.ZERO);
    sesion.setCantidadFacturas(0);
    sesion.setCantidadTiquetes(0);
    sesion.setCantidadNotasCredito(0);

    sesion = sesionCajaRepository.save(sesion);
    log.info("Sesión de caja abierta con ID: {}", sesion.getId());

    return sesion;
  }

  @Override
  public SesionCaja cerrarSesion(Long sesionId, BigDecimal montoCierre, String observaciones) {
    // Validar permisos
    if (!puedeCerrarCaja(sesionId)) {
      throw new RuntimeException("No tiene permisos para cerrar esta caja");
    }

    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

    // 🔥 AQUÍ USAS calcularMontoEsperado
    BigDecimal montoEsperado = calcularMontoEsperado(sesion);

    // Validar diferencia
    BigDecimal diferencia = montoCierre.subtract(montoEsperado);

    // Si hay diferencia significativa y no es supervisor
    if (diferencia.abs().compareTo(new BigDecimal("10000")) > 0
        && !securityContext.isSupervisor()) {
      throw new RuntimeException(String.format(
          "Diferencia de ₡%.2f requiere autorización de supervisor. Esperado: ₡%.2f, Cierre: ₡%.2f",
          diferencia, montoEsperado, montoCierre
      ));
    }

    // Actualizar sesión
    sesion.setFechaHoraCierre(LocalDateTime.now());
    sesion.setMontoCierre(montoCierre);
    sesion.setDiferenciaCierre(diferencia);
    sesion.setObservacionesCierre(observaciones);
    sesion.setEstado(EstadoSesion.CERRADA);

    return sesionCajaRepository.save(sesion);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SesionCaja> buscarSesionActiva(Long usuarioId) {
    return sesionCajaRepository.findSesionAbiertaByUsuarioId(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SesionCaja> buscarPorId(Long id) {
    return sesionCajaRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SesionCaja> listarPorFecha(LocalDate fecha) {
    LocalDateTime inicio = fecha.atStartOfDay();
    LocalDateTime fin = fecha.atTime(LocalTime.MAX);
    return sesionCajaRepository.findByFechaRango(inicio, fin);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SesionCaja> listarPorTerminalYFecha(Long terminalId, LocalDate fecha) {
    LocalDateTime inicio = fecha.atStartOfDay();
    LocalDateTime fin = fecha.atTime(LocalTime.MAX);
    return sesionCajaRepository.findByTerminalIdAndFechaRango(terminalId, inicio, fin);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean usuarioTieneSesionAbierta(Long usuarioId) {
    return sesionCajaRepository.existsSesionAbiertaByUsuarioId(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean terminalTieneSesionAbierta(Long terminalId) {
    return sesionCajaRepository.findSesionAbiertaByTerminalId(terminalId).isPresent();
  }

  @Override
  public Optional<SesionCaja> buscarSesionActivaPorTerminal(Long terminalId) {
    return sesionCajaRepository.findByTerminalIdAndEstado(terminalId, EstadoSesion.ABIERTA);
  }

  private boolean puedeVerResumen(SesionCaja sesion) {
    // Supervisores pueden ver todo
    if (securityContext.isSupervisor()) {
      return true;
    }
    // Cajero solo puede ver su propia sesión
    return sesion.getUsuario().getId().equals(securityContext.getCurrentUserId());
  }

  private boolean puedeAbrirCaja() {
    return securityContext.hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT");
  }

  private boolean puedeCerrarCaja(Long sesionId) {
    // JEFE_CAJAS y superiores pueden cerrar cualquier caja
    if (securityContext.isSupervisor()) {
      return true;
    }

    // CAJERO solo puede cerrar su propia caja
    SesionCaja sesion = sesionCajaRepository.findById(sesionId).orElse(null);
    return sesion != null &&
        sesion.getUsuario().getId().equals(securityContext.getCurrentUserId());
  }

  @Override
  public BigDecimal calcularMontoEsperado(SesionCaja sesion) {
    // Monto inicial
    BigDecimal montoEsperado = sesion.getMontoInicial();

    // + Ventas en efectivo
    montoEsperado = montoEsperado.add(sesion.getTotalEfectivo());

    // + Entradas adicionales (si agregaron más efectivo durante el día)
    BigDecimal entradas = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.ENTRADA_ADICIONAL);
    montoEsperado = montoEsperado.add(entradas);

    // - Salidas (vales y depósitos)
    BigDecimal salidas = movimientoCajaRepository.sumSalidasBySesionId(sesion.getId());
    montoEsperado = montoEsperado.subtract(salidas);

    return montoEsperado;
  }

  @Override
  public ResumenCajaDetalladoDTO obtenerResumenDetallado(Long sesionId) {
    log.info("Obteniendo resumen detallado de sesión: {}", sesionId);

    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

    // Validar permisos
    if (!puedeVerResumen(sesion)) {
      throw new RuntimeException("No tiene permisos para ver esta sesión");
    }

    // Construir resumen básico
    ResumenCajaDetalladoDTO resumen = new ResumenCajaDetalladoDTO();
    resumen.setSesionId(sesion.getId());
    resumen.setTerminal(sesion.getTerminal().getNombre());
    resumen.setCajero(
        sesion.getUsuario().getNombre().concat(" ").concat(sesion.getUsuario().getApellidos()));
    resumen.setFechaApertura(sesion.getFechaHoraApertura());
    resumen.setFechaCierre(sesion.getFechaHoraCierre());

    // Montos básicos
    resumen.setMontoInicial(sesion.getMontoInicial());
    resumen.setVentasEfectivo(sesion.getTotalEfectivo());
    resumen.setVentasTarjeta(sesion.getTotalTarjeta());
    resumen.setVentasTransferencia(sesion.getTotalTransferencia());
    resumen.setVentasOtros(sesion.getTotalOtros());

    // Movimientos
    resumen.setEntradasAdicionales(
        movimientoCajaRepository.sumBySesionIdAndTipo(sesionId,
            TipoMovimientoCaja.ENTRADA_ADICIONAL)
    );
    resumen.setVales(
        movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_VALE)
    );
    resumen.setDepositos(
        movimientoCajaRepository.sumBySesionIdAndTipo(sesionId, TipoMovimientoCaja.SALIDA_DEPOSITO)
    );

    // Monto esperado
    resumen.setMontoEsperado(calcularMontoEsperado(sesion));
    resumen.setMontoCierre(sesion.getMontoCierre());

    // NUEVO: Obtener facturas detalladas
    List<Factura> facturas = facturaRepository.findBySesionCajaId(sesionId);

    // Contadores y totales por tipo
    int cantFacturas = 0, cantTiquetes = 0, cantNC = 0;
    BigDecimal totalFacturas = BigDecimal.ZERO;
    BigDecimal totalTiquetes = BigDecimal.ZERO;
    BigDecimal totalNC = BigDecimal.ZERO;

    // Reiniciar totales por tipo de pago
    BigDecimal totalEfectivo = BigDecimal.ZERO;
    BigDecimal totalTarjeta = BigDecimal.ZERO;
    BigDecimal totalTransferencia = BigDecimal.ZERO;
    BigDecimal totalSinpe = BigDecimal.ZERO;

    // Lista de documentos para el detalle
    List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> documentos = new ArrayList<>();

    for (Factura f : facturas) {
      // Solo contar documentos válidos
      if (f.getEstado() == EstadoFactura.ANULADA || f.getEstado() == EstadoFactura.RECHAZADA) {
        continue;
      }

      // Construir DTO de documento
      ResumenCajaDetalladoDTO.DocumentoResumenDTO doc = ResumenCajaDetalladoDTO.DocumentoResumenDTO.builder()
          .id(f.getId())
          .consecutivo(f.getConsecutivo())
          .tipoDocumento(f.getTipoDocumento().getDescripcion())
          .clienteNombre(f.getNombreReceptor() != null ? f.getNombreReceptor() :
              (f.getCliente() != null ? f.getCliente().getRazonSocial() : "Cliente General"))
          .total(f.getTotalComprobante())
          .estado(f.getEstado().toString())
          .fechaEmision(LocalDateTime.now())
          .metodoPago(obtenerMetodosPago(f))
          .build();

      documentos.add(doc);

      // Sumar por tipo
      switch (f.getTipoDocumento()) {
        case FACTURA_ELECTRONICA:
        case FACTURA_INTERNA:
          cantFacturas++;
          totalFacturas = totalFacturas.add(f.getTotalComprobante());
          break;
        case TIQUETE_ELECTRONICO:
        case TIQUETE_INTERNO:
          cantTiquetes++;
          totalTiquetes = totalTiquetes.add(f.getTotalComprobante());
          break;
        case NOTA_CREDITO:
          cantNC++;
          totalNC = totalNC.add(f.getTotalComprobante());
          break;
      }

      if (f.getMediosPago() != null) {
        for (FacturaMedioPago medioPago : f.getMediosPago()) {
          switch (medioPago.getMedioPago()) {
            case EFECTIVO:
              totalEfectivo = totalEfectivo.add(medioPago.getMonto());
              break;
            case TARJETA:
              totalTarjeta = totalTarjeta.add(medioPago.getMonto());
              break;
            case CHEQUE:
              totalSinpe = totalSinpe.add(medioPago.getMonto());
              break;
            case TRANSFERENCIA:
              totalTransferencia = totalTransferencia.add(medioPago.getMonto());
              break;
          }
        }
      }
    }

    resumen.setVentasEfectivo(totalEfectivo);
    resumen.setVentasTarjeta(totalTarjeta);
    resumen.setVentasTransferencia(totalTransferencia);
    resumen.setVentasOtros(totalSinpe);

    // Establecer contadores
    resumen.setCantidadFacturas(cantFacturas);
    resumen.setCantidadTiquetes(cantTiquetes);
    resumen.setCantidadNotasCredito(cantNC);

    // Establecer totales
    resumen.setTotalFacturas(totalFacturas);
    resumen.setTotalTiquetes(totalTiquetes);
    resumen.setTotalNotasCredito(totalNC);

    // Establecer lista de documentos
    resumen.setDocumentos(documentos);

    List<MovimientoCaja> valesMovimientos = movimientoCajaRepository
        .findBySesionCajaIdAndTipoMovimiento(sesionId, TipoMovimientoCaja.SALIDA_VALE);

    List<ResumenCajaDetalladoDTO.ValeResumenDTO> valesDetalle = valesMovimientos.stream()
        .map(m -> ResumenCajaDetalladoDTO.ValeResumenDTO.builder()
            .id(m.getId())
            .monto(m.getMonto())
            .concepto(m.getConcepto())
            .autorizadoPor(m.getAutorizadoPorId() != null ?
                obtenerNombreUsuario(m.getAutorizadoPorId()) :
                "No especificado")
            .fecha(m.getFechaHora())
            .fecha(m.getFechaHora())
            .tipo(m.getTipoMovimiento().toString())
            .build())
        .collect(Collectors.toList());

    resumen.setValesDetalle(valesDetalle);

    // Lista de todos los movimientos
    resumen.setMovimientos(
        movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionId)
    );

    BigDecimal esperado = resumen.getMontoInicial().add(resumen.getVentasEfectivo())
        .subtract(resumen.getVales());
    resumen.setMontoEsperado(esperado);

    return resumen;
  }

  private String obtenerNombreUsuario(Long usuarioId) {
    return usuarioRepository.findById(usuarioId)
        .map(u -> u.getNombre() + " " + u.getApellidos())
        .orElse("Usuario no encontrado");
  }

  private String obtenerMetodosPago(Factura factura) {
    if (factura.getMediosPago() == null || factura.getMediosPago().isEmpty()) {
      return "No especificado";
    }

    return factura.getMediosPago().stream()
        .map(mp -> mp.getMedioPago().toString())
        .distinct()
        .collect(Collectors.joining(", "));
  }

  @Override
  public List<SesionCaja> buscarTodas() {
    log.info("Obteniendo todas las sesiones de caja");
    return sesionCajaRepository.findAll();
  }

  @Override
  public List<SesionCaja> buscarPorEstado(EstadoSesion estado) {
    log.info("Buscando sesiones por estado: {}", estado);
    return sesionCajaRepository.findByEstado(estado);
  }

  @Override
  public SesionCaja cerrarSesionAdmin(Long sesionId, BigDecimal montoCierre, String observaciones) {
    log.info("Cierre administrativo de sesión: {}", sesionId);

    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

    if (sesion.getEstado() != EstadoSesion.ABIERTA) {
      throw new RuntimeException("La sesión no está abierta");
    }

    // Actualizar totales desde las facturas
    actualizarTotalesDesdeFacturas(sesion);

    // Cerrar sesión sin validaciones de umbral
    sesion.setFechaHoraCierre(LocalDateTime.now());
    sesion.setMontoCierre(montoCierre);
    sesion.setObservacionesCierre(observaciones);
    sesion.setEstado(EstadoSesion.CERRADA);

    sesion = sesionCajaRepository.save(sesion);

    log.info("Sesión {} cerrada administrativamente con diferencia de {}",
        sesionId,
        montoCierre.subtract(calcularMontoEsperado(sesion)));

    return sesion;
  }

  // Agregar este método en SesionCajaServiceImpl

  private void actualizarTotalesDesdeFacturas(SesionCaja sesion) {
    log.info("Actualizando totales desde facturas para sesión: {}", sesion.getId());

    // Obtener todas las facturas de la sesión
    List<Factura> facturas = facturaRepository.findBySesionCajaId(sesion.getId());

    // Reiniciar contadores
    int cantFacturas = 0;
    int cantTiquetes = 0;
    int cantNC = 0;

    // Reiniciar totales por tipo de pago
    BigDecimal totalEfectivo = BigDecimal.ZERO;
    BigDecimal totalTarjeta = BigDecimal.ZERO;
    BigDecimal totalTransferencia = BigDecimal.ZERO;
    BigDecimal totalSinpe = BigDecimal.ZERO;
    BigDecimal totalOtros = BigDecimal.ZERO;

    // Totales generales
    BigDecimal totalVentas = BigDecimal.ZERO;
    BigDecimal totalDevoluciones = BigDecimal.ZERO;

    for (Factura factura : facturas) {
      // Solo contar documentos válidos (no anulados ni rechazados)
      if (factura.getEstado() == EstadoFactura.ANULADA ||
          factura.getEstado() == EstadoFactura.RECHAZADA) {
        continue;
      }

      BigDecimal totalFactura = factura.getTotalComprobante();

      // Contar por tipo de documento
      switch (factura.getTipoDocumento()) {
        case FACTURA_ELECTRONICA:
        case FACTURA_INTERNA:
          cantFacturas++;
          totalVentas = totalVentas.add(totalFactura);
          break;

        case TIQUETE_ELECTRONICO:
        case TIQUETE_INTERNO:
          cantTiquetes++;
          totalVentas = totalVentas.add(totalFactura);
          break;

        case NOTA_CREDITO:
          cantNC++;
          totalDevoluciones = totalDevoluciones.add(totalFactura);
          break;
      }

      // Sumar por tipo de pago
      if (factura.getMediosPago() != null) {
        for (FacturaMedioPago medioPago : factura.getMediosPago()) {
          switch (medioPago.getMedioPago()) {
            case EFECTIVO:
              totalEfectivo = totalEfectivo.add(medioPago.getMonto());
              break;
            case TARJETA:
              totalTarjeta = totalTarjeta.add(medioPago.getMonto());
              break;
            case CHEQUE:
              totalSinpe = totalSinpe.add(medioPago.getMonto());
              break;
            case TRANSFERENCIA:
              totalTransferencia = totalTransferencia.add(medioPago.getMonto());
              break;
          }
        }
      }
    }

    // Actualizar la sesión con los totales calculados
    sesion.setCantidadFacturas(cantFacturas);
    sesion.setCantidadTiquetes(cantTiquetes);
    sesion.setCantidadNotasCredito(cantNC);

    sesion.setTotalVentas(totalVentas);
    sesion.setTotalDevoluciones(totalDevoluciones);

    sesion.setTotalEfectivo(totalEfectivo);
    sesion.setTotalTarjeta(totalTarjeta);
    sesion.setTotalTransferencia(totalTransferencia);
    sesion.setTotalOtros(totalSinpe);

    log.info("Totales actualizados - Ventas: {}, Devoluciones: {}, Efectivo: {}",
        totalVentas, totalDevoluciones, totalEfectivo);
  }
}