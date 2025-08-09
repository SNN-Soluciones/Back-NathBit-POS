package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
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

    @Service
    @RequiredArgsConstructor
    @Transactional
    public static class SucursalServiceImpl implements SucursalService {

        private final SucursalRepository sucursalRepository;

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
    }
}