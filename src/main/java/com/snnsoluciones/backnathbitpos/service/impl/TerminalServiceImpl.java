package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TerminalServiceImpl implements TerminalService {

  private final TerminalRepository terminalRepository;

  // Límites para consecutivos
  private static final Long LIMITE_CONSECUTIVO = 9999999999L;
  private static final Long UMBRAL_ALERTA = 9999999000L;

  // Tipos de documentos internos
  private static final List<String> TIPOS_INTERNOS = Arrays.asList("TI", "FI", "PF", "OP");

  @Override
  public Terminal crear(Terminal terminal) {
    // La validación de límite se hace en la entidad
    return terminalRepository.save(terminal);
  }

  @Override
  public Terminal actualizar(Long id, Terminal terminal) {
    Terminal existente = terminalRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

    existente.setNumeroTerminal(terminal.getNumeroTerminal());
    existente.setNombre(terminal.getNombre());
    existente.setDescripcion(terminal.getDescripcion());
    existente.setActiva(terminal.getActiva());
    existente.setImprimirAutomatico(terminal.getImprimirAutomatico());

    return terminalRepository.save(existente);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Terminal> buscarPorId(Long id) {
    return terminalRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Terminal> listarPorSucursal(Long sucursalId) {
    return terminalRepository.findBySucursalId(sucursalId);
  }

  @Override
  public void eliminar(Long id) {
    if (estaOcupada(id)) {
      throw new RuntimeException("No se puede eliminar una terminal con sesión abierta");
    }
    terminalRepository.deleteById(id);
  }

  /**
   * Formato para documentos internos (no Hacienda) Ejemplo: FI0101000098 FI (2) + Sucursal (2) +
   * Terminal (2) + Número (5)
   */
  private String formatoConsecutivoInterno(String tipoDoc, String sucursal, String terminal,
      Long numero) {
    // Extraer solo los últimos 2 dígitos de sucursal y terminal
    String sucursalCorto = sucursal.length() > 2 ?
        sucursal.substring(sucursal.length() - 2) :
        String.format("%02d", Integer.parseInt(sucursal));

    String terminalCorto = terminal.length() > 2 ?
        terminal.substring(terminal.length() - 2) :
        String.format("%02d", Integer.parseInt(terminal));

    return String.format("%s%s%s%05d",
        tipoDoc,        // 2 caracteres (TI, FI, PF, OP)
        sucursalCorto,  // 2 dígitos
        terminalCorto,  // 2 dígitos
        numero          // 5 dígitos
    );
  }

  /**
   * Formato para documentos electrónicos (Hacienda) Ejemplo: 00100001010000000001 Sucursal (3) +
   * Terminal (5) + TipoDoc (2) + Número (10)
   */
  private String formatoConsecutivoHacienda(String sucursal, String terminal, String tipoDoc,
      Long numero) {
    return String.format("%s%s%s%010d",
        sucursal,       // 3 dígitos (001, 002, etc.)
        terminal,       // 5 dígitos (00001, 00002, etc.)
        tipoDoc,        // 2 dígitos (01, 04, etc.)
        numero          // 10 dígitos
    );
  }

  @Override
  @Transactional
  public Long obtenerSiguienteConsecutivo(Long terminalId, TipoDocumento tipoDocumento) {
    Terminal terminal = terminalRepository.findById(terminalId)
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

    Long consecutivoActual = terminal.getConsecutivoPorTipo(tipoDocumento.getCodigo());

    // Para documentos internos, el límite es menor (99999)
    if (TIPOS_INTERNOS.contains(tipoDocumento.getCodigo())) {
      if (consecutivoActual >= 99999) {
        throw new RuntimeException(
            "Terminal " + terminal.getNumeroTerminal() +
                " agotó consecutivos internos para " + tipoDocumento.getDescripcion()
        );
      }
    } else {
      // Para documentos de Hacienda, límite normal
      if (consecutivoActual >= LIMITE_CONSECUTIVO) {
        throw new RuntimeException(
            "Terminal " + terminal.getNumeroTerminal() +
                " agotó consecutivos para " + tipoDocumento.getDescripcion()
        );
      }

      if (consecutivoActual >= UMBRAL_ALERTA) {
        log.warn("ALERTA: Terminal {} cerca del límite de consecutivos para {}",
            terminal.getNumeroTerminal(), tipoDocumento.getDescripcion());
      }
    }

    // Incrementar y guardar
    Long siguiente = terminal.incrementarConsecutivo(tipoDocumento.getCodigo());
    terminalRepository.save(terminal);

    return siguiente;
  }

  @Override
  @Transactional
  public String generarNumeroConsecutivo(Long terminalId, TipoDocumento tipoDocumento) {
    Terminal terminal = terminalRepository.findById(terminalId)
        .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

    if (!terminal.getActiva()) {
      throw new RuntimeException("La terminal no está activa");
    }

    Sucursal sucursal = terminal.getSucursal();
    Long siguienteNumero = obtenerSiguienteConsecutivo(terminalId, tipoDocumento);

    // Determinar si es documento interno
    boolean esDocumentoInterno = TIPOS_INTERNOS.contains(tipoDocumento.getCodigo());

    // Validar el modo de facturación de la sucursal
    if (!esDocumentoInterno && sucursal.getModoFacturacion() == ModoFacturacion.SOLO_INTERNO) {
      throw new RuntimeException(
          "La sucursal está configurada para solo emitir documentos internos");
    }

    // Validar que la empresa tenga configuración de Hacienda si es documento electrónico
    if (!esDocumentoInterno) {
      if (!sucursal.getEmpresa().getRequiereHacienda()) {
        throw new RuntimeException("La empresa no tiene configuración de Hacienda");
      }
      // Aquí podrías validar también que la configuración esté completa
    }

    String consecutivo;

    if (esDocumentoInterno) {
      // Formato interno simplificado: TI0101000098
      consecutivo = formatoConsecutivoInterno(
          tipoDocumento.getCodigo(),
          sucursal.getNumeroSucursal(),
          terminal.getNumeroTerminal(),
          siguienteNumero
      );
      log.info("Generado consecutivo interno: {}", consecutivo);
    } else {
      // Formato Hacienda (20 dígitos): 00100001010000000001
      consecutivo = formatoConsecutivoHacienda(
          sucursal.getNumeroSucursal(),
          terminal.getNumeroTerminal(),
          tipoDocumento.getCodigo(),
          siguienteNumero
      );
      log.info("Generado consecutivo Hacienda: {}", consecutivo);
    }

    return consecutivo;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean estaOcupada(Long terminalId) {
    return terminalRepository.isTerminalOcupada(terminalId);
  }
}