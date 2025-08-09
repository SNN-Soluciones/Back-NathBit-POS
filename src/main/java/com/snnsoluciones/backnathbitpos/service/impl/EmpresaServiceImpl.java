package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaServiceImpl implements EmpresaService {
    
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    
    @Override
    public Empresa crear(Empresa empresa) {
        return empresaRepository.save(empresa);
    }
    
    @Override
    public Empresa actualizar(Long id, Empresa empresa) {
        Empresa existente = empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        
        existente.setNombre(empresa.getNombre());
        existente.setCodigo(empresa.getCodigo());
        existente.setTipoIdentificacion(empresa.getTipoIdentificacion());
        existente.setIdentificacion(empresa.getIdentificacion());
        existente.setDireccion(empresa.getDireccion());
        existente.setTelefono(empresa.getTelefono());
        existente.setEmail(empresa.getEmail());
        existente.setActiva(empresa.getActiva());
        
        return empresaRepository.save(existente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Empresa> buscarPorId(Long id) {
        return empresaRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Empresa> buscarPorCodigo(String codigo) {
        return empresaRepository.findByCodigo(codigo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }
    
    @Override
    public void eliminar(Long id) {
        empresaRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return empresaRepository.existsByCodigo(codigo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeIdentificacion(String identificacion) {
        return empresaRepository.existsByIdentificacion(identificacion);
    }

    // En EmpresaServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public List<Empresa> listarPorUsuario(Long usuarioId) {
        // Obtener usuario para verificar rol
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si es ROOT o SOPORTE, devolver todas
        if (usuario.esRolSistema()) {
            return empresaRepository.findAll();
        }

        // Para otros, buscar por asignaciones
        return empresaRepository.findByUsuarioId(usuarioId);
    }
}