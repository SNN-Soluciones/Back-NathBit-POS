package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Provincia;
import com.snnsoluciones.backnathbitpos.entity.Canton;
import com.snnsoluciones.backnathbitpos.entity.Distrito;
import com.snnsoluciones.backnathbitpos.entity.Barrio;
import com.snnsoluciones.backnathbitpos.repository.ProvinciaRepository;
import com.snnsoluciones.backnathbitpos.repository.CantonRepository;
import com.snnsoluciones.backnathbitpos.repository.DistritoRepository;
import com.snnsoluciones.backnathbitpos.repository.BarrioRepository;
import com.snnsoluciones.backnathbitpos.service.UbicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UbicacionServiceImpl implements UbicacionService {
    
    private final ProvinciaRepository provinciaRepository;
    private final CantonRepository cantonRepository;
    private final DistritoRepository distritoRepository;
    private final BarrioRepository barrioRepository;
    
    @Override
    @Cacheable("provincias")
    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll();
    }
    
    @Override
    public Optional<Provincia> buscarProvinciaPorId(Integer id) {
        return provinciaRepository.findById(id);
    }
    
    @Override
    @Cacheable("cantones")
    public List<Canton> listarCantonesPorProvincia(Integer codigoProvincia) {
        return cantonRepository.findByCodigoProvincia(codigoProvincia);
    }
    
    @Override
    public Optional<Canton> buscarCantonPorId(Integer id) {
        return cantonRepository.findById(id);
    }

    @Override
    @Cacheable("distritos")
    public List<Distrito> listarDistritosPorCanton(Integer codigoCanton) {
        return distritoRepository.findByCodigoProvinciaAndCodigoCanton(codigoCanton);
    }
    
    @Override
    public Optional<Distrito> buscarDistritoPorId(Integer id) {
        return distritoRepository.findById(id);
    }

    @Override
    @Cacheable("barrios")
    public List<Barrio> listarBarriosPorDistrito(Integer codigoDistrito) {
        return barrioRepository.findByCodigoProvinciaAndCodigoCantonAndCodigoDistrito(codigoDistrito);
    }
    
    @Override
    public Optional<Barrio> buscarBarrioPorId(Integer id) {
        return barrioRepository.findById(id);
    }
}