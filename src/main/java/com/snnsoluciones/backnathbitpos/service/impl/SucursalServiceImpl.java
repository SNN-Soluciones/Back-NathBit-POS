package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository sucursalRepository;
    private final TerminalRepository terminalRepository;

    @Override
    public Optional<Sucursal> finById(Long id) {
        return sucursalRepository.findById(id);
    }

    @Override
    public Sucursal crear(Sucursal sucursal) {
        // Generar número de sucursal si no se proporciona
        if (sucursal.getNumeroSucursal() == null || sucursal.getNumeroSucursal().isEmpty()) {
            Integer maxNumero = sucursalRepository.findMaxNumeroSucursalByEmpresaId(sucursal.getEmpresa().getId());
            int siguiente = (maxNumero != null ? maxNumero : 0) + 1;
            sucursal.setNumeroSucursal(String.format("%03d", siguiente));
        }

        return sucursalRepository.save(sucursal);
    }

    @Override
    public Sucursal actualizar(Long id, Sucursal sucursal) {
        Sucursal existente = sucursalRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        // Campos existentes
        existente.setNombre(sucursal.getNombre());
        existente.setTelefono(sucursal.getTelefono());
        existente.setEmail(sucursal.getEmail());
        existente.setActiva(sucursal.getActiva());

        // === NUEVOS CAMPOS ===
        // Validar número de sucursal si cambió
        if (!existente.getNumeroSucursal().equals(sucursal.getNumeroSucursal())) {
            if (sucursalRepository.existsNumeroSucursalInEmpresa(
                existente.getEmpresa().getId(),
                sucursal.getNumeroSucursal(),
                id)) {
                throw new RuntimeException("El número de sucursal ya existe en esta empresa");
            }
            existente.setNumeroSucursal(sucursal.getNumeroSucursal());
        }

        existente.setModoFacturacion(sucursal.getModoFacturacion());
        existente.setProvincia(sucursal.getProvincia());
        existente.setCanton(sucursal.getCanton());
        existente.setDistrito(sucursal.getDistrito());
        existente.setBarrio(sucursal.getBarrio());
        existente.setOtrasSenas(sucursal.getOtrasSenas());

        return sucursalRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sucursal> buscarPorId(Long id) {
        return sucursalRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarPorEmpresa(Long empresaId) {
        return sucursalRepository.findByEmpresaId(empresaId);
    }

    @Override
    public void eliminar(Long id) {
        // Verificar que no tenga terminales activas
        if (terminalRepository.countActivasBySucursalId(id) > 0) {
            throw new RuntimeException("No se puede eliminar una sucursal con terminales activas");
        }
        sucursalRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarPorUsuarioYEmpresa(Long usuarioId, Long empresaId) {
        return sucursalRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }

    @Override
    public Boolean existsNumeroSucursalYEmpresaId(String numeroSucursal, Long empresaId) {
        return sucursalRepository.existsByNumeroSucursalAndEmpresaId(numeroSucursal, empresaId);
    }

    // === NUEVOS MÉTODOS ===

    @Override
    public Terminal crearTerminal(Long sucursalId, Terminal terminal) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        // Validar límite de terminales
        long terminalesActivas = terminalRepository.countActivasBySucursalId(sucursalId);
        if (terminalesActivas >= 2) {
            throw new RuntimeException("Máximo 2 terminales activas por sucursal");
        }

        // Generar número de terminal si no se proporciona
        if (terminal.getNumeroTerminal() == null || terminal.getNumeroTerminal().isEmpty()) {
            Integer maxNumero = terminalRepository.findMaxNumeroTerminalBySucursalId(sucursalId);
            int siguiente = (maxNumero != null ? maxNumero : 0) + 1;
            terminal.setNumeroTerminal(String.format("%05d", siguiente));
        }

        terminal.setSucursal(sucursal);
        return terminalRepository.save(terminal);
    }
}