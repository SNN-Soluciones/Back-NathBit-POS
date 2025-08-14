package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TerminalServiceImpl implements TerminalService {

    private final TerminalRepository terminalRepository;
    private final SesionCajaRepository sesionCajaRepository;

    private static final Long LIMITE_CONSECUTIVO = 9999999999L;
    private static final Long UMBRAL_ALERTA = 9999999000L;

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

    @Override
    @Transactional
    public Long obtenerSiguienteConsecutivo(Long terminalId, TipoDocumento tipoDocumento) {
        Terminal terminal = terminalRepository.findById(terminalId)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

        Long consecutivoActual = terminal.getConsecutivoPorTipo(tipoDocumento.getCodigo());

        if (consecutivoActual >= LIMITE_CONSECUTIVO) {
            throw new RuntimeException(
                "Terminal " + terminal.getNumeroTerminal() +
                    " agotó consecutivos para " + tipoDocumento.getDescripcion()
            );
        }

        if (consecutivoActual >= UMBRAL_ALERTA) {
            // TODO: Enviar notificación de alerta
            System.out.println("ALERTA: Terminal " + terminal.getNumeroTerminal() +
                " cerca del límite de consecutivos para " + tipoDocumento.getDescripcion());
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

        // Formar el consecutivo de 20 dígitos
        String consecutivo = String.format("%s%s%s%010d",
            sucursal.getNumeroSucursal(),      // 3 dígitos
            terminal.getNumeroTerminal(),       // 5 dígitos
            tipoDocumento.getCodigo(),          // 2 dígitos
            siguienteNumero                     // 10 dígitos
        );

        return consecutivo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaOcupada(Long terminalId) {
        return terminalRepository.isTerminalOcupada(terminalId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeEliminar(Long terminalId) {
        return !estaOcupada(terminalId);
    }
}