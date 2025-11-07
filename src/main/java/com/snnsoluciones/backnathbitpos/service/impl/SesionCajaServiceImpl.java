package com.snnsoluciones.backnathbitpos.service.impl;

import com.lowagie.text.pdf.BaseFont;
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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
import org.xhtmlrenderer.pdf.ITextFontResolver;
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
          String tipo = mp.getTipo();

          if ("EFECTIVO".equals(tipo)) {
            totalEfectivo = totalEfectivo.add(mp.getMonto());
          } else if ("TARJETA".equals(tipo)) {
            totalTarjeta = totalTarjeta.add(mp.getMonto());
          } else if ("TRANSFERENCIA".equals(tipo)) {
            totalTransferencia = totalTransferencia.add(mp.getMonto());
          } else if ("SINPE".equals(tipo) || "SINPE_MOVIL".equals(tipo)) {
            totalSinpe = totalSinpe.add(mp.getMonto());
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
      return "";
    }

    Set<String> medios = new HashSet<>();

    for (FacturaMedioPago mp : factura.getMediosPago()) {
      switch (mp.getMedioPago()) {
        case EFECTIVO:
          medios.add("E");
          break;
        case TARJETA:
          medios.add("TC");
          break;
        case SINPE_MOVIL:
          medios.add("S");
          break;
        case TRANSFERENCIA:
          medios.add("TB");
          break;
      }
    }

    return String.join(",", medios);
  }

  private String obtenerMetodosPagoInterna(FacturaInterna factura) {
    if (factura.getMediosPago() == null || factura.getMediosPago().isEmpty()) {
      return "";
    }

    Set<String> medios = new HashSet<>();

    for (FacturaInternaMedioPago mp : factura.getMediosPago()) {
      String tipo = mp.getTipo().toUpperCase();

      if (tipo.equals("EFECTIVO")) {
        medios.add("E");
      } else if (tipo.equals("TARJETA")) {
        medios.add("TC");
      } else if (tipo.equals("SINPE") || tipo.equals("SINPE_MOVIL")) {  // ← Acepta ambos
        medios.add("S");
      } else if (tipo.equals("TRANSFERENCIA")) {
        medios.add("TB");
      }
    }

    return String.join(",", medios);
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

  private static File cpToTempFile(String classpath, String prefix) throws IOException {
    ClassPathResource res = new ClassPathResource(classpath);
    if (!res.exists()) {
      throw new FileNotFoundException("No existe en classpath: " + classpath);
    }
    File tmp = File.createTempFile(prefix, ".ttf");
    tmp.deleteOnExit();
    try (InputStream in = res.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
      in.transferTo(out);
    }
    return tmp;
  }

  private byte[] convertirHtmlAPdf(String htmlContent) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();

      // Base URL para recursos relativos (si los tienes en /templates o /static)
      // Usa setDocumentFromString(html, baseUrl) para que <img src="..."> relativos funcionen
      String baseUrl = this.getClass().getResource("/templates/") != null
          ? this.getClass().getResource("/templates/").toString()
          : this.getClass().getResource("/static/") != null
              ? this.getClass().getResource("/static/").toString()
              : null;

      // Registrar fuentes desde classpath -> temp file (funciona dentro de JAR)
      ITextFontResolver fontResolver = renderer.getFontResolver();
      fontResolver.addFont(cpToTempFile("fonts/NotoSans-Regular.ttf",     "noto-regular").getAbsolutePath(),
          BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      fontResolver.addFont(cpToTempFile("fonts/NotoSans-Bold.ttf",        "noto-bold").getAbsolutePath(),
          BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      fontResolver.addFont(cpToTempFile("fonts/NotoSans-Italic.ttf",      "noto-italic").getAbsolutePath(),
          BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      fontResolver.addFont(cpToTempFile("fonts/NotoSans-BoldItalic.ttf",  "noto-bolditalic").getAbsolutePath(),
          BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

      if (baseUrl != null) {
        renderer.setDocumentFromString(htmlContent, baseUrl);
      } else {
        renderer.setDocumentFromString(htmlContent);
      }

      renderer.layout();
      renderer.createPDF(outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("❌ Error en conversión HTML a PDF: {}", e.getMessage(), e);
      throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
    }
  }


  @Override
  public String generarHtmlCierre(Long sesionId, OpcionesImpresionCierreDTO opciones) {
    log.info("📄 Generando HTML de cierre para sesión {}", sesionId);

    // 1. Obtener sesión
    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

    // 2. Obtener resumen detallado
    ResumenCajaDetalladoDTO resumen = obtenerResumenDetallado(sesionId);

    // 3. Calcular monto esperado
    BigDecimal montoEsperado = calcularMontoEsperado(sesion);

    // 4. Formatear números
    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // 5. Construir HTML
    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<title>Cierre de Caja #").append(sesionId).append("</title>");
    html.append("<style>");
    html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
    html.append("body { font-family: 'Courier New', monospace; font-size: 12px; padding: 10px; }");
    html.append(".header { text-align: center; margin-bottom: 15px; }");
    html.append(".title { font-size: 16px; font-weight: bold; margin: 5px 0; }");
    html.append(".divider { border-top: 1px dashed #000; margin: 8px 0; }");
    html.append(".double-divider { border-top: 2px solid #000; margin: 10px 0; }");
    html.append(".section { margin: 10px 0; }");
    html.append(".section-title { font-weight: bold; margin: 8px 0 5px 0; }");
    html.append(".row { display: flex; justify-content: space-between; margin: 3px 0; }");
    html.append(".row-left { text-align: left; }");
    html.append(".row-right { text-align: right; }");
    html.append(".total-row { font-weight: bold; }");
    html.append(".detail-line { font-size: 11px; margin: 2px 0; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");

    // HEADER
    html.append("<div class='header'>");
    html.append("<div class='title'>═══════════════════════════</div>");
    html.append("<div class='title'>CIERRE DE CAJA</div>");
    html.append("<div class='title'>═══════════════════════════</div>");
    html.append("</div>");

    // CÁLCULO EFECTIVO EN CAJA
    html.append("<div class='section'>");
    html.append("<div class='section-title'>📊 EFECTIVO EN CAJA</div>");
    html.append("<div class='divider'></div>");

    html.append("<div class='row'>");
    html.append("<span>Monto Apertura:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getMontoInicial())).append("</span>");
    html.append("</div>");

    html.append("<div class='row'>");
    html.append("<span>+ Ventas Efectivo:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getVentasEfectivo() != null ? resumen.getVentasEfectivo() : BigDecimal.ZERO)).append("</span>");
    html.append("</div>");

// Calcular totales de movimientos
    List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraAsc(sesion.getId());
    BigDecimal totalEntradas = BigDecimal.ZERO;
    BigDecimal totalSalidas = BigDecimal.ZERO;

    for (MovimientoCaja mov : movimientos) {
      if (mov.getTipoMovimiento().esEntrada()) {
        totalEntradas = totalEntradas.add(mov.getMonto());
      } else if (mov.getTipoMovimiento().esSalida()) {
        totalSalidas = totalSalidas.add(mov.getMonto());
      }
    }

    html.append("<div class='row'>");
    html.append("<span>+ Movimientos Entrada:</span>");
    html.append("<span>").append(currencyFormat.format(totalEntradas)).append("</span>");
    html.append("</div>");

    html.append("<div class='row'>");
    html.append("<span>- Movimientos Salida:</span>");
    html.append("<span>").append(currencyFormat.format(totalSalidas)).append("</span>");
    html.append("</div>");

    html.append("<div class='double-divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>= Total Esperado:</span>");
    html.append("<span>").append(currencyFormat.format(montoEsperado)).append("</span>");
    html.append("</div>");
    html.append("</div>");

    // RESUMEN VENTAS
    html.append("<div class='section'>");
    html.append("<div class='section-title'>📈 TOTAL DE VENTAS</div>");
    html.append("<div class='divider'></div>");

    html.append("<div class='row'>");
    html.append("<span>Efectivo:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getVentasEfectivo() != null ? resumen.getVentasEfectivo() : BigDecimal.ZERO)).append("</span>");
    html.append("</div>");

    html.append("<div class='row'>");
    html.append("<span>Tarjeta:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getVentasTarjeta() != null ? resumen.getVentasTarjeta() : BigDecimal.ZERO)).append("</span>");
    html.append("</div>");

    html.append("<div class='row'>");
    html.append("<span>SINPE:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getVentasOtros() != null ? resumen.getVentasOtros() : BigDecimal.ZERO)).append("</span>");
    html.append("</div>");

    html.append("<div class='row'>");
    html.append("<span>Transferencia:</span>");
    html.append("<span>").append(currencyFormat.format(resumen.getVentasTransferencia() != null ? resumen.getVentasTransferencia() : BigDecimal.ZERO)).append("</span>");
    html.append("</div>");

    BigDecimal totalPlataformas = BigDecimal.ZERO;
    if (resumen.getVentasPlataformas() != null) {
      for (ResumenCajaDetalladoDTO.VentaPlataformaDTO plat : resumen.getVentasPlataformas()) {
        totalPlataformas = totalPlataformas.add(plat.getTotalVentas());
      }
    }

    html.append("<div class='row'>");
    html.append("<span>Plataformas:</span>");
    html.append("<span>").append(currencyFormat.format(totalPlataformas)).append("</span>");
    html.append("</div>");

    BigDecimal totalVentas = BigDecimal.ZERO;
    totalVentas = totalVentas.add(resumen.getVentasEfectivo() != null ? resumen.getVentasEfectivo() : BigDecimal.ZERO);
    totalVentas = totalVentas.add(resumen.getVentasTarjeta() != null ? resumen.getVentasTarjeta() : BigDecimal.ZERO);
    totalVentas = totalVentas.add(resumen.getVentasOtros() != null ? resumen.getVentasOtros() : BigDecimal.ZERO);
    totalVentas = totalVentas.add(resumen.getVentasTransferencia() != null ? resumen.getVentasTransferencia() : BigDecimal.ZERO);
    totalVentas = totalVentas.add(totalPlataformas);

    html.append("<div class='double-divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>TOTAL VENTAS:</span>");
    html.append("<span>").append(currencyFormat.format(totalVentas)).append("</span>");
    html.append("</div>");
    html.append("</div>");

    // RESUMEN DOCUMENTOS (SIEMPRE)
    html.append("<div class='section'>");
    html.append("<div class='section-title'>📄 DOCUMENTOS EMITIDOS</div>");
    html.append("<div class='divider'></div>");

    int totalDocs = 0;

    if (resumen.getCantidadFacturas() != null && resumen.getCantidadFacturas() > 0) {
      html.append("<div class='row'>");
      html.append("<span>Fact. Electrónicas: ").append(resumen.getCantidadFacturas()).append("</span>");
      html.append("<span>").append(formatearMontoCorto(resumen.getTotalFacturas())).append("</span>");
      html.append("</div>");
      totalDocs += resumen.getCantidadFacturas();
    }

    if (resumen.getCantidadTiquetes() != null && resumen.getCantidadTiquetes() > 0) {
      html.append("<div class='row'>");
      html.append("<span>Tiquetes Electr.: ").append(resumen.getCantidadTiquetes()).append("</span>");
      html.append("<span>").append(formatearMontoCorto(resumen.getTotalTiquetes())).append("</span>");
      html.append("</div>");
      totalDocs += resumen.getCantidadTiquetes();
    }

    if (resumen.getCantidadVentasInternas() != null && resumen.getCantidadVentasInternas() > 0) {
      html.append("<div class='row'>");
      html.append("<span>Facturas Internas: ").append(resumen.getCantidadVentasInternas()).append("</span>");
      html.append("<span>").append(formatearMontoCorto(resumen.getTotalVentasInternas())).append("</span>");
      html.append("</div>");
      totalDocs += resumen.getCantidadVentasInternas();
    }

    html.append("<div class='divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>TOTAL: ").append(totalDocs).append(" documentos</span>");
    html.append("</div>");
    html.append("</div>");

    // DETALLE DE FACTURAS (OPCIONAL)
    if (Boolean.TRUE.equals(opciones.getIncluirFacturas()) && resumen.getDocumentos() != null && !resumen.getDocumentos().isEmpty()) {
      agregarDetalleFacturas(html, resumen, currencyFormat);
    }

    // DENOMINACIONES (OPCIONAL)
    if (Boolean.TRUE.equals(opciones.getIncluirDenominaciones())) {
      agregarDenominaciones(html, sesionId, currencyFormat);
    }

    // DATAFONOS (OPCIONAL)
    if (Boolean.TRUE.equals(opciones.getIncluirDatafonos())) {
      agregarDatafonos(html, sesionId, currencyFormat);
    }

    // MOVIMIENTOS (OPCIONAL)
    if (Boolean.TRUE.equals(opciones.getIncluirMovimientos())) {
      agregarMovimientos(html, sesionId, currencyFormat);
    }

    // PLATAFORMAS (OPCIONAL)
    if (Boolean.TRUE.equals(opciones.getIncluirPlataformas()) && resumen.getVentasPlataformas() != null && !resumen.getVentasPlataformas().isEmpty()) {
      agregarPlataformas(html, resumen, currencyFormat);
    }

    html.append("</body>");
    html.append("</html>");

    return html.toString();
  }

  private String formatearMontoCorto(BigDecimal monto) {
    if (monto == null) return "₡ 0.00";

    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
    return currencyFormat.format(monto);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Integer> contarDocumentosPorTipo(Long sesionId) {
    log.debug("📊 Contando documentos por tipo para sesión {}", sesionId);

    Map<String, Integer> conteo = new HashMap<>();

    // Inicializar todos los contadores en 0
    conteo.put("FACTURA_ELECTRONICA", 0);
    conteo.put("TIQUETE_ELECTRONICO", 0);
    conteo.put("FACTURA_INTERNA", 0);
    conteo.put("TIQUETE_INTERNO", 0);
    conteo.put("NOTA_CREDITO", 0);

    try {
      // 1️⃣ CONTAR FACTURAS Y TIQUETES ELECTRÓNICOS
      List<Factura> facturas = facturaRepository.findBySesionCajaId(sesionId);

      for (Factura f : facturas) {
        // Solo contar documentos válidos (no anulados ni rechazados)
        if (f.getEstado() == EstadoFactura.ANULADA ||
            f.getEstado() == EstadoFactura.RECHAZADA) {
          continue;
        }

        String tipo = f.getTipoDocumento().name();

        // Mapear tipos de documentos
        if (tipo.equals("FACTURA_ELECTRONICA") || tipo.equals("FACTURA_INTERNA")) {
          conteo.merge("FACTURA_ELECTRONICA", 1, Integer::sum);
        } else if (tipo.equals("TIQUETE_ELECTRONICO") || tipo.equals("TIQUETE_INTERNO")) {
          conteo.merge("TIQUETE_ELECTRONICO", 1, Integer::sum);
        } else if (tipo.equals("NOTA_CREDITO")) {
          conteo.merge("NOTA_CREDITO", 1, Integer::sum);
        }
      }

      // 2️⃣ CONTAR FACTURAS INTERNAS (las que NO están en Hacienda)
      List<FacturaInterna> facturasInternas = facturaInternaRepository.findBySesionCajaId(sesionId);

      for (FacturaInterna fi : facturasInternas) {
        // Solo contar las que no están anuladas
        if ("ANULADA".equals(fi.getEstado())) {
          continue;
        }

        // Determinar si es factura o tiquete interno por el prefijo del número
        String numero = fi.getNumero();
        if (numero != null) {
          if (numero.startsWith("FI-") || numero.startsWith("FACT-")) {
            conteo.merge("FACTURA_INTERNA", 1, Integer::sum);
          } else if (numero.startsWith("TI-") || numero.startsWith("TIQ-")) {
            conteo.merge("TIQUETE_INTERNO", 1, Integer::sum);
          }
        }
      }

      log.debug("✅ Conteo de documentos: {}", conteo);

    } catch (Exception e) {
      log.error("❌ Error contando documentos para sesión {}: {}", sesionId, e.getMessage());
      // Retornar el mapa con ceros en caso de error
    }

    return conteo;
  }

  // Helper para formatear moneda
  private String formatearMoneda(BigDecimal monto) {
    if (monto == null) return "₡0";
    return String.format("₡%,d", monto.longValue());
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

  private void agregarDetalleFacturas(StringBuilder html, ResumenCajaDetalladoDTO resumen, NumberFormat currencyFormat) {
    // Agrupar documentos por tipo
    Map<String, List<ResumenCajaDetalladoDTO.DocumentoResumenDTO>> documentosPorTipo = new HashMap<>();
    BigDecimal totalFacturasElec = BigDecimal.ZERO;
    BigDecimal totalTiquetesElec = BigDecimal.ZERO;
    BigDecimal totalNotasCredito = BigDecimal.ZERO;
    BigDecimal totalFacturasInternas = BigDecimal.ZERO;

    for (ResumenCajaDetalladoDTO.DocumentoResumenDTO doc : resumen.getDocumentos()) {
      String tipo = doc.getTipoDocumento();
      documentosPorTipo.computeIfAbsent(tipo, k -> new ArrayList<>()).add(doc);

      if (tipo.contains("Factura") && tipo.contains("Electrónica")) {
        totalFacturasElec = totalFacturasElec.add(doc.getTotal());
      } else if (tipo.contains("Tiquete") && tipo.contains("Electrónico")) {
        totalTiquetesElec = totalTiquetesElec.add(doc.getTotal());
      } else if (tipo.contains("Nota") && tipo.contains("Crédito")) {
        totalNotasCredito = totalNotasCredito.add(doc.getTotal());
      } else if (tipo.contains("Interno")) {
        totalFacturasInternas = totalFacturasInternas.add(doc.getTotal());
      }
    }

    // FACTURAS ELECTRÓNICAS
    if (documentosPorTipo.containsKey("Factura Electrónica")) {
      List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> facturas = documentosPorTipo.get("Factura Electrónica");
      html.append("<div class='section'>");
      html.append("<div class='section-title'>📄 FACTURAS ELECTRÓNICAS (").append(facturas.size()).append(")</div>");
      html.append("<div class='divider'></div>");

      for (ResumenCajaDetalladoDTO.DocumentoResumenDTO doc : facturas) {
        html.append("<div class='detail-line'>");
        html.append(doc.getConsecutivo()).append(" ");
        html.append(currencyFormat.format(doc.getTotal())).append(" ");
        html.append(doc.getMetodoPago());
        html.append("</div>");
      }

      html.append("<div class='divider'></div>");
      html.append("<div class='row total-row'>");
      html.append("<span>SUBTOTAL:</span>");
      html.append("<span>").append(currencyFormat.format(totalFacturasElec)).append("</span>");
      html.append("</div>");
      html.append("</div>");
    }

    // TIQUETES ELECTRÓNICOS
    if (documentosPorTipo.containsKey("Tiquete Electrónico")) {
      List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> tiquetes = documentosPorTipo.get("Tiquete Electrónico");
      html.append("<div class='section'>");
      html.append("<div class='section-title'>📄 TIQUETES ELECTRÓNICOS (").append(tiquetes.size()).append(")</div>");
      html.append("<div class='divider'></div>");

      for (ResumenCajaDetalladoDTO.DocumentoResumenDTO doc : tiquetes) {
        html.append("<div class='detail-line'>");
        html.append(doc.getConsecutivo()).append(" ");
        html.append(currencyFormat.format(doc.getTotal())).append(" ");
        html.append(doc.getMetodoPago());
        html.append("</div>");
      }

      html.append("<div class='divider'></div>");
      html.append("<div class='row total-row'>");
      html.append("<span>SUBTOTAL:</span>");
      html.append("<span>").append(currencyFormat.format(totalTiquetesElec)).append("</span>");
      html.append("</div>");
      html.append("</div>");
    }

    // NOTAS DE CRÉDITO
    if (documentosPorTipo.containsKey("Nota de Crédito Electrónica")) {
      List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> notas = documentosPorTipo.get("Nota de Crédito Electrónica");
      html.append("<div class='section'>");
      html.append("<div class='section-title'>📄 NOTAS DE CRÉDITO (").append(notas.size()).append(")</div>");
      html.append("<div class='divider'></div>");

      for (ResumenCajaDetalladoDTO.DocumentoResumenDTO doc : notas) {
        html.append("<div class='detail-line'>");
        html.append(doc.getConsecutivo()).append(" ");
        html.append(currencyFormat.format(doc.getTotal())).append(" ");
        html.append(doc.getMetodoPago());
        html.append("</div>");
      }

      html.append("<div class='divider'></div>");
      html.append("<div class='row total-row'>");
      html.append("<span>SUBTOTAL:</span>");
      html.append("<span>").append(currencyFormat.format(totalNotasCredito)).append("</span>");
      html.append("</div>");
      html.append("</div>");
    }

    // FACTURAS INTERNAS
    if (documentosPorTipo.containsKey("Tiquete Interno")) {
      List<ResumenCajaDetalladoDTO.DocumentoResumenDTO> internas = documentosPorTipo.get("Tiquete Interno");
      html.append("<div class='section'>");
      html.append("<div class='section-title'>📄 FACTURAS INTERNAS (").append(internas.size()).append(")</div>");
      html.append("<div class='divider'></div>");

      for (ResumenCajaDetalladoDTO.DocumentoResumenDTO doc : internas) {
        html.append("<div class='detail-line'>");
        html.append(doc.getConsecutivo()).append(" ");
        html.append(currencyFormat.format(doc.getTotal())).append(" ");
        html.append(doc.getMetodoPago());
        html.append("</div>");
      }

      html.append("<div class='divider'></div>");
      html.append("<div class='row total-row'>");
      html.append("<span>SUBTOTAL:</span>");
      html.append("<span>").append(currencyFormat.format(totalFacturasInternas)).append("</span>");
      html.append("</div>");
      html.append("</div>");
    }
  }

  private void agregarDenominaciones(StringBuilder html, Long sesionId, NumberFormat currencyFormat) {
    List<SesionCajaDenominacion> denominaciones = sesionCajaDenominacionRepository.findBySesionCajaId(sesionId);

    if (denominaciones.isEmpty()) {
      return;
    }

    html.append("<div class='section'>");
    html.append("<div class='section-title'>💵 CONTEO DE DENOMINACIONES</div>");
    html.append("<div class='divider'></div>");

    BigDecimal totalContado = BigDecimal.ZERO;

    for (SesionCajaDenominacion denom : denominaciones) {
      html.append("<div class='row'>");
      html.append("<span>").append(currencyFormat.format(denom.getValor())).append(" x ").append(denom.getCantidad()).append("</span>");
      html.append("<span>").append(currencyFormat.format(denom.getTotal())).append("</span>");
      html.append("</div>");
      totalContado = totalContado.add(denom.getTotal());
    }

    html.append("<div class='divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>TOTAL CONTADO:</span>");
    html.append("<span>").append(currencyFormat.format(totalContado)).append("</span>");
    html.append("</div>");
    html.append("</div>");
  }

  private void agregarDatafonos(StringBuilder html, Long sesionId, NumberFormat currencyFormat) {
    List<CierreDatafono> datafonos = cierreDatafonoRepository.findBySesionCajaId(sesionId);

    if (datafonos.isEmpty()) {
      return;
    }

    html.append("<div class='section'>");
    html.append("<div class='section-title'>💳 CIERRES DATAFONOS</div>");
    html.append("<div class='divider'></div>");

    BigDecimal totalDatafonos = BigDecimal.ZERO;

    for (CierreDatafono datafono : datafonos) {
      html.append("<div class='row'>");
      html.append("<span>").append(datafono.getDatafono()).append("</span>");
      html.append("<span>").append(currencyFormat.format(datafono.getMonto())).append("</span>");
      html.append("</div>");
      totalDatafonos = totalDatafonos.add(datafono.getMonto());
    }

    html.append("<div class='divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>TOTAL TARJETAS:</span>");
    html.append("<span>").append(currencyFormat.format(totalDatafonos)).append("</span>");
    html.append("</div>");
    html.append("</div>");
  }

  private void agregarMovimientos(StringBuilder html, Long sesionId, NumberFormat currencyFormat) {
    List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionCajaIdOrderByFechaHoraAsc(sesionId);

    if (movimientos.isEmpty()) {
      return;
    }

    html.append("<div class='section'>");
    html.append("<div class='section-title'>💰 MOVIMIENTOS DE CAJA</div>");
    html.append("<div class='divider'></div>");

    BigDecimal totalEntradas = BigDecimal.ZERO;
    BigDecimal totalSalidas = BigDecimal.ZERO;

    // Separar entradas y salidas
    List<MovimientoCaja> entradas = new ArrayList<>();
    List<MovimientoCaja> salidas = new ArrayList<>();

    for (MovimientoCaja mov : movimientos) {
      if (mov.getTipoMovimiento().name().startsWith("ENTRADA")) {
        entradas.add(mov);
        totalEntradas = totalEntradas.add(mov.getMonto());
      } else if (mov.getTipoMovimiento().name().startsWith("SALIDA")) {
        salidas.add(mov);
        totalSalidas = totalSalidas.add(mov.getMonto());
      }
    }

    // ENTRADAS
    if (!entradas.isEmpty()) {
      html.append("<div class='section-title' style='font-size: 11px;'>[ENTRADAS]</div>");
      for (MovimientoCaja mov : entradas) {
        html.append("<div class='detail-line'>");
        html.append(mov.getConcepto()).append(" ");
        html.append(currencyFormat.format(mov.getMonto()));
        html.append("</div>");
      }
    }

    // SALIDAS
    if (!salidas.isEmpty()) {
      html.append("<div class='section-title' style='font-size: 11px; margin-top: 8px;'>[SALIDAS]</div>");
      for (MovimientoCaja mov : salidas) {
        html.append("<div class='detail-line'>");
        html.append(mov.getConcepto()).append(" ");
        html.append(currencyFormat.format(mov.getMonto()));
        html.append("</div>");
      }
    }

    html.append("<div class='divider'></div>");
    html.append("<div class='row'>");
    html.append("<span>Total Entradas:</span>");
    html.append("<span>").append(currencyFormat.format(totalEntradas)).append("</span>");
    html.append("</div>");
    html.append("<div class='row'>");
    html.append("<span>Total Salidas:</span>");
    html.append("<span>").append(currencyFormat.format(totalSalidas)).append("</span>");
    html.append("</div>");
    html.append("</div>");
  }

  private void agregarPlataformas(StringBuilder html, ResumenCajaDetalladoDTO resumen, NumberFormat currencyFormat) {
    html.append("<div class='section'>");
    html.append("<div class='section-title'>🚚 VENTAS POR PLATAFORMA</div>");
    html.append("<div class='divider'></div>");

    BigDecimal totalPlataformas = BigDecimal.ZERO;

    for (ResumenCajaDetalladoDTO.VentaPlataformaDTO plat : resumen.getVentasPlataformas()) {
      html.append("<div class='row'>");
      html.append("<span>").append(plat.getPlataformaNombre()).append(": ").append(plat.getCantidadTransacciones()).append(" pedidos</span>");
      html.append("<span>").append(currencyFormat.format(plat.getTotalVentas())).append("</span>");
      html.append("</div>");
      totalPlataformas = totalPlataformas.add(plat.getTotalVentas());
    }

    html.append("<div class='divider'></div>");
    html.append("<div class='row total-row'>");
    html.append("<span>TOTAL PLATAFORMAS:</span>");
    html.append("<span>").append(currencyFormat.format(totalPlataformas)).append("</span>");
    html.append("</div>");
    html.append("</div>");
  }
}