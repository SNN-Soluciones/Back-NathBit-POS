package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.RecetaCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaIngredienteDto;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaDto;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaUpdateDTO;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoRecetaService {

    private final ProductoRecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoInventarioService inventarioService;

    /**
     * Crear receta para un producto
     */
    @Transactional
    public ProductoReceta crearReceta(Long empresaId, Long productoId, RecetaCreateDTO dto) {
        log.info("Creando receta para producto {} de empresa {}", productoId, empresaId);

        // 1. Validar que no exista receta
        if (recetaRepository.existsByProductoIdAndEmpresaId(productoId, empresaId)) {
            throw new BusinessException("Ya existe una receta para este producto");
        }

        // 2. Obtener y validar producto
        Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // 3. Validar tipo de producto (solo VENTA con inventario RECETA)
        if (producto.getTipo() != TipoProducto.VENTA ||
            producto.getTipoInventario() != TipoInventario.RECETA) {
            throw new BusinessException(
                "Solo productos de VENTA con inventario tipo RECETA pueden tener receta"
            );
        }

        // 4. Crear receta
        ProductoReceta receta = new ProductoReceta();
        receta.setProducto(producto);
        receta.setEmpresa(producto.getEmpresa());
        receta.setIngredientes(new ArrayList<>());
        receta.setCostoEstimado(BigDecimal.ZERO);
        receta.setEstado(true);

        // 5. Agregar ingredientes
        if (dto.getIngredientes() != null && !dto.getIngredientes().isEmpty()) {
            for (RecetaIngredienteDto ingredienteDto : dto.getIngredientes()) {
                agregarIngrediente(receta, ingredienteDto, empresaId);
            }
        } else {
            throw new BusinessException("La receta debe tener al menos un ingrediente");
        }

        // 6. Calcular costo total
        calcularCostoTotal(receta);

        // 7. Guardar
        return recetaRepository.save(receta);
    }

    /**
     * Actualizar receta existente
     */
    @Transactional
    public ProductoReceta actualizarReceta(Long empresaId, Long recetaId, RecetaUpdateDTO dto) {
        log.info("Actualizando receta {} de empresa {}", recetaId, empresaId);

        // 1. Buscar receta
        ProductoReceta receta = recetaRepository.findByIdAndEmpresaId(recetaId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        // 2. Limpiar ingredientes actuales
        receta.getIngredientes().clear();

        // 3. Agregar nuevos ingredientes
        if (dto.getIngredientes() != null && !dto.getIngredientes().isEmpty()) {
            for (RecetaIngredienteDto ingredienteDto : dto.getIngredientes()) {
                agregarIngrediente(receta, ingredienteDto, empresaId);
            }
        } else {
            throw new BusinessException("La receta debe tener al menos un ingrediente");
        }

        // 4. Recalcular costo
        calcularCostoTotal(receta);

        // 5. Guardar
        return recetaRepository.save(receta);
    }

    /**
     * Verificar si se puede producir cantidad de producto
     */
    @Transactional(readOnly = true)
    public boolean puedeProducir(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        // 1. Buscar receta
        ProductoReceta receta = recetaRepository
            .findByProductoIdAndEmpresaId(productoId, empresaId)
            .orElse(null);

        // Si no tiene receta, se puede producir (producto sin receta)
        if (receta == null) {
            return true;
        }

        // 2. Verificar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadNecesaria = ingrediente.getCantidad().multiply(cantidad);

            // Obtener el inventario completo
            ProductoInventario inventario = inventarioService.obtenerInventario(
                ingrediente.getProducto().getId(),
                sucursalId
            );

            // Verificar si existe inventario
            if (inventario == null) {
                log.warn("No existe inventario para ingrediente {}",
                    ingrediente.getProducto().getNombre());
                return false;
            }

            // Obtener cantidad disponible (actual - bloqueada)
            BigDecimal cantidadDisponible = inventario.getCantidadActual()
                .subtract(inventario.getCantidadBloqueada() != null ?
                    inventario.getCantidadBloqueada() : BigDecimal.ZERO);

            if (cantidadDisponible.compareTo(cantidadNecesaria) < 0) {
                log.warn("Ingrediente {} insuficiente. Necesario: {}, Disponible: {}",
                    ingrediente.getProducto().getNombre(),
                    cantidadNecesaria,
                    cantidadDisponible);
                return false;
            }
        }

        return true;
    }

    /**
     * Descontar ingredientes al producir
     */
    @Transactional
    public void descontarIngredientes(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        log.info("Descontando ingredientes para producir {} unidades de producto {}", cantidad, productoId);

        // 1. Obtener receta
        ProductoReceta receta = recetaRepository
            .findByProductoIdAndEmpresaId(productoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        // 2. Descontar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadADescontar = ingrediente.getCantidad().multiply(cantidad);

            inventarioService.consumirInventario(
                ingrediente.getProducto().getId(),
                sucursalId,
                cantidadADescontar,
                "Producción de " + receta.getProducto().getNombre()
            );
        }
    }

    /**
     * Obtener receta de un producto
     */
    @Transactional(readOnly = true)
    public RecetaDto obtenerReceta(Long empresaId, Long productoId) {
        ProductoReceta receta = recetaRepository
            .findByProductoIdAndEmpresaId(productoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        return convertirADto(receta);
    }

    /**
     * Listar todas las recetas activas de la empresa
     */
    @Transactional(readOnly = true)
    public List<RecetaDto> listarRecetas(Long empresaId) {
        List<ProductoReceta> recetas = recetaRepository.findByEmpresaIdAndEstadoTrue(empresaId);
        return recetas.stream()
            .map(this::convertirADto)
            .toList();
    }

    /**
     * Eliminar (desactivar) receta
     */
    @Transactional
    public void eliminarReceta(Long empresaId, Long recetaId) {
        ProductoReceta receta = recetaRepository.findByIdAndEmpresaId(recetaId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        receta.setEstado(false);
        recetaRepository.save(receta);
        log.info("Receta {} desactivada", recetaId);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void agregarIngrediente(ProductoReceta receta, RecetaIngredienteDto dto, Long empresaId) {
        // Buscar ingrediente
        Producto ingrediente = productoRepository
            .findByIdAndEmpresaId(dto.getProductoId(), empresaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Ingrediente no encontrado: " + dto.getProductoId()));

        // Validar que sea MATERIA_PRIMA o MIXTO
        if (ingrediente.getTipo() != TipoProducto.MATERIA_PRIMA &&
            ingrediente.getTipo() != TipoProducto.MIXTO) {
            throw new BusinessException(
                "El producto " + ingrediente.getNombre() + " no puede usarse como ingrediente"
            );
        }

        // Crear relación
        RecetaIngrediente recetaIngrediente = new RecetaIngrediente();
        recetaIngrediente.setReceta(receta);
        recetaIngrediente.setProducto(ingrediente);
        recetaIngrediente.setCantidad(dto.getCantidad());
        recetaIngrediente.setCostoUnitario(ingrediente.getPrecioCompra());

        receta.getIngredientes().add(recetaIngrediente);
    }

    private void calcularCostoTotal(ProductoReceta receta) {
        BigDecimal costoTotal = BigDecimal.ZERO;

        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal costoIngrediente = ingrediente.getCantidad()
                .multiply(ingrediente.getCostoUnitario());
            costoTotal = costoTotal.add(costoIngrediente);
        }

        receta.setCostoEstimado(costoTotal);
        log.info("Costo total calculado para receta: {}", costoTotal);
    }

    private RecetaDto convertirADto(ProductoReceta receta) {
        RecetaDto dto = new RecetaDto();
        dto.setId(receta.getId());
        dto.setProductoId(receta.getProducto().getId());
        dto.setProductoNombre(receta.getProducto().getNombre());
        dto.setCostoTotal(receta.getCostoEstimado());

        List<RecetaIngredienteDto> ingredientes = new ArrayList<>();
        for (RecetaIngrediente ing : receta.getIngredientes()) {
            RecetaIngredienteDto ingDto = new RecetaIngredienteDto();
            ingDto.setProductoId(ing.getProducto().getId());
            ingDto.setProductoId(ing.getProducto().getId());
            ingDto.setCantidad(ing.getCantidad());
            ingDto.setCostoUnitario(ing.getCostoUnitario());
            ingredientes.add(ingDto);
        }
        dto.setIngredientes(ingredientes);

        return dto;
    }
}