package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioEmpresaServiceImpl implements UsuarioEmpresaService {
    
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    
    @Override
    public UsuarioEmpresa asignar(Long usuarioId, Long empresaId, Long sucursalId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
            
        Sucursal sucursal = null;
        if (sucursalId != null) {
            sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        }
        
        UsuarioEmpresa asignacion = new UsuarioEmpresa();
        asignacion.setUsuario(usuario);
        asignacion.setEmpresa(empresa);
        asignacion.setSucursal(sucursal);
        
        return usuarioEmpresaRepository.save(asignacion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioEmpresa> listarPorUsuario(Long usuarioId) {
        return usuarioEmpresaRepository.findByUsuarioId(usuarioId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioEmpresa> listarPorEmpresa(Long empresaId) {
        return usuarioEmpresaRepository.findByEmpresaId(empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Long usuarioId, Long empresaId, Long sucursalId) {
        if (sucursalId != null) {
            return usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId)
                .isPresent();
        }
        return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }

    @Override
    public boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId) {
        if (usuarioId == null || empresaId == null) {
            return false;
        }
      return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }

    @Override
    public Optional<UsuarioEmpresa> buscarPorId(Long id) {
        return usuarioEmpresaRepository.findById(id);
    }
}