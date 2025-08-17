package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.cabys.CABySDto;
import com.snnsoluciones.backnathbitpos.dto.cabys.EmpresaCABySDto;
import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import com.snnsoluciones.backnathbitpos.repository.CodigoCABySRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaCABySRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.EmpresaCABySService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmpresaCABySServiceImpl implements EmpresaCABySService {
    
    private final EmpresaCABySRepository empresaCABySRepository;
    private final CodigoCABySRepository codigoCABySRepository;
    private final EmpresaRepository empresaRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<CABySDto> buscarEnCatalogo(String impuesto, String busqueda) {
        List<CodigoCAByS> resultados;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            resultados = codigoCABySRepository.buscarPorTermino(busqueda);
        } else if (impuesto != null && !impuesto.trim().isEmpty()) {
            resultados = codigoCABySRepository.findByImpuestoSugeridoContainingAndActivoTrue(impuesto);
        } else {
            resultados = codigoCABySRepository.findTop100ByActivoTrueOrderByCodigoAsc();
        }
        
        return resultados.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    @Override
    public EmpresaCABySDto asignar(Long empresaId, Long codigoCabysId) {
        if (empresaCABySRepository.existsByEmpresaIdAndCodigoCabysId(empresaId, codigoCabysId)) {
            throw new RuntimeException("Este CAByS ya está asignado a la empresa");
        }
        
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        
        CodigoCAByS codigo = codigoCABySRepository.findById(codigoCabysId)
                .orElseThrow(() -> new RuntimeException("Código CAByS no encontrado"));
        
        EmpresaCAByS nuevo = EmpresaCAByS.builder()
                .empresa(empresa)
                .codigoCabys(codigo)
                .activo(true)
                .build();
        
        EmpresaCAByS guardado = empresaCABySRepository.save(nuevo);
        return convertirAEmpresaDto(guardado);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmpresaCABySDto> listarPorEmpresa(Long empresaId) {
        return empresaCABySRepository.findByEmpresaIdAndActivoTrue(empresaId)
                .stream()
                .map(this::convertirAEmpresaDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public void quitar(Long id) {
        EmpresaCAByS empresaCAByS = empresaCABySRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));
        
        empresaCAByS.setActivo(false);
        empresaCABySRepository.save(empresaCAByS);
    }
    
    // Métodos de conversión
    private CABySDto convertirADto(CodigoCAByS entity) {
        return CABySDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descripcion(entity.getDescripcion())
                .impuestoSugerido(entity.getImpuestoSugerido())
                .activo(entity.getActivo())
                .build();
    }
    
    private EmpresaCABySDto convertirAEmpresaDto(EmpresaCAByS entity) {
        return EmpresaCABySDto.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa().getId())
                .empresaNombre(entity.getEmpresa().getNombreComercial())
                .cabysId(entity.getCodigoCabys().getId())
                .cabysCodigo(entity.getCodigoCabys().getCodigo())
                .cabysDescripcion(entity.getCodigoCabys().getDescripcion())
                .cabysImpuesto(entity.getCodigoCabys().getImpuestoSugerido())
                .activo(entity.getActivo())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}