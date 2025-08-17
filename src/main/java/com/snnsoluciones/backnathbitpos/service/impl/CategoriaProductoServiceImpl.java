package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import com.snnsoluciones.backnathbitpos.service.CategoriaProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoriaProductoServiceImpl implements CategoriaProductoService {
    
    private final CategoriaProductoRepository categoriaRepository;
    
    @Override
    @Transactional(readOnly = true)
    public CategoriaProducto buscarPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaProducto> listarPorEmpresa(Long empresaId, String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            // Si hay búsqueda, usar el query con filtro
            return categoriaRepository.buscarPorEmpresa(empresaId, busqueda, 
                    org.springframework.data.domain.PageRequest.of(0, 100))
                    .getContent();
        } else {
            // Si no hay búsqueda, traer todas las activas ordenadas
            return categoriaRepository.findByEmpresaIdAndActivoTrueOrderByOrdenAscNombreAsc(empresaId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existePorNombreYEmpresa(String nombre, Long empresaId) {
        return categoriaRepository.existsByNombreAndEmpresaId(nombre, empresaId);
    }
    
    @Override
    public CategoriaProducto crear(CategoriaProducto categoria) {
        // Si no tiene orden, asignar el siguiente disponible
        if (categoria.getOrden() == null || categoria.getOrden() == 0) {
            categoria.setOrden(obtenerSiguienteOrden(categoria.getEmpresa().getId()));
        }
        
        log.info("Creando categoría: {} para empresa: {}", 
                categoria.getNombre(), categoria.getEmpresa().getId());
        
        return categoriaRepository.save(categoria);
    }
    
    @Override
    public CategoriaProducto actualizar(Long id, CategoriaProducto categoria) {
        log.info("Actualizando categoría ID: {}", id);
        return categoriaRepository.save(categoria);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean tieneProductosActivos(Long categoriaId) {
        long cantidad = categoriaRepository.contarProductosActivos(categoriaId);
        return cantidad > 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long contarProductosActivos(Long categoriaId) {
        return categoriaRepository.contarProductosActivos(categoriaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSiguienteOrden(Long empresaId) {
        Integer siguienteOrden = categoriaRepository.obtenerSiguienteOrden(empresaId);
        return siguienteOrden != null ? siguienteOrden : 1;
    }
}