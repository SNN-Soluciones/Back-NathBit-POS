package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.RecetaCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.IngredienteDTO;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoRecetaService {

    private final ProductoRecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoInventarioService inventarioService;

    // Crear receta
    public ProductoReceta crearReceta(Long empresaId, RecetaCreateDTO dto) {
        // Validar que no exista
        if (recetaRepository.existsByEmpresaIdAndProductoId(empresaId, dto.getProductoId())) {
            throw new IllegalStateException("Ya existe una receta para este producto");
        }

        // Obtener y validar producto
        Producto producto = productoRepository.findById(dto.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Validar que el producto pertenezca a la empresa
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalStateException("El producto no pertenece a esta empresa");
        }

        // Validar tipo de producto
        if (producto.getTipo() != TipoProducto.VENTA &&
            producto.getTipo() != TipoProducto.AMBOS &&
            producto.getTipo() != TipoProducto.COMBO) {
            throw new IllegalStateException("Solo productos de venta, combo o ambos pueden tener receta");
        }

        // Crear receta
        ProductoReceta receta = new ProductoReceta();
        receta.setEmpresa(empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        receta.setProducto(producto);

        // Agregar ingredientes
        for (IngredienteDTO ing : dto.getIngredientes()) {
            agregarIngrediente(receta, ing);
        }

        // Calcular costo
        receta.calcularCosto();

        return recetaRepository.save(receta);
    }

    // Actualizar receta
    public ProductoReceta actualizarReceta(Long empresaId, Long recetaId, RecetaDTO dto) {
        ProductoReceta receta = recetaRepository.findById(recetaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        // Validar que la receta pertenezca a la empresa
        if (!receta.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalStateException("La receta no pertenece a esta empresa");
        }

        // Limpiar ingredientes actuales
        receta.getIngredientes().clear();

        // Agregar nuevos ingredientes
        for (IngredienteDTO ing : dto.getIngredientes()) {
            agregarIngrediente(receta, ing);
        }

        // Recalcular costo
        receta.calcularCosto();

        return recetaRepository.save(receta);
    }

    // Verificar si se puede producir
    public boolean puedeProducir(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        ProductoReceta receta = recetaRepository
            .findByEmpresaIdAndProductoIdWithIngredientes(empresaId, productoId)
            .orElse(null);

        if (receta == null) {
            return true; // Si no tiene receta, se puede vender
        }

        // Verificar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadNecesaria = ingrediente.getCantidad().multiply(cantidad);

            // Si tiene conversión, ajustar
            if (ingrediente.getProducto().getFactorConversion() != null) {
                cantidadNecesaria = cantidadNecesaria.divide(
                    ingrediente.getProducto().getFactorConversion(),
                    4,
                    BigDecimal.ROUND_HALF_UP
                );
            }

            if (!inventarioService.verificarDisponibilidad(
                ingrediente.getProducto().getId(),
                sucursalId,
                cantidadNecesaria)) {
                return false;
            }
        }

        return true;
    }

    // Descontar ingredientes al vender
    public void descontarIngredientes(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        ProductoReceta receta = recetaRepository
            .findByEmpresaIdAndProductoIdWithIngredientes(empresaId, productoId)
            .orElse(null);

        if (receta == null) {
            return; // No tiene receta, no hay que descontar
        }

        // Descontar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadADescontar = ingrediente.getCantidad().multiply(cantidad);

            // Si tiene conversión, ajustar
            if (ingrediente.getProducto().getFactorConversion() != null) {
                cantidadADescontar = cantidadADescontar.divide(
                    ingrediente.getProducto().getFactorConversion(),
                    4,
                    BigDecimal.ROUND_HALF_UP
                );
            }

            inventarioService.descontarInventario(
                ingrediente.getProducto().getId(),
                sucursalId,
                cantidadADescontar
            );
        }
    }

    // Obtener receta
    public ProductoReceta obtenerReceta(Long empresaId, Long productoId) {
        return recetaRepository.findByEmpresaIdAndProductoIdWithIngredientes(empresaId, productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));
    }

    // Listar recetas de la empresa
    public List<ProductoReceta> listarRecetas(Long empresaId) {
        return recetaRepository.findByEmpresaIdAndEstadoTrue(empresaId);
    }

    // Método helper privado
    private void agregarIngrediente(ProductoReceta receta, IngredienteDTO dto) {
        Producto ingrediente = productoRepository.findById(dto.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado"));

        // Validar que sea materia prima
        if (ingrediente.getTipo() != TipoProducto.MATERIA_PRIMA &&
            ingrediente.getTipo() != TipoProducto.AMBOS) {
            throw new IllegalStateException("El producto " + ingrediente.getNombre() +
                " no puede usarse como ingrediente");
        }

        // Validar que pertenezca a la misma empresa
        if (!ingrediente.getEmpresa().getId().equals(receta.getEmpresa().getId())) {
            throw new IllegalStateException("El ingrediente no pertenece a esta empresa");
        }

        RecetaIngrediente recetaIngrediente = new RecetaIngrediente();
        recetaIngrediente.setProducto(ingrediente);
        recetaIngrediente.setCantidad(dto.getCantidad());

        receta.agregarIngrediente(recetaIngrediente);
    }
}