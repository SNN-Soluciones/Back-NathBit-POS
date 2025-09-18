package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.CategoriaProductoService;
import com.snnsoluciones.backnathbitpos.service.impl.ModularHelperService;
import com.snnsoluciones.backnathbitpos.service.impl.ModularHelperService.QueryParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaProductoServiceImpl implements CategoriaProductoService {

    private final CategoriaProductoRepository categoriaRepository;
    private final EmpresaRepository empresaRepository;
    private final ModularHelperService modularHelper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoriaProducto> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaProducto> listarPorEmpresa(Long empresaId, String busqueda) {
        log.debug("Listando categorías para empresa: {}", empresaId);

        // Obtener parámetros de búsqueda según configuración
        QueryParams params = modularHelper.construirParametrosBusqueda(empresaId, "categoria");

        if (params.esGlobal()) {
            log.debug("Buscando categorías GLOBALES de empresa: {}", empresaId);
            return categoriaRepository.findByEmpresaIdAndSucursalIdIsNullAndActivoTrueOrderByOrdenAsc(empresaId);
        } else {
            log.debug("Buscando categorías LOCALES de empresa: {} y sucursal: {}",
                empresaId, params.getSucursalId());
            return categoriaRepository.findByEmpresaIdAndSucursalIdAndActivoTrueOrderByOrdenAsc(
                empresaId, params.getSucursalId()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNombreYEmpresa(String nombre, Long empresaId) {
        // Obtener parámetros según configuración
        QueryParams params = modularHelper.construirParametrosBusqueda(empresaId, "categoria");

        if (params.esGlobal()) {
            return categoriaRepository.existsByNombreAndEmpresaIdAndSucursalIdIsNull(nombre, empresaId);
        } else {
            return categoriaRepository.existsByNombreAndEmpresaIdAndSucursalId(
                nombre, empresaId, params.getSucursalId()
            );
        }
    }

    @Override
    @Transactional
    public CategoriaProducto crear(CategoriaProducto categoria) {
        log.info("Creando categoría: {} para empresa: {}",
            categoria.getNombre(), categoria.getEmpresa().getId());

        Long empresaId = categoria.getEmpresa().getId();

        // Determinar si asignar sucursal según configuración
        Sucursal sucursal = modularHelper.determinarSucursalParaEntidad(empresaId, "categoria");
        categoria.setSucursal(sucursal);

        // Validar nombre duplicado en el contexto correcto
        if (existePorNombreYEmpresa(categoria.getNombre(), empresaId)) {
            throw new BusinessException("Ya existe una categoría con el nombre: " + categoria.getNombre());
        }

        // Si no tiene orden, asignar el siguiente
        if (categoria.getOrden() == null || categoria.getOrden() == 0) {
            Integer siguienteOrden = obtenerSiguienteOrden(empresaId);
            categoria.setOrden(siguienteOrden);
        }

        CategoriaProducto saved = categoriaRepository.save(categoria);
        log.info("Categoría creada exitosamente con ID: {} ({})",
            saved.getId(), sucursal == null ? "GLOBAL" : "LOCAL - Sucursal: " + sucursal.getId());

        return saved;
    }

    @Override
    @Transactional
    public CategoriaProducto actualizar(Long id, CategoriaProducto categoria) {
        log.info("Actualizando categoría con ID: {}", id);

        CategoriaProducto existente = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        // Validar que no se cambie el alcance (global <-> local)
        Long sucursalActualId = existente.getSucursal() != null ? existente.getSucursal().getId() : null;
        Long sucursalNuevaId = categoria.getSucursal() != null ? categoria.getSucursal().getId() : null;

        modularHelper.validarCambioAlcance(sucursalActualId, sucursalNuevaId, "categoría");

        // Validar nombre si cambió
        if (!existente.getNombre().equals(categoria.getNombre())) {
            if (existePorNombreYEmpresa(categoria.getNombre(), existente.getEmpresa().getId())) {
                throw new BusinessException("Ya existe una categoría con el nombre: " + categoria.getNombre());
            }
        }

        // Actualizar campos permitidos
        existente.setNombre(categoria.getNombre());
        existente.setDescripcion(categoria.getDescripcion());
        existente.setColor(categoria.getColor());
        existente.setIcono(categoria.getIcono());
        existente.setOrden(categoria.getOrden());
        existente.setActivo(categoria.getActivo());

        return categoriaRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneProductosActivos(Long categoriaId) {
        return contarProductosActivos(categoriaId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long contarProductosActivos(Long categoriaId) {
        return categoriaRepository.contarProductosActivos(categoriaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerSiguienteOrden(Long empresaId) {
        // Obtener parámetros según configuración
        QueryParams params = modularHelper.construirParametrosBusqueda(empresaId, "categoria");

        if (params.esGlobal()) {
            return categoriaRepository.obtenerSiguienteOrdenGlobal(empresaId);
        } else {
            return categoriaRepository.obtenerSiguienteOrdenLocal(empresaId, params.getSucursalId());
        }
    }
}