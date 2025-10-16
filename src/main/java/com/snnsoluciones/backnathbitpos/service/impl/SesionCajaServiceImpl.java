package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarSesionRequest;
import com.snnsoluciones.backnathbitpos.dto.sesion.OpcionesImpresionCierreDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.SesionCajaDTO;
import com.snnsoluciones.backnathbitpos.entity.CierreDatafono;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import com.snnsoluciones.backnathbitpos.entity.FacturaInternaMedioPago;
import com.snnsoluciones.backnathbitpos.entity.FacturaMedioPago;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimientoCaja;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CierreDatafonoRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaInternaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.repository.MovimientoCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.PlataformaDigitalConfigRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaDenominacionRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.EmailService;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import com.snnsoluciones.backnathbitpos.service.ThymeleafService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.xhtmlrenderer.pdf.ITextRenderer;

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
  private final FacturaInternaRepository facturaInternaRepository;
  private final SesionCajaDenominacionRepository sesionCajaDenominacionRepository;
  private final SucursalRepository sucursalRepository;
  private final PlataformaDigitalConfigRepository plataformaDigitalConfigRepository;
  private final CierreDatafonoRepository cierreDatafonoRepository;
  private final EmailService emailService;
  private final ThymeleafService thymeleafService;
  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String emailFrom;

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

  // SesionCajaServiceImpl.java (núcleo)
  @Transactional
  @Override
  public SesionCaja cerrarSesion(Long id, BigDecimal montoCierre, CerrarSesionRequest request, String observaciones,
      List<CerrarSesionRequest.DenominacionDTO> denominaciones) {

    SesionCaja sesion = sesionCajaRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

    // actualiza totales propios de tu negocio (ventas, devoluciones, etc.) antes de cerrar
    sesion.setMontoCierre(montoCierre);
    sesion.setObservacionesCierre(observaciones);
    sesion.setFechaHoraCierre(LocalDateTime.now());

    // 🆕 Validar y guardar nuevos campos
    BigDecimal montoRetirado = request.getMontoRetirado();
    BigDecimal fondoCaja = request.getFondoCaja();

// Validación: fondoCaja debe ser = montoCierre - montoRetirado
    BigDecimal fondoCalculado = montoCierre.subtract(montoRetirado);
    if (fondoCaja.compareTo(fondoCalculado) != 0) {
      throw new RuntimeException(String.format(
          "El fondo de caja no coincide. Esperado: ₡%.2f (₡%.2f - ₡%.2f), Recibido: ₡%.2f",
          fondoCalculado, montoCierre, montoRetirado, fondoCaja
      ));
    }

// Validación: montoRetirado no puede ser mayor que montoCierre
    if (montoRetirado.compareTo(montoCierre) > 0) {
      throw new RuntimeException(String.format(
          "No puedes retirar más dinero del que hay en caja. Cierre: ₡%.2f, Retiro: ₡%.2f",
          montoCierre, montoRetirado
      ));
    }

// Guardar los nuevos campos
    sesion.setMontoRetirado(montoRetirado);
    sesion.setFondoCaja(fondoCaja);

    // guarda desglose
    sesionCajaDenominacionRepository.deleteAll(
        sesionCajaDenominacionRepository.findBySesionCajaId(id)); // limpia previos si reintentan

    List<SesionCajaDenominacion> filas = denominaciones.stream()
        .map(d -> SesionCajaDenominacion.builder()
            .sesionCaja(sesion)
            .valor(d.getValor())
            .cantidad(d.getCantidad())
            .total(d.getValor().multiply(BigDecimal.valueOf(d.getCantidad())))
            .build()
        ).toList();
    sesionCajaDenominacionRepository.saveAll(filas);

    // puedes recalcular totalEfectivo con las denominaciones si aplica

    sesion.setTotalEfectivo(request.getTotalEfectivo());
    sesion.setTotalTarjeta(request.getTotalTarjeta());
    sesion.setTotalTransferencia(request.getTotalTransferencia());
    sesion.setTotalOtros(request.getTotalSinpe());

    if (request.getDatafonos() != null && !request.getDatafonos().isEmpty()) {
      // Limpiar datafonos previos (por si reintentan)
      cierreDatafonoRepository.deleteBySesionCajaId(id);

      // Guardar nuevos datafonos
      List<CierreDatafono> datafonos = request.getDatafonos().stream()
          .map(dto -> CierreDatafono.builder()
              .sesionCaja(sesion)
              .datafono(dto.getDatafono())
              .monto(dto.getMonto())
              .build())
          .toList();

      cierreDatafonoRepository.saveAll(datafonos);

      log.info("Guardados {} datafonos para sesión {}", datafonos.size(), id);
    }

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

  @Override
  public Page<SesionCajaDTO> listarPorSucursal(Long sucursalId, Pageable pageable) {
    log.info("Buscando sesiones de caja para sucursal ID: {}", sucursalId);

    // Verificar que la sucursal existe
    Sucursal sucursal = sucursalRepository.findById(sucursalId)
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con ID: " + sucursalId));

    // Obtener las sesiones de caja de la sucursal
    Page<SesionCaja> sesiones = sesionCajaRepository.findByTerminalSucursalId(sucursalId, pageable);

    log.info("Se encontraron {} sesiones de caja para la sucursal {}",
        sesiones.getTotalElements(), sucursal.getNombre());

    // Convertir a DTOs
    return sesiones.map(this::convertirADTO);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SesionCaja> buscarUltimaSesionCerrada(Long terminalId) {
    log.info("Buscando última sesión cerrada para terminal: {}", terminalId);

    Optional<SesionCaja> ultimaSesion = sesionCajaRepository
        .findTopByTerminalIdAndEstadoOrderByFechaHoraCierreDesc(
            terminalId,
            EstadoSesion.CERRADA
        );

    if (ultimaSesion.isPresent()) {
      log.info("Última sesión encontrada - ID: {}, Fondo Caja: {}",
          ultimaSesion.get().getId(),
          ultimaSesion.get().getFondoCaja());
    } else {
      log.info("No se encontró sesión cerrada previa para terminal: {}", terminalId);
    }

    return ultimaSesion;
  }

  private SesionCajaDTO convertirADTO(SesionCaja sesion) {
    SesionCajaDTO dto = SesionCajaDTO.builder()
        .id(sesion.getId())
        .usuarioId(sesion.getUsuario().getId())
        .usuarioNombre(sesion.getUsuario().getNombre())
        .usuarioEmail(sesion.getUsuario().getEmail())
        .sucursalId(sesion.getTerminal().getSucursal().getId())
        .sucursalNombre(sesion.getTerminal().getSucursal().getNombre())
        .fechaHoraApertura(sesion.getFechaHoraApertura())
        .fechaHoraCierre(sesion.getFechaHoraCierre())
        .montoInicial(sesion.getMontoInicial())
        .montoFinal(sesion.getMontoCierre())
        .estado(sesion.getEstado())
        .observaciones(sesion.getObservacionesCierre())
        .build();

    // Calcular total de ventas si la sesión está cerrada
    if (sesion.getEstado() == EstadoSesion.CERRADA
        && sesion.getMontoCierre() != null
        && sesion.getMontoInicial() != null) {
      BigDecimal totalVentas = sesion.getMontoCierre().subtract(sesion.getMontoInicial());
      dto.setTotalVentas(totalVentas);
    }

    return dto;
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
    log.debug("Calculando monto esperado para sesión: {}", sesion.getId());

    // 1. Monto inicial
    BigDecimal montoEsperado = sesion.getMontoInicial();
    log.debug("  Monto inicial: {}", montoEsperado);

    // 2. + Ventas en efectivo
    montoEsperado = montoEsperado.add(sesion.getTotalEfectivo());
    log.debug("  + Total efectivo (ventas): {} = {}", sesion.getTotalEfectivo(), montoEsperado);

    // 3. + Entradas adicionales de efectivo
    BigDecimal entradas = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.ENTRADA_ADICIONAL);
    if (entradas == null) entradas = BigDecimal.ZERO;
    montoEsperado = montoEsperado.add(entradas);
    log.debug("  + Entradas adicionales: {} = {}", entradas, montoEsperado);

    // 4. + Entradas de efectivo (NUEVO TIPO)
    BigDecimal entradasEfectivo = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.ENTRADA_EFECTIVO);
    if (entradasEfectivo == null) entradasEfectivo = BigDecimal.ZERO;
    montoEsperado = montoEsperado.add(entradasEfectivo);
    log.debug("  + Entradas efectivo: {} = {}", entradasEfectivo, montoEsperado);

    // 5. - Vales (LEGACY - mantener compatibilidad)
    BigDecimal vales = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.SALIDA_VALE);
    if (vales == null) vales = BigDecimal.ZERO;
    montoEsperado = montoEsperado.subtract(vales);
    log.debug("  - Vales: {} = {}", vales, montoEsperado);

    // 6. - Depósitos
    BigDecimal depositos = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.SALIDA_DEPOSITO);
    if (depositos == null) depositos = BigDecimal.ZERO;
    montoEsperado = montoEsperado.subtract(depositos);
    log.debug("  - Depósitos: {} = {}", depositos, montoEsperado);

    // 7. 🆕 - Arqueos
    BigDecimal arqueos = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.SALIDA_ARQUEO);
    if (arqueos == null) arqueos = BigDecimal.ZERO;
    montoEsperado = montoEsperado.subtract(arqueos);
    log.debug("  - Arqueos: {} = {}", arqueos, montoEsperado);

    // 8. 🆕 - Pagos a proveedores
    BigDecimal pagosProveedores = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.SALIDA_PAGO_PROVEEDOR);
    if (pagosProveedores == null) pagosProveedores = BigDecimal.ZERO;
    montoEsperado = montoEsperado.subtract(pagosProveedores);
    log.debug("  - Pagos proveedores: {} = {}", pagosProveedores, montoEsperado);

    // 9. 🆕 - Otros gastos
    BigDecimal otros = movimientoCajaRepository
        .sumBySesionIdAndTipo(sesion.getId(), TipoMovimientoCaja.SALIDA_OTROS);
    if (otros == null) otros = BigDecimal.ZERO;
    montoEsperado = montoEsperado.subtract(otros);
    log.debug("  - Otros gastos: {} = {}", otros, montoEsperado);

    log.info("Monto esperado final para sesión {}: {}", sesion.getId(), montoEsperado);

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

    int cantVentasInternas = 0;
    BigDecimal totalVentasInternas = BigDecimal.ZERO;

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

    // Contadores y totales por tipo
    int cantFacturas = 0, cantTiquetes = 0, cantNC = 0;
    BigDecimal totalFacturas = BigDecimal.ZERO;
    BigDecimal totalTiquetes = BigDecimal.ZERO;
    BigDecimal totalNC = BigDecimal.ZERO;

    // Totales por tipo de pago
    BigDecimal totalEfectivo = BigDecimal.ZERO;
    BigDecimal totalTarjeta = BigDecimal.ZERO;
    BigDecimal totalTransferencia = BigDecimal.ZERO;
    BigDecimal totalSinpe = BigDecimal.ZERO;

    // Lista de documentos para el detalle
    List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> documentos = new ArrayList<>();

    // PROCESAR FACTURAS ELECTRÓNICAS
    List<Factura> facturas = facturaRepository.findBySesionCajaId(sesionId);
    Map<Long, BigDecimal[]> ventasPorPlataforma = new HashMap<>();

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
          .fechaEmision(f.getFechaEmision()) // Usar fecha real de la factura
          .metodoPago(obtenerMetodosPago(f))
          .build();

      documentos.add(doc);

      // Sumar por tipo
      switch (f.getTipoDocumento()) {
        case FACTURA_ELECTRONICA:
          cantFacturas++;
          totalFacturas = totalFacturas.add(f.getTotalComprobante());
          break;
        case TIQUETE_ELECTRONICO:
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
            case SINPE_MOVIL:
              totalSinpe = totalSinpe.add(medioPago.getMonto());
              break;
            case TRANSFERENCIA:
              totalTransferencia = totalTransferencia.add(medioPago.getMonto());
              break;
          }

          if (medioPago.getPlataformaDigital() != null) {
            Long plataformaId = medioPago.getPlataformaDigital().getId();
            BigDecimal[] datos = ventasPorPlataforma.getOrDefault(
                plataformaId,
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}
            );
            datos[0] = datos[0].add(medioPago.getMonto());
            datos[1] = datos[1].add(BigDecimal.ONE);
            ventasPorPlataforma.put(plataformaId, datos);
          }
        }
      }
    }

    // NUEVO: PROCESAR FACTURAS INTERNAS
    List<FacturaInterna> facturasInternas = facturaInternaRepository.findBySesionCajaId(sesionId);

    for (FacturaInterna fi : facturasInternas) {
      // Solo contar documentos válidos
      if ("ANULADA".equals(fi.getEstado())) {
        continue;
      }

      // Obtener métodos de pago para mostrar
      String metodosPago = obtenerMetodosPagoInterna(fi);

      // Agregar al listado de documentos
      ResumenCajaDetalladoDTO.DocumentoResumenDTO doc = ResumenCajaDetalladoDTO.DocumentoResumenDTO.builder()
          .id(fi.getId())
          .consecutivo(fi.getNumero())
          .tipoDocumento("Tiquete Interno")
          .clienteNombre(fi.getNombreCliente() != null ? fi.getNombreCliente() : "Cliente General")
          .total(fi.getTotal())
          .estado(fi.getEstado())
          .fechaEmision(String.valueOf(fi.getFecha()))
          .metodoPago(metodosPago)
          .build();

      documentos.add(doc);

      // Sumar por tipo de pago
      if (fi.getMediosPago() != null && !fi.getMediosPago().isEmpty()) {
        for (FacturaInternaMedioPago mp : fi.getMediosPago()) {
          switch (mp.getTipo()) {
            case "EFECTIVO":
              totalEfectivo = totalEfectivo.add(mp.getMonto());
              break;
            case "TARJETA":
              totalTarjeta = totalTarjeta.add(mp.getMonto());
              break;
            case "TRANSFERENCIA":
              totalTransferencia = totalTransferencia.add(mp.getMonto());
              break;
            case "SINPE_MOVIL":
              totalSinpe = totalSinpe.add(mp.getMonto());
              break;
          }

          if (mp.getPlataformaDigital() != null) {
            Long plataformaId = mp.getPlataformaDigital().getId();
            BigDecimal[] datos = ventasPorPlataforma.getOrDefault(
                plataformaId,
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}
            );
            datos[0] = datos[0].add(mp.getMonto());
            datos[1] = datos[1].add(BigDecimal.ONE);
            ventasPorPlataforma.put(plataformaId, datos);
          }
        }
      }
      cantVentasInternas++;
      totalVentasInternas = totalVentasInternas.add(fi.getTotal());
    }

    List<ResumenCajaDetalladoDTO.VentaPlataformaDTO> ventasPlataformas = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal[]> entry : ventasPorPlataforma.entrySet()) {
      Long plataformaId = entry.getKey();
      BigDecimal[] datos = entry.getValue();

      plataformaDigitalConfigRepository.findById(plataformaId).ifPresent(plataforma -> {
        ResumenCajaDetalladoDTO.VentaPlataformaDTO ventaPlataforma =
            ResumenCajaDetalladoDTO.VentaPlataformaDTO.builder()
                .plataformaId(plataformaId)
                .plataformaNombre(plataforma.getNombre())
                .plataformaCodigo(plataforma.getCodigo())
                .totalVentas(datos[0])
                .cantidadTransacciones(datos[1].intValue())
                .build();
        ventasPlataformas.add(ventaPlataforma);
      });
    }

    // Asignar totales por tipo de pago
    resumen.setVentasEfectivo(totalEfectivo);
    resumen.setVentasTarjeta(totalTarjeta);
    resumen.setVentasTransferencia(totalTransferencia);
    resumen.setVentasOtros(totalSinpe);
    ventasPlataformas.sort((a, b) -> b.getTotalVentas().compareTo(a.getTotalVentas()));

// Luego donde asignas los valores al resumen:
    resumen.setVentasPlataformas(ventasPlataformas);

    // Establecer contadores
    resumen.setCantidadFacturas(cantFacturas);
    resumen.setCantidadTiquetes(cantTiquetes);
    resumen.setCantidadNotasCredito(cantNC);
    resumen.setCantidadVentasInternas(cantVentasInternas);

    // Establecer totales
    resumen.setTotalFacturas(totalFacturas);
    resumen.setTotalTiquetes(totalTiquetes);
    resumen.setTotalNotasCredito(totalNC);
    resumen.setTotalVentasInternas(totalVentasInternas);

    // Establecer lista de documentos
    resumen.setDocumentos(documentos);

    // Procesar vales
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
            .tipo(m.getTipoMovimiento().toString())
            .build())
        .collect(Collectors.toList());

    resumen.setValesDetalle(valesDetalle);

    // Lista de todos los movimientos
    resumen.setMovimientos(
        movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraDesc(sesionId)
    );

    sesion.setTotalEfectivo(totalEfectivo);

    BigDecimal esperado = calcularMontoEsperado(sesion);
    resumen.setMontoEsperado(esperado);

    resumen.setMontoCierre(sesion.getMontoCierre());

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

  /**
   * 🖨️ Genera PDF del cierre de caja con opciones personalizadas
   *
   * @param sesionId ID de la sesión cerrada
   * @param opciones Opciones de qué incluir en el PDF
   * @return Bytes del PDF generado
   */
  @Override
  @Transactional(readOnly = true)
  public byte[] generarPdfCierre(Long sesionId, OpcionesImpresionCierreDTO opciones) {
    log.info("📄 Generando PDF de cierre para sesión {} con opciones: {}", sesionId, opciones);

    // 1. Obtener sesión
    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

    // 2. Validar que esté cerrada
    if (sesion.getEstado() != EstadoSesion.CERRADA) {
      throw new IllegalStateException("La sesión debe estar cerrada para generar el PDF");
    }

    // 3. Obtener resumen detallado
    ResumenCajaDetalladoDTO resumen = obtenerResumenDetallado(sesionId);

    // 4. Generar HTML usando Thymeleaf
    Map<String, Object> variables = new HashMap<>();
    variables.put("sesion", sesion);
    variables.put("resumen", resumen);
    variables.put("opciones", opciones);
    variables.put("empresa", sesion.getTerminal().getSucursal().getEmpresa());
    variables.put("sucursal", sesion.getTerminal().getSucursal());
    variables.put("fechaGeneracion", LocalDateTime.now());

    String htmlContent = thymeleafService.processTemplate("cierre-caja", variables);

    // 5. Convertir HTML a PDF usando Flying Saucer o similar
    try {
      return convertirHtmlAPdf(htmlContent);
    } catch (Exception e) {
      log.error("Error convirtiendo HTML a PDF: {}", e.getMessage(), e);
      throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
    }
  }

  /**
   * 📧 Envía el cierre de caja por email
   *
   * @param sesionId ID de la sesión cerrada
   * @param opciones Opciones de envío (emails, qué incluir)
   */
  @Transactional
  @Override
  public void enviarCierrePorEmail(Long sesionId, OpcionesImpresionCierreDTO opciones) {
    log.info("📧 Enviando cierre por email para sesión {}", sesionId);

    // 1. Obtener sesión
    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

    // 2. Validar que esté cerrada
    if (sesion.getEstado() != EstadoSesion.CERRADA) {
      throw new IllegalStateException("La sesión debe estar cerrada para enviar email");
    }

    // 3. Obtener resumen
    ResumenCajaDetalladoDTO resumen = obtenerResumenDetallado(sesionId);

    // 4. Generar PDF
    byte[] pdfBytes = generarPdfCierre(sesionId, opciones);

    // 5. Obtener emails destino
    List<String> destinatarios = new ArrayList<>();

    // Email de la sucursal (automático)
    Sucursal sucursal = sesion.getTerminal().getSucursal();
    if (sucursal.getEmail() != null && !sucursal.getEmail().isBlank()) {
      destinatarios.add(sucursal.getEmail());
    } else if (sucursal.getEmpresa().getEmail() != null) {
      destinatarios.add(sucursal.getEmpresa().getEmail());
    }

    // Emails adicionales del formulario
    if (opciones.getCorreosAdicionales() != null) {
      destinatarios.addAll(opciones.getCorreosAdicionales());
    }

    // 6. Generar HTML del email
    String htmlEmail = generarHtmlEmailCierre(sesion, resumen, opciones);

    // 7. Enviar a cada destinatario
    for (String email : destinatarios) {
      try {
        enviarEmailCierreIndividual(email, sesion, htmlEmail, pdfBytes);
        log.info("✅ Email enviado a: {}", email);
      } catch (Exception e) {
        log.error("❌ Error enviando email a {}: {}", email, e.getMessage());
      }
    }
  }

  /**
   * 📨 Envía email individual con el cierre
   */
  private void enviarEmailCierreIndividual(
      String destinatario,
      SesionCaja sesion,
      String htmlContent,
      byte[] pdfBytes) {

    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // Configurar
      helper.setFrom(emailFrom);
      helper.setTo(destinatario);
      helper.setSubject(String.format(
          "Cierre de Caja - %s - %s",
          sesion.getTerminal().getNombre(),
          LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
      ));

      // HTML
      helper.setText(htmlContent, true);

      // Adjuntar PDF
      helper.addAttachment(
          String.format("cierre_caja_%s.pdf", sesion.getId()),
          new ByteArrayDataSource(pdfBytes, "application/pdf")
      );

      // Enviar
      javaMailSender.send(message);

    } catch (MessagingException e) {
      log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
      throw new RuntimeException("Error al enviar email", e);
    }
  }

  /**
   * 🎨 Genera HTML del email de cierre
   */
  private String generarHtmlEmailCierre(
      SesionCaja sesion,
      ResumenCajaDetalladoDTO resumen,
      OpcionesImpresionCierreDTO opciones) {

    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<style>");
    html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
    html.append(".container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
    html.append(".header { background: linear-gradient(135deg, #7C3AED 0%, #A855F7 100%); padding: 30px; text-align: center; color: white; }");
    html.append(".header h1 { margin: 0; font-size: 24px; }");
    html.append(".content { padding: 30px; }");
    html.append(".info-box { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #7C3AED; }");
    html.append(".info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #dee2e6; }");
    html.append(".info-label { font-weight: 600; color: #6c757d; }");
    html.append(".info-value { color: #212529; font-weight: 600; }");
    html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #6c757d; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class='container'>");

    // Header
    html.append("<div class='header'>");
    html.append("<h1>🔒 Cierre de Caja</h1>");
    html.append("<p style='margin: 10px 0 0; opacity: 0.9;'>").append(sesion.getTerminal().getNombre()).append("</p>");
    html.append("</div>");

    // Content
    html.append("<div class='content'>");
    html.append("<p>Se adjunta el cierre de caja con el detalle completo de la sesión.</p>");

    // Info Box
    html.append("<div class='info-box'>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Terminal:</span>");
    html.append("<span class='info-value'>").append(sesion.getTerminal().getNombre()).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Cajero:</span>");
    html.append("<span class='info-value'>").append(sesion.getUsuario().getNombre().concat(sesion.getUsuario().getApellidos())).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row'>");
    html.append("<span class='info-label'>Fecha:</span>");
    html.append("<span class='info-value'>").append(
        sesion.getFechaHoraApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    ).append("</span>");
    html.append("</div>");

    html.append("<div class='info-row' style='border-bottom: none; margin-top: 10px;'>");
    html.append("<span class='info-label'>Monto Esperado:</span>");
    html.append("<span class='info-value' style='color: #7C3AED; font-size: 18px;'>₡")
        .append(String.format("%,.0f", resumen.getMontoEsperado()))
        .append("</span>");
    html.append("</div>");

    html.append("<div class='info-row' style='border-bottom: none;'>");
    html.append("<span class='info-label'>Monto de Cierre:</span>");
    html.append("<span class='info-value' style='color: #7C3AED; font-size: 18px;'>₡")
        .append(String.format("%,.0f", sesion.getMontoCierre()))
        .append("</span>");
    html.append("</div>");

    html.append("</div>");

    html.append("<p>Adjunto encontrará el PDF con el detalle completo del cierre.</p>");
    html.append("</div>");

    // Footer
    html.append("<div class='footer'>");
    html.append("<p><strong>").append(sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial()).append("</strong></p>");
    html.append("<p>Este es un correo automático del sistema NathBit POS</p>");
    html.append("</div>");

    html.append("</div>");
    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  /**
   * 📄 Convierte HTML a PDF usando Flying Saucer
   */
  private byte[] convertirHtmlAPdf(String htmlContent) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
      // Usar Flying Saucer (ya debes tenerlo en dependencies)
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(htmlContent);
      renderer.layout();
      renderer.createPDF(outputStream);

      return outputStream.toByteArray();

    } finally {
      outputStream.close();
    }
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

  private String obtenerMetodosPagoInterna(FacturaInterna factura) {
    if (factura.getMediosPago() == null || factura.getMediosPago().isEmpty()) {
      return "No especificado";
    }

    return factura.getMediosPago().stream()
        .map(FacturaInternaMedioPago::getTipo)
        .distinct()
        .collect(Collectors.joining(", "));
  }
}