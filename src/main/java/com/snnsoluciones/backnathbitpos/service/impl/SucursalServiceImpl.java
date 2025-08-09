package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;
    
    @Override
    public Sucursal crear(Sucursal sucursal) {
        return sucursalRepository.save(sucursal);
    }
    
    @Override
    public Sucursal actualizar(Long id, Sucursal sucursal) {
        Sucursal existente = sucursalRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        
        existente.setNombre(sucursal.getNombre());
        existente.setCodigo(sucursal.getCodigo());
        existente.setDireccion(sucursal.getDireccion());
        existente.setTelefono(sucursal.getTelefono());
        existente.setEmail(sucursal.getEmail());
        existente.setActiva(sucursal.getActiva());
        // No cambiar empresa
        
        return sucursalRepository.save(existente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Sucursal> buscarPorId(Long id) {
        return sucursalRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Sucursal> buscarPorCodigo(String codigo) {
        return sucursalRepository.findByCodigo(codigo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarPorEmpresa(Long empresaId) {
        return sucursalRepository.findByEmpresaId(empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarTodas() {
        return sucursalRepository.findAll();
    }
    
    @Override
    public void eliminar(Long id) {
        sucursalRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return sucursalRepository.existsByCodigo(codigo);
    }

    // En SucursalServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarPorUsuario(Long usuarioId) {
        // Obtener usuario para verificar rol
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si es ROOT o SOPORTE, devolver todas
        if (usuario.esRolSistema()) {
            return sucursalRepository.findAll();
        }

        // Para otros, buscar por asignaciones
        return sucursalRepository.findByUsuarioId(usuarioId);
    }

    // En SucursalServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarPorUsuarioYEmpresa(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si es ROOT o SOPORTE, devolver todas las sucursales de esa empresa
        if (usuario.esRolSistema()) {
            return sucursalRepository.findByEmpresaId(empresaId);
        }

        // Para otros, buscar por asignaciones específicas
        return sucursalRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }
}