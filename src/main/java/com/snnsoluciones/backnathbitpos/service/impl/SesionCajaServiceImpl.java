package com.snnsoluciones.backnathbitpos.service.impl;

import com.lowagie.text.pdf.BaseFont;
import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarSesionRequest;
import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarTurnoRequest;
import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarTurnoResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.OpcionesImpresionCierreDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.TurnoReporteDTO;
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
import com.snnsoluciones.backnathbitpos.entity.SesionCajaUsuario;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.TipoConteoCaja;
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
import com.snnsoluciones.backnathbitpos.repository.SesionCajaUsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.ResendEmailService;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final SesionCajaUsuarioRepository sesionCajaUsuarioRepository;
  private final TerminalRepository terminalRepository;
  private final MovimientoCajaRepository movimientoCajaRepository;
  private final SecurityContextService securityContext;
  private final FacturaRepository facturaRepository;
  private final FacturaInternaRepository facturaInternaRepository;
  private final SesionCajaDenominacionRepository sesionCajaDenominacionRepository;
  private final SucursalRepository sucursalRepository;
  private final PlataformaDigitalConfigRepository plataformaDigitalConfigRepository;
  private final CierreDatafonoRepository cierreDatafonoRepository;
  private final ResendEmailService resendEmailService;


  @Override
  public SesionCaja abrirSesion(Long usuarioId, Long terminalId, BigDecimal montoInicial) {
    log.info("Abriendo sesión de caja para usuario: {} en terminal: {}", usuarioId, terminalId);

    if (!puedeAbrirCaja()) {
      throw new RuntimeException("No tiene permisos para abrir caja");
    }

    // En modo SHARED el usuario no "posee" una SesionCaja, tiene un turno.
    // Pero sí bloqueamos que la misma persona abra dos terminales distintas.
    if (terminalTieneSesionAbierta(terminalId)) {
      throw new RuntimeException("La terminal ya tiene una sesión abierta. Usa 'unirte a turno'.");
    }

    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    Terminal terminal = terminalRepository.findById(terminalId)
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

    if (!terminal.getActiva()) {
      throw new RuntimeException("La terminal no está activa");
    }

    BigDecimal monto = montoInicial != null ? montoInicial : BigDecimal.ZERO;

    // Crear sesión maestra (pertenece a la terminal, no al cajero)
    SesionCaja sesion = new SesionCaja();
    sesion.setUsuario(usuario);          // usuario que abrió (auditoría)
    sesion.setTerminal(terminal);
    sesion.setFechaHoraApertura(LocalDateTime.now(ZoneId.of("America/Costa_Rica")));
    sesion.setMontoInicial(monto);
    sesion.setEstado(EstadoSesion.ABIERTA);
    sesion.setModoCaja("SHARED");        // ← siempre SHARED desde ahora
    sesion.setObservacionesApertura("");
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
    log.info("Sesión maestra abierta con ID: {}", sesion.getId());

    // Crear el turno del cajero que abre (Ana)
    // fondoInicioTurno = montoInicial porque es la primera persona del día
    SesionCajaUsuario turnoApertura = new SesionCajaUsuario();
    turnoApertura.setSesionCaja(sesion);
    turnoApertura.setUsuario(usuario);
    turnoApertura.setFechaHoraInicio(sesion.getFechaHoraApertura());
    turnoApertura.setFondoInicioTurno(monto);   // ← fondo completo al inicio del día
    turnoApertura.setEstado("ACTIVA");
    turnoApertura.setVentasEfectivo(BigDecimal.ZERO);
    turnoApertura.setVentasTarjeta(BigDecimal.ZERO);
    turnoApertura.setVentasTransferencia(BigDecimal.ZERO);
    turnoApertura.setVentasOtros(BigDecimal.ZERO);
    turnoApertura.setTotalRetiros(BigDecimal.ZERO);
    turnoApertura.setTotalDevolucionesEfectivo(BigDecimal.ZERO);

    sesionCajaUsuarioRepository.save(turnoApertura);
    log.info("Turno de apertura creado para usuario {} (fondoInicioTurno={})", usuarioId, monto);

    return sesion;
  }

  @Transactional
  @Override
  public SesionCajaUsuario unirseATurno(Long usuarioId, Long sesionCajaId) {
    log.info("Usuario {} uniéndose a turno en sesión {}", usuarioId, sesionCajaId);

    SesionCaja sesion = sesionCajaRepository.findById(sesionCajaId)
        .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));

    if (sesion.getEstado() != EstadoSesion.ABIERTA) {
      throw new RuntimeException("La sesión de caja no está abierta");
    }

    if (!"SHARED".equals(sesion.getModoCaja())) {
      throw new RuntimeException("Esta sesión no es de modo compartido");
    }

    // Bloquear doble-entrada: el mismo usuario no puede tener dos turnos ACTIVOS
    // en la misma sesión al mismo tiempo (sí puede tener un turno CERRADO anterior)
    sesionCajaUsuarioRepository
        .findTurnoActivoUsuario(usuarioId)
        .ifPresent(t -> {
          throw new RuntimeException(
              "El usuario ya tiene un turno activo en la terminal: "
                  + t.getSesionCaja().getTerminal().getNombre()
                  + ". Debe cerrar ese turno antes de unirse a otra sesión."
          );
        });

    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Costa_Rica"));

    // Calcular cuánto efectivo hay en caja en este momento exacto.
    // = montoInicial + Σ ventas efectivo de todos los turnos - Σ retiros globales
    // Este es el "fondo" que recibe este cajero al entrar.
    BigDecimal fondoAlEntrar = calcularMontoEsperadoEfectivoHasta(sesion, ahora);

    SesionCajaUsuario turno = new SesionCajaUsuario();
    turno.setSesionCaja(sesion);
    turno.setUsuario(usuario);
    turno.setFechaHoraInicio(ahora);
    turno.setFondoInicioTurno(fondoAlEntrar);   // ← lo que hay en caja cuando entra
    turno.setEstado("ACTIVA");
    turno.setVentasEfectivo(BigDecimal.ZERO);
    turno.setVentasTarjeta(BigDecimal.ZERO);
    turno.setVentasTransferencia(BigDecimal.ZERO);
    turno.setVentasOtros(BigDecimal.ZERO);
    turno.setTotalRetiros(BigDecimal.ZERO);
    turno.setTotalDevolucionesEfectivo(BigDecimal.ZERO);

    turno = sesionCajaUsuarioRepository.save(turno);
    log.info("Turno {} creado para usuario {} — fondoInicioTurno={}",
        turno.getId(), usuarioId, fondoAlEntrar);

    return turno;
  }

  @Transactional
  @Override
  public CerrarTurnoResponse cerrarTurno(Long turnoId, CerrarTurnoRequest request) {
    log.info("Cerrando turno {}", turnoId);

    // 1. Cargar turno
    SesionCajaUsuario turno = sesionCajaUsuarioRepository.findById(turnoId)
        .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

    if ("CERRADA".equals(turno.getEstado())) {
      throw new RuntimeException("El turno ya fue cerrado");
    }

    // 2. Validar fondoCaja = montoContado - montoRetirado
    BigDecimal montoContado   = request.getMontoContado();
    BigDecimal montoRetirado  = request.getMontoRetirado();
    BigDecimal fondoCaja      = request.getFondoCaja();

    if (montoRetirado.compareTo(montoContado) > 0) {
      throw new RuntimeException(String.format(
          "No podés retirar más de lo que hay en caja. Contado: ₡%.2f, Retiro: ₡%.2f",
          montoContado, montoRetirado));
    }

    BigDecimal fondoCalculado = montoContado.subtract(montoRetirado);
    if (fondoCaja.compareTo(fondoCalculado) != 0) {
      throw new RuntimeException(String.format(
          "Fondo de caja no coincide. Esperado: ₡%.2f (₡%.2f - ₡%.2f), Recibido: ₡%.2f",
          fondoCalculado, montoContado, montoRetirado, fondoCaja));
    }

    // 3. Congelar timestamp del conteo
    LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Costa_Rica"));
    turno.setFechaHoraInicioConteo(ahora);

    // 4. Calcular esperados por medio de pago desde facturas del turno
    List<Factura> facturas         = facturaRepository.findByTurnoId(turnoId);
    List<FacturaInterna> internas  = facturaInternaRepository.findByTurnoId(turnoId);

    BigDecimal esperadoEfectivo      = BigDecimal.ZERO;
    BigDecimal esperadoTarjeta       = BigDecimal.ZERO;
    BigDecimal esperadoTransferencia = BigDecimal.ZERO;
    BigDecimal esperadoSinpe         = BigDecimal.ZERO;

    for (Factura f : facturas) {
      if (f.getMediosPago() != null) {
        for (FacturaMedioPago mp : f.getMediosPago()) {
          switch (obtenerMetodoPagoEstandar(mp.getMedioPago().name())) {
            case "E"  -> esperadoEfectivo      = esperadoEfectivo.add(mp.getMonto());
            case "TC" -> esperadoTarjeta        = esperadoTarjeta.add(mp.getMonto());
            case "TB" -> esperadoTransferencia  = esperadoTransferencia.add(mp.getMonto());
            case "S"  -> esperadoSinpe           = esperadoSinpe.add(mp.getMonto());
          }
        }
      }
    }

    for (FacturaInterna fi : internas) {
      if (fi.getMediosPago() != null) {
        for (FacturaInternaMedioPago mp : fi.getMediosPago()) {
          switch (obtenerMetodoPagoEstandar(mp.getTipo())) {
            case "E"  -> esperadoEfectivo      = esperadoEfectivo.add(mp.getMonto());
            case "TC" -> esperadoTarjeta        = esperadoTarjeta.add(mp.getMonto());
            case "TB" -> esperadoTransferencia  = esperadoTransferencia.add(mp.getMonto());
            case "S"  -> esperadoSinpe           = esperadoSinpe.add(mp.getMonto());
          }
        }
      }
    }

    // 5. Calcular diferencias (declarado - esperado)
    // Efectivo: usa montoEsperado de caja (incluye fondo inicial + ventas - retiros)
    BigDecimal montoEsperadoCaja = calcularMontoEsperadoEfectivoHasta(turno.getSesionCaja(), ahora);

    BigDecimal difEfectivo      = montoContado.subtract(montoEsperadoCaja);
    BigDecimal declaradoTarjeta = request.getTotalTarjeta()       != null ? request.getTotalTarjeta()       : BigDecimal.ZERO;
    BigDecimal declaradoSinpe   = request.getTotalSinpe()         != null ? request.getTotalSinpe()         : BigDecimal.ZERO;
    BigDecimal declaradoTransf  = request.getTotalTransferencia() != null ? request.getTotalTransferencia() : BigDecimal.ZERO;

    BigDecimal difTarjeta       = declaradoTarjeta.subtract(esperadoTarjeta);
    BigDecimal difSinpe         = declaradoSinpe.subtract(esperadoSinpe);
    BigDecimal difTransferencia = declaradoTransf.subtract(esperadoTransferencia);

    // 6. Persistir en el turno
    turno.setMontoEsperado(montoEsperadoCaja);
    turno.setMontoContado(montoContado);
    turno.setDiferencia(difEfectivo);
    turno.setDiferenciaEfectivo(difEfectivo);
    turno.setDiferenciaTarjeta(difTarjeta);
    turno.setDiferenciaSinpe(difSinpe);
    turno.setDiferenciaTransferencia(difTransferencia);
    turno.setVentasEfectivo(esperadoEfectivo);
    turno.setVentasTarjeta(esperadoTarjeta);
    turno.setVentasTransferencia(esperadoTransferencia);
    turno.setVentasOtros(esperadoSinpe);
    turno.setMontoRetirado(montoRetirado);
    turno.setFondoCaja(fondoCaja);
    turno.setObservacionesCierre(request.getObservaciones());

    // 7. Guardar denominaciones ligadas al turno
    if (request.getDenominaciones() != null && !request.getDenominaciones().isEmpty()) {
      final SesionCajaUsuario turnoFinal = turno;
      List<SesionCajaDenominacion> filas = request.getDenominaciones().stream()
          .map(d -> SesionCajaDenominacion.builder()
              .sesionCaja(turnoFinal.getSesionCaja())
              .sesionCajaUsuario(turnoFinal)
              .valor(d.getValor())
              .cantidad(d.getCantidad())
              .total(d.getValor().multiply(BigDecimal.valueOf(d.getCantidad())))
              .tipoConteo(TipoConteoCaja.TURNO)
              .build())
          .toList();
      sesionCajaDenominacionRepository.saveAll(filas);
    }

    // 8. Guardar datafonos ligados al turno
    if (request.getDatafonos() != null && !request.getDatafonos().isEmpty()) {
      final SesionCajaUsuario turnoFinal = turno;
      List<CierreDatafono> datafonos = request.getDatafonos().stream()
          .map(d -> CierreDatafono.builder()
              .sesionCaja(turnoFinal.getSesionCaja())
              .sesionCajaUsuario(turnoFinal)
              .datafono(d.getDatafono())
              .monto(d.getMonto())
              .build())
          .toList();
      cierreDatafonoRepository.saveAll(datafonos);
    }

    // 9. Cerrar turno
    turno.setFechaHoraFin(ahora);
    turno.setEstado("CERRADA");
    turno = sesionCajaUsuarioRepository.save(turno);

    log.info("Turno {} cerrado. Dif.efectivo: {}, Dif.tarjeta: {}, Dif.sinpe: {}, Dif.transf: {}",
        turno.getId(), difEfectivo, difTarjeta, difSinpe, difTransferencia);

    // 10. Verificar si es el último turno activo
    boolean todosHanCerrado = sesionCajaUsuarioRepository
        .todosLosTurnosCerrados(turno.getSesionCaja().getId());

    // 10a. Si es el último cajero → cerrar la sesión maestra automáticamente
    if (todosHanCerrado) {
      cerrarSesionMaestraInternal(turno.getSesionCaja(), turno);
      log.info("Sesión {} cerrada automáticamente — último turno cerrado por usuario {}",
          turno.getSesionCaja().getId(), turno.getUsuario().getId());
    }

    String mensaje = todosHanCerrado
        ? "Eras el último cajero. La sesión fue cerrada automáticamente."
        : "Turno cerrado exitosamente. La sesión continúa abierta para los demás cajeros.";

    // 11. Armar response
    return CerrarTurnoResponse.builder()
        .turnoId(turno.getId())
        .sesionCajaId(turno.getSesionCaja().getId())
        .estado(turno.getEstado())
        .montoEsperadoEfectivo(montoEsperadoCaja)
        .montoEsperadoTarjeta(esperadoTarjeta)
        .montoEsperadoTransferencia(esperadoTransferencia)
        .montoEsperadoSinpe(esperadoSinpe)
        .montoContado(montoContado)
        .totalEfectivoDeclarado(request.getTotalEfectivo())
        .totalTarjetaDeclarado(declaradoTarjeta)
        .totalTransferenciaDeclarado(declaradoTransf)
        .totalSinpeDeclarado(declaradoSinpe)
        .diferenciaEfectivo(difEfectivo)
        .diferenciaTarjeta(difTarjeta)
        .diferenciaTransferencia(difTransferencia)
        .diferenciaSinpe(difSinpe)
        .montoRetirado(montoRetirado)
        .fondoCaja(fondoCaja)
        .fechaHoraFin(turno.getFechaHoraFin())
        .esSesionCerrada(todosHanCerrado)
        .mensajeCierre(mensaje)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SesionCaja> obtenerSesionesSharedActivasPorSucursal(Long sucursalId) {
    log.info("Buscando sesiones SHARED activas para sucursal {}", sucursalId);
    return sesionCajaRepository.findSharedActivasBySucursal(sucursalId);
  }

  @Override
  @Transactional(readOnly = true)
  public SesionCajaUsuario obtenerMiTurnoActivo(Long usuarioId) {
    return sesionCajaUsuarioRepository.findTurnoActivoUsuario(usuarioId)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SesionCaja> obtenerSesionActivaPorTerminal(Long terminalId) {
    return sesionCajaRepository.findSesionAbiertaByTerminalId(terminalId);
  }

  /**
   * Cierra la sesión maestra consolidando los totales de todos sus turnos.
   * Se invoca automáticamente desde cerrarTurno() cuando el último cajero cierra.
   */
  private void cerrarSesionMaestraInternal(SesionCaja sesion, SesionCajaUsuario ultimoTurno) {
    List<SesionCajaUsuario> turnos = sesionCajaUsuarioRepository
        .findBySesionCajaId(sesion.getId());

    BigDecimal totalEfectivo      = turnos.stream().map(t -> t.getVentasEfectivo()      != null ? t.getVentasEfectivo()      : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTarjeta       = turnos.stream().map(t -> t.getVentasTarjeta()       != null ? t.getVentasTarjeta()       : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTransferencia = turnos.stream().map(t -> t.getVentasTransferencia() != null ? t.getVentasTransferencia() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalOtros         = turnos.stream().map(t -> t.getVentasOtros()         != null ? t.getVentasOtros()         : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

    LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Costa_Rica"));

    sesion.setTotalEfectivo(totalEfectivo);
    sesion.setTotalTarjeta(totalTarjeta);
    sesion.setTotalTransferencia(totalTransferencia);
    sesion.setTotalOtros(totalOtros);
    sesion.setMontoCierre(ultimoTurno.getMontoContado() != null ? ultimoTurno.getMontoContado() : BigDecimal.ZERO);
    sesion.setMontoRetirado(ultimoTurno.getMontoRetirado() != null ? ultimoTurno.getMontoRetirado() : BigDecimal.ZERO);
    sesion.setFondoCaja(ultimoTurno.getFondoCaja() != null ? ultimoTurno.getFondoCaja() : BigDecimal.ZERO);
    sesion.setEstadoConteo("COMPLETADO");
    sesion.setFechaHoraCierre(ahora);
    sesion.setEstado(EstadoSesion.CERRADA);

    sesionCajaRepository.save(sesion);
    log.info("Sesión maestra {} consolidada y cerrada automáticamente", sesion.getId());
  }

  @Transactional
  @Override
  public SesionCaja confirmarCierreSesion(Long sesionCajaId, Long usuarioId) {
    log.info("Confirmando cierre de sesión maestra {} por usuario {}", sesionCajaId, usuarioId);

    SesionCaja sesion = sesionCajaRepository.findById(sesionCajaId)
        .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

    if (sesion.getEstado() != EstadoSesion.ABIERTA) {
      throw new RuntimeException("La sesión ya está cerrada");
    }

    // Validar que todos los turnos estén cerrados
    boolean todosHanCerrado = sesionCajaUsuarioRepository
        .todosLosTurnosCerrados(sesionCajaId);

    if (!todosHanCerrado) {
      throw new RuntimeException("Aún hay cajeros con turno activo. Todos deben cerrar su turno antes de cerrar la sesión.");
    }

    // Consolidar totales sumando todos los turnos
    List<SesionCajaUsuario> turnos = sesionCajaUsuarioRepository
        .findBySesionCajaId(sesionCajaId);

    BigDecimal totalEfectivo      = turnos.stream().map(t -> t.getVentasEfectivo()      != null ? t.getVentasEfectivo()      : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTarjeta       = turnos.stream().map(t -> t.getVentasTarjeta()       != null ? t.getVentasTarjeta()       : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTransferencia = turnos.stream().map(t -> t.getVentasTransferencia() != null ? t.getVentasTransferencia() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalOtros         = turnos.stream().map(t -> t.getVentasOtros()         != null ? t.getVentasOtros()         : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

    // El fondo y retiro viene del último turno (el que confirmó el cierre)
    SesionCajaUsuario ultimoTurno = turnos.stream()
        .max(Comparator.comparing(SesionCajaUsuario::getFechaHoraFin))
        .orElseThrow(() -> new RuntimeException("No se encontraron turnos para la sesión"));

    LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Costa_Rica"));

    sesion.setTotalEfectivo(totalEfectivo);
    sesion.setTotalTarjeta(totalTarjeta);
    sesion.setTotalTransferencia(totalTransferencia);
    sesion.setTotalOtros(totalOtros);
    sesion.setMontoCierre(ultimoTurno.getMontoContado());
    sesion.setMontoRetirado(ultimoTurno.getMontoRetirado());
    sesion.setFondoCaja(ultimoTurno.getFondoCaja());
    sesion.setEstadoConteo("COMPLETADO");
    sesion.setFechaHoraCierre(ahora);
    sesion.setEstado(EstadoSesion.CERRADA);

    sesion = sesionCajaRepository.save(sesion);
    log.info("Sesión maestra {} cerrada por usuario {}", sesionCajaId, usuarioId);

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
    BigDecimal totalEfectivo      = nvl(sesion.getTotalEfectivo());
    BigDecimal totalTarjeta       = nvl(sesion.getTotalTarjeta());
    BigDecimal totalTransferencia = nvl(sesion.getTotalTransferencia());
    BigDecimal totalOtros         = nvl(sesion.getTotalOtros());  // SINPE

    // Plataformas: consultar facturas + internas de esta sesión
    BigDecimal totalPlataformas = nvl(facturaRepository.sumPlataformasBySesionId(sesion.getId()))
        .add(nvl(facturaInternaRepository.sumPlataformasBySesionId(sesion.getId())));

    BigDecimal totalVentas = totalEfectivo
        .add(totalTarjeta)
        .add(totalTransferencia)
        .add(totalOtros)
        .add(totalPlataformas);

    // Monto esperado y diferencia solo para sesiones cerradas
    BigDecimal montoEsperado  = null;
    BigDecimal diferenciaCierre = null;
    if (sesion.getEstado() == EstadoSesion.CERRADA) {
      montoEsperado = calcularMontoEsperado(sesion);
      if (sesion.getMontoCierre() != null) {
        diferenciaCierre = sesion.getMontoCierre().subtract(montoEsperado);
      }
    }

    return SesionCajaDTO.builder()
        .id(sesion.getId())
        .usuarioId(sesion.getUsuario().getId())
        .usuarioNombre(sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos())
        .usuarioEmail(sesion.getUsuario().getEmail())
        .sucursalId(sesion.getTerminal().getSucursal().getId())
        .sucursalNombre(sesion.getTerminal().getSucursal().getNombre())
        .terminalId(sesion.getTerminal().getId())
        .terminalNombre(sesion.getTerminal().getNombre())
        .fechaHoraApertura(sesion.getFechaHoraApertura())
        .fechaHoraCierre(sesion.getFechaHoraCierre())
        .montoInicial(sesion.getMontoInicial())
        .montoFinal(sesion.getMontoCierre())
        .montoEsperado(montoEsperado)
        .totalVentas(totalVentas)
        .totalEfectivo(totalEfectivo)
        .totalTarjeta(totalTarjeta)
        .totalTransferencia(totalTransferencia)
        .totalOtros(totalOtros)
        .totalPlataformas(totalPlataformas)
        .diferenciaCierre(diferenciaCierre)
        .estado(sesion.getEstado())
        .observaciones(sesion.getObservacionesCierre())
        .build();
  }

  /** null-safe BigDecimal */
  private BigDecimal nvl(BigDecimal v) {
    return v != null ? v : BigDecimal.ZERO;
  }

  private boolean puedeVerResumen(SesionCaja sesion) {
    // Supervisores pueden ver todo
    if (securityContext.isSupervisor()) {
      return true;
    }
    Long currentUserId = securityContext.getCurrentUserId();
    // El usuario que abrió la sesión puede verla
    if (sesion.getUsuario().getId().equals(currentUserId)) {
      return true;
    }
    // En modo SHARED: cualquier cajero que tenga o haya tenido un turno en esta sesión puede verla
    return sesionCajaUsuarioRepository
        .findBySesionCajaId(sesion.getId())
        .stream()
        .anyMatch(t -> t.getUsuario().getId().equals(currentUserId));
  }

  private boolean puedeAbrirCaja() {
    return securityContext.hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT", "SOPORTE");
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
    log.info("🎯 INICIANDO - Obteniendo resumen detallado de sesión: {}", sesionId);

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

    // 🆕 NUEVO: Cargar campos para Android
    resumen.setMontoRetirado(sesion.getMontoRetirado() != null ? sesion.getMontoRetirado() : BigDecimal.ZERO);
    resumen.setFondoCaja(sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO);

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

    // 🆕 NUEVO: Inicializar totalDevoluciones (notas de crédito)
    BigDecimal totalDevoluciones = BigDecimal.ZERO;

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
          .fechaEmision(f.getFechaEmision())
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
          // 🆕 NUEVO: Sumar a totalDevoluciones
          totalDevoluciones = totalDevoluciones.add(f.getTotalComprobante());
          break;
      }

      if (f.getMediosPago() != null) {

        for (FacturaMedioPago mp : f.getMediosPago()) {
          // Usar método estandarizado para determinar el tipo de pago
          String metodoPago = obtenerMetodoPagoEstandar(mp.getMedioPago().name());
          String metodoOriginal = mp.getMedioPago().name();

          switch (metodoPago) {
            case "E":
              totalEfectivo = totalEfectivo.add(mp.getMonto());
              break;
            case "TC":
              totalTarjeta = totalTarjeta.add(mp.getMonto());
              break;
            case "S":
              totalSinpe = totalSinpe.add(mp.getMonto());
              break;
            case "TB":
              totalTransferencia = totalTransferencia.add(mp.getMonto());
              break;
            case "PLATAFORMA":
              break;
            default:
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
      } else {
        log.info("    ❌ Sin medios de pago");
      }
    }

    // PROCESAR FACTURAS INTERNAS
    List<FacturaInterna> facturasInternas = facturaInternaRepository.findBySesionCajaId(sesionId);

    for (FacturaInterna fi : facturasInternas) {
      // Solo contar documentos válidos
      if ("ANULADA".equals(fi.getEstado())) {
        continue;
      }

      // Obtener métodos de pago para mostrar (ya estandarizados)
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

      // Sumar por tipo de pago usando método estandarizado
      if (fi.getMediosPago() != null && !fi.getMediosPago().isEmpty()) {

        for (FacturaInternaMedioPago mp : fi.getMediosPago()) {
          String metodoPago = obtenerMetodoPagoEstandar(mp.getTipo());

          switch (metodoPago) {
            case "E":
              totalEfectivo = totalEfectivo.add(mp.getMonto());
              break;
            case "TC":
              totalTarjeta = totalTarjeta.add(mp.getMonto());
              break;
            case "S":
              totalSinpe = totalSinpe.add(mp.getMonto());
              break;
            case "TB":
              totalTransferencia = totalTransferencia.add(mp.getMonto());
              break;
            default:
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
      } else {
        log.info("    ❌ Sin medios de pago internos");
      }

      cantVentasInternas++;
      totalVentasInternas = totalVentasInternas.add(fi.getTotal());
    }

    log.info("✅ FACTURAS INTERNAS PROCESADAS - Efectivo: {}, Tarjeta: {}, Sinpe: {}, Transferencia: {}, Total Internas: {}",
        totalEfectivo, totalTarjeta, totalSinpe, totalTransferencia, totalVentasInternas);

    // 🆕 NUEVO: Cargar datafonos para Android
    List<CierreDatafono> datafonosEntities = cierreDatafonoRepository.findBySesionCajaId(sesionId);
    List<ResumenCajaDetalladoDTO.DatafonoResumenDTO> datafonos = datafonosEntities.stream()
        .map(df -> ResumenCajaDetalladoDTO.DatafonoResumenDTO.builder()
            .datafono(df.getDatafono())
            .monto(df.getMonto())
            .build())
        .collect(Collectors.toList());

    resumen.setDatafonos(datafonos);
    log.info("💳 DATAFONOS CARGADOS: {} datafonos encontrados", datafonos.size());

    // PROCESAR PLATAFORMAS
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

    BigDecimal totalPlataformas = BigDecimal.ZERO;
    for (ResumenCajaDetalladoDTO.VentaPlataformaDTO plataforma : ventasPlataformas) {
      totalPlataformas = totalPlataformas.add(plataforma.getTotalVentas());
    }

    // Asignar totales por tipo de pago
    resumen.setVentasEfectivo(totalEfectivo);
    resumen.setVentasTarjeta(totalTarjeta);
    resumen.setVentasTransferencia(totalTransferencia);
    resumen.setVentasOtros(totalSinpe); // SINPE va en VentasOtros
    ventasPlataformas.sort((a, b) -> b.getTotalVentas().compareTo(a.getTotalVentas()));
    resumen.setVentasPlataformas(ventasPlataformas);

    // 🆕 NUEVO: Asignar totalDevoluciones al resumen
    resumen.setTotalDevoluciones(totalDevoluciones);

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

    // Lista de todos los movimientos — mapeados a DTO para evitar serialización de proxies Hibernate
    List<MovimientoCajaDTO> movimientosDTO = movimientoCajaRepository
        .findBySesionCajaIdOrderByFechaHoraDesc(sesionId)
        .stream()
        .map(m -> MovimientoCajaDTO.builder()
            .id(m.getId())
            .tipoMovimiento(m.getTipoMovimiento() != null ? m.getTipoMovimiento().name() : null)
            .monto(m.getMonto())
            .concepto(m.getConcepto())
            .fechaHora(m.getFechaHora())
            .observaciones(m.getObservaciones())
            .autorizadoPor(m.getAutorizadoPorId())
            .build())
        .collect(Collectors.toList());
    resumen.setMovimientos(movimientosDTO);

    sesion.setTotalEfectivo(totalEfectivo);

    BigDecimal esperado = calcularMontoEsperado(sesion);
    resumen.setMontoEsperado(esperado);

    resumen.setMontoCierre(sesion.getMontoCierre());

    // 🎯 VALIDACIÓN FINAL DE TOTALES
    BigDecimal totalFacturasCalculado = totalEfectivo
        .add(totalTarjeta)
        .add(totalSinpe)
        .add(totalTransferencia)
        .add(totalPlataformas);

    BigDecimal totalDocumentosCalculado = totalFacturas
        .add(totalTiquetes)
        .add(totalNC)
        .add(totalVentasInternas);

    if (totalFacturasCalculado.compareTo(totalDocumentosCalculado) != 0) {
      log.error("🚨 DISCREPANCIA CRÍTICA: Métodos pago ({}) vs Documentos ({}) - Diferencia: {}",
          totalFacturasCalculado, totalDocumentosCalculado,
          totalDocumentosCalculado.subtract(totalFacturasCalculado));
    } else {
      log.info("✅ TOTALES COINCIDEN PERFECTAMENTE");
    }

    log.info("🎯 FINALIZANDO - Resumen detallado generado para sesión: {}", sesionId);

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
        default:
          medios.add(mp.getMedioPago().name());
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

      switch (tipo) {
        case "EFECTIVO":
        case "E":
          medios.add("E");
          break;
        case "TARJETA":
        case "TC":
        case "TARJETA_CREDITO":
        case "TARJETA_DEBITO":
          medios.add("TC");
          break;
        case "SINPE":
        case "SINPE_MOVIL":
        case "S":
          medios.add("S");
          break;
        case "TRANSFERENCIA":
        case "TB":
        case "TRANSFERENCIA_BANCARIA":
          medios.add("TB");
          break;
        default:
          // Si viene un código desconocido, mantenerlo tal cual
          medios.add(tipo);
          break;
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
    String fechaAperturaStr = sesion.getFechaHoraApertura() != null
        ? sesion.getFechaHoraApertura().format(dateFormatter) : "-";
    String fechaCierreStr = sesion.getFechaHoraCierre() != null
        ? sesion.getFechaHoraCierre().format(dateFormatter) : "En curso";
    String cajeroNombre = sesion.getUsuario() != null
        ? sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "-";
    String terminalNombre = sesion.getTerminal() != null ? sesion.getTerminal().getNombre() : "-";

    html.append("<div class='header'>");
    html.append("<div class='title'>═══════════════════════════</div>");
    html.append("<div class='title'>CIERRE DE CAJA</div>");
    html.append("<div class='title'>═══════════════════════════</div>");
    html.append("<div class='row' style='margin-top:6px'>");
    html.append("<span>Cajero:</span><span>").append(cajeroNombre).append("</span>");
    html.append("</div>");
    html.append("<div class='row'>");
    html.append("<span>Terminal:</span><span>").append(terminalNombre).append("</span>");
    html.append("</div>");
    html.append("<div class='row'>");
    html.append("<span>Apertura:</span><span>").append(fechaAperturaStr).append("</span>");
    html.append("</div>");
    html.append("<div class='row'>");
    html.append("<span>Cierre:</span><span>").append(fechaCierreStr).append("</span>");
    html.append("</div>");
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

    // Mostrar monto contado y diferencia solo si la sesión está cerrada y tiene montoCierre
    if (sesion.getMontoCierre() != null) {
      BigDecimal diferencia = sesion.getMontoCierre().subtract(montoEsperado);

      html.append("<div class='row'>");
      html.append("<span>Efectivo Declarado:</span>");
      html.append("<span>").append(currencyFormat.format(sesion.getMontoCierre())).append("</span>");
      html.append("</div>");

      if (diferencia.compareTo(BigDecimal.ZERO) != 0) {
        String colorDif = diferencia.compareTo(BigDecimal.ZERO) > 0 ? "color:#16a34a;" : "color:#dc2626;";
        String signoDif = diferencia.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        html.append("<div class='row total-row' style='").append(colorDif).append("'>");
        html.append("<span>Diferencia:</span>");
        html.append("<span>").append(signoDif).append(currencyFormat.format(diferencia)).append("</span>");
        html.append("</div>");
      } else {
        html.append("<div class='row' style='color:#16a34a;'>");
        html.append("<span>Diferencia:</span>");
        html.append("<span>✓ Cuadra</span>");
        html.append("</div>");
      }
    }

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

  /**
   * 📨 Envía email de cierre de caja a todos los destinatarios relevantes
   * @param sesionId ID de la sesión
   * @param opciones Opciones de impresión para el PDF
   * @param emailAdicional Email adicional opcional (puede ser null)
   */
  @Override
  public void enviarEmailCierre(Long sesionId, OpcionesImpresionCierreDTO opciones, String emailAdicional) {
    log.info("📧 Iniciando envío de email de cierre para sesión: {}", sesionId);

    // 1. Obtener sesión
    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

    // 2. Obtener resumen
    ResumenCajaDetalladoDTO resumen = obtenerResumenDetallado(sesionId);

    // 3. Generar HTML del email
    String htmlContent = generarHtmlEmailCierre(sesion, resumen, opciones);

    // 4. Generar PDF
    String htmlPdf = generarHtmlCierre(sesionId, opciones);
    byte[] pdfBytes = convertirHtmlAPdf(htmlPdf);

    // 5. Recolectar destinatarios (sin duplicados)
    Set<String> destinatarios = new HashSet<>();

    // Email de la sucursal
    Sucursal sucursal = sesion.getTerminal().getSucursal();

    // Email de la empresa
    if (sucursal != null && sucursal.getEmpresa() != null) {
      // Priorizar emailNotificacion; fallback a email general si no tiene
      String emailEmpresa = sucursal.getEmpresa().getEmailNotificacion();
      if (emailEmpresa == null || emailEmpresa.isBlank()) {
        emailEmpresa = sucursal.getEmpresa().getEmail();
      }
      if (emailEmpresa != null && !emailEmpresa.isBlank()) {
        destinatarios.add(emailEmpresa.toLowerCase().trim());
      }
    }

    // Email adicional (parámetro opcional)
    if (emailAdicional != null && !emailAdicional.isBlank()) {
      destinatarios.add(emailAdicional.toLowerCase().trim());
    }

    // 6. Enviar a cada destinatario
    log.info("📤 Enviando cierre de caja a {} destinatarios: {}", destinatarios.size(), destinatarios);

    int enviados = 0;
    int fallidos = 0;

    for (String destinatario : destinatarios) {
      try {
        enviarEmailCierreIndividual(destinatario, sesion, htmlContent, pdfBytes);
        enviados++;
      } catch (Exception e) {
        log.error("❌ Falló envío a {}: {}", destinatario, e.getMessage());
        fallidos++;
      }
    }

    log.info("✅ Cierre de caja enviado - Exitosos: {}, Fallidos: {}", enviados, fallidos);
  }

  /**
   * 📨 Envía email individual con el cierre
   */
  private void enviarEmailCierreIndividual(
      String destinatario,
      SesionCaja sesion,
      String htmlContent,
      byte[] pdfBytes) {

    String asunto = String.format(
        "Cierre de Caja - %s - %s",
        sesion.getTerminal().getNombre(),
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    );

    List<ResendEmailService.EmailAttachment> adjuntos = List.of(
        new ResendEmailService.EmailAttachment(
            String.format("cierre_caja_%s.pdf", sesion.getId()),
            pdfBytes,
            "application/pdf"
        )
    );

    boolean enviado = resendEmailService.enviarConAdjuntos(
        destinatario, asunto, htmlContent, adjuntos
    );

    if (enviado) {
      log.info("✅ Email de cierre enviado a: {}", destinatario);
    } else {
      throw new RuntimeException("No se pudo enviar email a: " + destinatario);
    }
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

  private String obtenerMetodoPagoEstandar(String metodoOriginal) {
    if (metodoOriginal == null) return "";

    String metodo = metodoOriginal.toUpperCase().trim();

    // Estandarización para EFECTIVO
    if (metodo.equals("E") || metodo.contains("EFECTIVO")) return "E";

    // Estandarización para TARJETA
    if (metodo.equals("TC") || metodo.contains("TARJETA") ||
        metodo.contains("CREDITO") || metodo.contains("DEBITO")) return "TC";

    // Estandarización para SINPE
    if (metodo.equals("S") || metodo.contains("SINPE")) return "S";

    // Estandarización para TRANSFERENCIA
    if (metodo.equals("TB") || metodo.contains("TRANSFERENCIA") ||
        metodo.contains("BANCARIA")) return "TB";

    // 🆕 Estandarización para PLATAFORMA_DIGITAL
    if (metodo.contains("PLATAFORMA") || metodo.contains("DIGITAL") ||
        metodo.contains("UBER") || metodo.contains("RAPPI")) {
      return "PLATAFORMA";
    }

    return metodo; // Mantener original si no coincide
  }

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

  @Override
  public BigDecimal calcularMontoEsperadoEfectivoHasta(SesionCaja sesion, LocalDateTime hasta) {
    // fondo inicial de Ana
    BigDecimal esperado = sesion.getMontoInicial();

    // + todas las ventas en efectivo de TODOS los turnos hasta ese momento
    BigDecimal ventasEfectivo = sesionCajaUsuarioRepository
        .sumVentasEfectivoSesionHasta(sesion.getId(), hasta);
    esperado = esperado.add(ventasEfectivo != null ? ventasEfectivo : BigDecimal.ZERO);

    // - todas las salidas globales de caja hasta ese momento
    BigDecimal salidas = movimientoCajaRepository
        .sumSalidasBySesionIdHasta(sesion.getId(), hasta);
    esperado = esperado.subtract(salidas != null ? salidas : BigDecimal.ZERO);

    return esperado;
  }
  // =========================================================================
  // REPORTE DE SESIÓN (todos los turnos)
  // =========================================================================

  @Override
  @Transactional(readOnly = true)
  public List<TurnoReporteDTO> obtenerTurnosParaReporte(Long sesionId) {
    List<SesionCajaUsuario> turnos = sesionCajaUsuarioRepository
        .findBySesionCajaId(sesionId);

    return turnos.stream().map(t -> {
      BigDecimal ef  = nvl(t.getVentasEfectivo());
      BigDecimal tc  = nvl(t.getVentasTarjeta());
      BigDecimal tb  = nvl(t.getVentasTransferencia());
      BigDecimal sin = nvl(t.getVentasOtros());

      // Siempre recalcular montoEsperado desde la lógica del negocio
      // (incluye movimientos de caja: entradas y salidas)
      // Para turno único legacy: usa calcularMontoEsperado(sesion)
      // Para turnos SHARED reales: usa calcularMontoEsperadoEfectivoHasta al momento del cierre
      SesionCaja sesion = t.getSesionCaja();
      BigDecimal montoEsperadoReal;
      List<SesionCajaUsuario> todosLosTurnos = sesionCajaUsuarioRepository.findBySesionCajaId(sesion.getId());

      if (todosLosTurnos.size() == 1) {
        // Turno único (legacy o SHARED con un solo cajero): calcular desde sesión completa
        montoEsperadoReal = calcularMontoEsperado(sesion);
      } else {
        // Múltiples turnos: usar el valor guardado en el turno (calculado al momento del cierre)
        montoEsperadoReal = nvl(t.getMontoEsperado());
      }

      BigDecimal montoContado = nvl(t.getMontoContado());
      BigDecimal difEfectivo  = "CERRADA".equals(t.getEstado())
          ? montoContado.subtract(montoEsperadoReal)
          : BigDecimal.ZERO;

      return TurnoReporteDTO.builder()
          .turnoId(t.getId())
          .usuarioNombre(t.getUsuario().getNombre() + " " + t.getUsuario().getApellidos())
          .estado(t.getEstado())
          .fechaInicio(t.getFechaHoraInicio())
          .fechaFin(t.getFechaHoraFin())
          .fondoInicioTurno(nvl(t.getFondoInicioTurno()))
          .ventasEfectivo(ef)
          .ventasTarjeta(tc)
          .ventasTransferencia(tb)
          .ventasSinpe(sin)
          .totalVentas(ef.add(tc).add(tb).add(sin))
          .montoEsperado(montoEsperadoReal)
          .montoContado(montoContado)
          .diferenciaEfectivo(difEfectivo)
          .diferenciaTarjeta(nvl(t.getDiferenciaTarjeta()))
          .diferenciaTransferencia(nvl(t.getDiferenciaTransferencia()))
          .diferenciaSinpe(nvl(t.getDiferenciaSinpe()))
          .montoRetirado(nvl(t.getMontoRetirado()))
          .fondoCaja(nvl(t.getFondoCaja()))
          .observacionesCierre(t.getObservacionesCierre())
          .build();
    }).collect(Collectors.toList());
  }

  @Override
  public String generarHtmlReporteSesion(Long sesionId) {
    SesionCaja sesion = sesionCajaRepository.findById(sesionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

    List<TurnoReporteDTO> turnos = obtenerTurnosParaReporte(sesionId);

    NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    boolean sesionAbierta = sesion.getEstado() == EstadoSesion.ABIERTA;
    String estadoLabel = sesionAbierta
        ? "<span style='color:#f59e0b;font-weight:bold'>● EN CURSO</span>"
        : "<span style='color:#16a34a;font-weight:bold'>✓ CERRADA</span>";

    // ── Calcular totales generales ────────────────────────────────────────
    // Si no hay turnos (sesión legacy pre-SHARED), leer totales directamente de SesionCaja
    BigDecimal totEf, totTc, totTb, totSin, totVentas;
    boolean esSesionLegacy = turnos.isEmpty();

    if (esSesionLegacy) {
      totEf  = nvl(sesion.getTotalEfectivo());
      totTc  = nvl(sesion.getTotalTarjeta());
      totTb  = nvl(sesion.getTotalTransferencia());
      totSin = nvl(sesion.getTotalOtros());
    } else {
      totEf  = turnos.stream().map(TurnoReporteDTO::getVentasEfectivo).reduce(BigDecimal.ZERO, BigDecimal::add);
      totTc  = turnos.stream().map(TurnoReporteDTO::getVentasTarjeta).reduce(BigDecimal.ZERO, BigDecimal::add);
      totTb  = turnos.stream().map(TurnoReporteDTO::getVentasTransferencia).reduce(BigDecimal.ZERO, BigDecimal::add);
      totSin = turnos.stream().map(TurnoReporteDTO::getVentasSinpe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    totVentas = totEf.add(totTc).add(totTb).add(totSin);

    BigDecimal totEsperado  = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getMontoEsperado).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totContado   = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getMontoContado).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totDifEf     = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getDiferenciaEfectivo).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totDifTc     = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getDiferenciaTarjeta).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totDifTb     = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getDiferenciaTransferencia).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totDifSin    = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getDiferenciaSinpe).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totRetirado  = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getMontoRetirado).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totFondo     = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).map(TurnoReporteDTO::getFondoCaja).reduce(BigDecimal.ZERO, BigDecimal::add);

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
    sb.append("<meta name='viewport' content='width=device-width,initial-scale=1.0'>");
    sb.append("<title>Reporte de Sesión #").append(sesionId).append("</title>");
    sb.append("<style>");
    sb.append("*{margin:0;padding:0;box-sizing:border-box}");
    sb.append("body{font-family:'Courier New',monospace;font-size:11px;padding:10px;background:#fff}");
    sb.append(".center{text-align:center}");
    sb.append(".title{font-size:14px;font-weight:bold;text-align:center;margin:4px 0}");
    sb.append(".section-title{font-weight:bold;font-size:12px;margin:10px 0 4px;border-bottom:1px solid #000;padding-bottom:2px}");
    sb.append(".dash{border-top:1px dashed #000;margin:6px 0}");
    sb.append(".solid{border-top:1px solid #000;margin:6px 0}");
    sb.append(".double{border-top:2px solid #000;margin:6px 0}");
    sb.append(".row{display:flex;justify-content:space-between;margin:2px 0}");
    sb.append(".row-bold{display:flex;justify-content:space-between;margin:2px 0;font-weight:bold}");
    sb.append(".indent{padding-left:12px}");
    sb.append(".badge-activa{color:#f59e0b;font-weight:bold}");
    sb.append(".badge-cerrada{color:#16a34a;font-weight:bold}");
    sb.append(".dif-neg{color:#dc2626;font-weight:bold}");
    sb.append(".dif-pos{color:#16a34a;font-weight:bold}");
    sb.append(".dif-ok{color:#16a34a}");
    sb.append(".turno-block{border:1px solid #ccc;border-radius:4px;padding:8px;margin:8px 0}");
    sb.append(".turno-activa{border-color:#f59e0b;background:#fffbeb}");
    sb.append("table{width:100%;border-collapse:collapse;font-size:10px;margin:4px 0}");
    sb.append("th{background:#f3f4f6;text-align:left;padding:3px 5px;border:1px solid #ddd;font-weight:bold}");
    sb.append("th.right,td.right{text-align:right}");
    sb.append("td{padding:3px 5px;border:1px solid #ddd}");
    sb.append("tr.total-row{font-weight:bold;background:#f9fafb}");
    sb.append("tr.dif-row td.dif-neg{color:#dc2626;font-weight:bold}");
    sb.append("tr.dif-row td.dif-pos{color:#16a34a;font-weight:bold}");
    sb.append("tr.dif-row td.dif-ok{color:#16a34a}");
    sb.append("</style></head><body>");

    // ── HEADER ───────────────────────────────────────────────────────────
    sb.append("<div class='title'>═══════════════════════════</div>");
    sb.append("<div class='title'>REPORTE DE SESIÓN</div>");
    sb.append("<div class='title'>═══════════════════════════</div>");
    sb.append("<div class='dash'></div>");

    String empresa = sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial() != null
        ? sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial()
        : sesion.getTerminal().getSucursal().getEmpresa().getNombreRazonSocial();

    sb.append("<div class='row'><span>Empresa:</span><span>").append(empresa).append("</span></div>");
    sb.append("<div class='row'><span>Sucursal:</span><span>").append(sesion.getTerminal().getSucursal().getNombre()).append("</span></div>");
    sb.append("<div class='row'><span>Terminal:</span><span>").append(sesion.getTerminal().getNombre()).append("</span></div>");
    sb.append("<div class='row'><span>Sesión #:</span><span>").append(sesionId).append("</span></div>");
    sb.append("<div class='row'><span>Estado:</span>").append(estadoLabel).append("</div>");
    sb.append("<div class='row'><span>Apertura:</span><span>")
        .append(sesion.getFechaHoraApertura() != null ? sesion.getFechaHoraApertura().format(dtf) : "-")
        .append("</span></div>");
    sb.append("<div class='row'><span>Cierre:</span><span>")
        .append(sesion.getFechaHoraCierre() != null ? sesion.getFechaHoraCierre().format(dtf) : "En curso")
        .append("</span></div>");
    sb.append("<div class='row'><span>Fondo Inicial:</span><span>").append(fmt.format(sesion.getMontoInicial())).append("</span></div>");
    sb.append("<div class='double'></div>");

    // ── TURNOS ───────────────────────────────────────────────────────────
    sb.append("<div class='section-title'>TURNOS (").append(turnos.size()).append(")</div>");

    // ── Sesión legacy: sin registros de turno ─────────────────────────────
    if (esSesionLegacy) {
      sb.append("<div style='padding:10px;background:#f3f4f6;border-radius:4px;");
      sb.append("border-left:3px solid #9ca3af;font-size:11px;color:#6b7280;margin:6px 0'>");
      sb.append("<strong>Sesión sin desglose por cajero</strong><br>");
      sb.append("Esta sesión fue registrada antes del sistema de turnos compartidos. ");
      sb.append("Los totales se obtienen directamente del registro de la sesión.");
      sb.append("</div>");
    }

    for (int i = 0; i < turnos.size(); i++) {
      TurnoReporteDTO t = turnos.get(i);
      boolean activa = "ACTIVA".equals(t.getEstado());

      sb.append("<div class='turno-block").append(activa ? " turno-activa" : "").append("'>");

      // Sub-header turno
      sb.append("<div class='row-bold'>");
      sb.append("<span>").append(i + 1).append(". ").append(t.getUsuarioNombre()).append("</span>");
      sb.append("<span class='").append(activa ? "badge-activa" : "badge-cerrada").append("'>")
          .append(activa ? "● EN CURSO" : "✓ CERRADO").append("</span>");
      sb.append("</div>");

      sb.append("<div class='row indent'><span>Inicio:</span><span>")
          .append(t.getFechaInicio() != null ? t.getFechaInicio().format(dtf) : "-").append("</span></div>");
      sb.append("<div class='row indent'><span>Fin:</span><span>")
          .append(t.getFechaFin() != null ? t.getFechaFin().format(dtf) : "En curso").append("</span></div>");
      sb.append("<div class='row indent'><span>Fondo al entrar:</span><span>")
          .append(fmt.format(t.getFondoInicioTurno())).append("</span></div>");

      sb.append("<div class='dash'></div>");

      // Ventas
      sb.append("<div style='font-weight:bold;font-size:10px;margin:3px 0'>Ventas por medio de pago:</div>");
      sb.append("<div class='row indent'><span>Efectivo:</span><span>").append(fmt.format(t.getVentasEfectivo())).append("</span></div>");
      sb.append("<div class='row indent'><span>Tarjeta:</span><span>").append(fmt.format(t.getVentasTarjeta())).append("</span></div>");
      sb.append("<div class='row indent'><span>SINPE:</span><span>").append(fmt.format(t.getVentasSinpe())).append("</span></div>");
      sb.append("<div class='row indent'><span>Transferencia:</span><span>").append(fmt.format(t.getVentasTransferencia())).append("</span></div>");
      sb.append("<div class='row-bold indent'><span>Total Ventas:</span><span>").append(fmt.format(t.getTotalVentas())).append("</span></div>");

      if (!activa) {
        sb.append("<div class='dash'></div>");

        // Arqueo efectivo
        sb.append("<div style='font-weight:bold;font-size:10px;margin:3px 0'>Arqueo de efectivo:</div>");
        sb.append("<div class='row indent'><span>Esperado:</span><span>").append(fmt.format(t.getMontoEsperado())).append("</span></div>");
        sb.append("<div class='row indent'><span>Contado:</span><span>").append(fmt.format(t.getMontoContado())).append("</span></div>");

        sb.append("<div class='dash'></div>");

        // Diferencias
        sb.append("<div style='font-weight:bold;font-size:10px;margin:3px 0'>Diferencias:</div>");
        appendDifRow(sb, fmt, "Efectivo:", t.getDiferenciaEfectivo());
        appendDifRow(sb, fmt, "Tarjeta:", t.getDiferenciaTarjeta());
        appendDifRow(sb, fmt, "SINPE:", t.getDiferenciaSinpe());
        appendDifRow(sb, fmt, "Transferencia:", t.getDiferenciaTransferencia());

        sb.append("<div class='dash'></div>");

        // Distribución
        sb.append("<div style='font-weight:bold;font-size:10px;margin:3px 0'>Distribución:</div>");
        sb.append("<div class='row indent'><span>Retiro:</span><span>").append(fmt.format(t.getMontoRetirado())).append("</span></div>");
        sb.append("<div class='row indent'><span>Fondo que dejó:</span><span>").append(fmt.format(t.getFondoCaja())).append("</span></div>");

        if (t.getObservacionesCierre() != null && !t.getObservacionesCierre().isBlank()) {
          sb.append("<div class='dash'></div>");
          sb.append("<div class='row indent'><span>Obs:</span><span>").append(t.getObservacionesCierre()).append("</span></div>");
        }
      }

      sb.append("</div>"); // turno-block
    }

    sb.append("<div class='double'></div>");

    // ── TOTALES GENERALES ─────────────────────────────────────────────────
    sb.append("<div class='section-title'>TOTALES GENERALES</div>");
    sb.append("<div class='row'><span>Ventas Efectivo:</span><span>").append(fmt.format(totEf)).append("</span></div>");
    sb.append("<div class='row'><span>Ventas Tarjeta:</span><span>").append(fmt.format(totTc)).append("</span></div>");
    sb.append("<div class='row'><span>Ventas SINPE:</span><span>").append(fmt.format(totSin)).append("</span></div>");
    sb.append("<div class='row'><span>Ventas Transferencia:</span><span>").append(fmt.format(totTb)).append("</span></div>");
    sb.append("<div class='solid'></div>");
    sb.append("<div class='row-bold'><span>TOTAL VENTAS:</span><span>").append(fmt.format(totVentas)).append("</span></div>");

    long turnosCerrados = turnos.stream().filter(t -> "CERRADA".equals(t.getEstado())).count();
    // Para sesión legacy: mostrar monto esperado vs contado si está disponible
    if (esSesionLegacy && sesion.getMontoCierre() != null) {
      BigDecimal montoEsperado = calcularMontoEsperado(sesion);
      BigDecimal diferencia = sesion.getMontoCierre().subtract(montoEsperado);
      sb.append("<div class='dash'></div>");
      sb.append("<div class='row'><span>Monto Esperado:</span><span>").append(fmt.format(montoEsperado)).append("</span></div>");
      sb.append("<div class='row'><span>Monto Contado:</span><span>").append(fmt.format(sesion.getMontoCierre())).append("</span></div>");
      sb.append("<div class='row'><span>Retiro:</span><span>").append(fmt.format(nvl(sesion.getMontoRetirado()))).append("</span></div>");
      sb.append("<div class='row'><span>Fondo dejado:</span><span>").append(fmt.format(nvl(sesion.getFondoCaja()))).append("</span></div>");
    }
    if (turnosCerrados > 0) {
      sb.append("<div class='dash'></div>");
      sb.append("<div class='row'><span>Total Esperado (turnos cerrados):</span><span>").append(fmt.format(totEsperado)).append("</span></div>");
      sb.append("<div class='row'><span>Total Contado (turnos cerrados):</span><span>").append(fmt.format(totContado)).append("</span></div>");
      sb.append("<div class='row'><span>Total Retirado:</span><span>").append(fmt.format(totRetirado)).append("</span></div>");
      sb.append("<div class='row'><span>Total Fondo Dejado:</span><span>").append(fmt.format(totFondo)).append("</span></div>");
    }

    sb.append("<div class='double'></div>");

    // ── RESUMEN DIFERENCIAS ───────────────────────────────────────────────
    sb.append("<div class='section-title'>RESUMEN DE DIFERENCIAS</div>");

    // Tabla de diferencias
    sb.append("<table>");
    sb.append("<tr>");
    sb.append("<th>Cajero</th>");
    sb.append("<th class='right'>Dif. Efectivo</th>");
    sb.append("<th class='right'>Dif. Tarjeta</th>");
    sb.append("<th class='right'>Dif. SINPE</th>");
    sb.append("<th class='right'>Dif. Transfer.</th>");
    sb.append("</tr>");

    for (TurnoReporteDTO t : turnos) {
      boolean activa = "ACTIVA".equals(t.getEstado());
      sb.append("<tr").append(activa ? " style='background:#fffbeb'" : "").append(">");
      sb.append("<td>").append(t.getUsuarioNombre())
          .append(activa ? " <span class='badge-activa'>●</span>" : "").append("</td>");

      if (activa) {
        sb.append("<td class='right' colspan='4' style='color:#9ca3af;font-style:italic'>En curso</td>");
      } else {
        appendDifCell(sb, t.getDiferenciaEfectivo(), fmt);
        appendDifCell(sb, t.getDiferenciaTarjeta(), fmt);
        appendDifCell(sb, t.getDiferenciaSinpe(), fmt);
        appendDifCell(sb, t.getDiferenciaTransferencia(), fmt);
      }
      sb.append("</tr>");
    }

    // Fila de totales
    sb.append("<tr class='total-row'>");
    sb.append("<td>TOTALES</td>");
    appendDifCell(sb, totDifEf, fmt);
    appendDifCell(sb, totDifTc, fmt);
    appendDifCell(sb, totDifSin, fmt);
    appendDifCell(sb, totDifTb, fmt);
    sb.append("</tr>");
    sb.append("</table>");

    sb.append("</body></html>");
    return sb.toString();
  }

  /** Fila de diferencia con color */
  private void appendDifRow(StringBuilder sb, NumberFormat fmt, String label, BigDecimal valor) {
    String cls, text;
    if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
      cls = "dif-ok"; text = "✓ Cuadra";
    } else if (valor.compareTo(BigDecimal.ZERO) > 0) {
      cls = "dif-pos"; text = "+" + fmt.format(valor);
    } else {
      cls = "dif-neg"; text = fmt.format(valor);
    }
    sb.append("<div class='row indent ").append(cls).append("'>")
        .append("<span>").append(label).append("</span>")
        .append("<span>").append(text).append("</span>")
        .append("</div>");
  }

  /** Celda TD de diferencia con color */
  private void appendDifCell(StringBuilder sb, BigDecimal valor, NumberFormat fmt) {
    String cls, text;
    if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
      cls = "dif-ok"; text = "✓";
    } else if (valor.compareTo(BigDecimal.ZERO) > 0) {
      cls = "dif-pos"; text = "+" + fmt.format(valor);
    } else {
      cls = "dif-neg"; text = fmt.format(valor);
    }
    sb.append("<td class='right ").append(cls).append("'>").append(text).append("</td>");
  }
}