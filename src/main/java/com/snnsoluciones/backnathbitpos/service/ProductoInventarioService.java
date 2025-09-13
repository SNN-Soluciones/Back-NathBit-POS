package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.AjusteInventarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoInventario;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoInventarioRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoInventarioService {
    
    private final ProductoInventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    
    // Obtener inventario de un producto en una sucursal
    public ProductoInventario obtenerInventario(Long productoId, Long sucursalId) {
        return inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado"));
    }
    
    // Crear inventario inicial para un producto
    public ProductoInventario crearInventario(Long productoId, Long sucursalId, BigDecimal cantidadMinima) {
        // Verificar que no exista
        if (inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId).isPresent()) {
            throw new IllegalStateException("El inventario ya existe para este producto y sucursal");
        }
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        ProductoInventario inventario = new ProductoInventario();
        inventario.setProducto(producto);
        inventario.setSucursal(sucursal);
        inventario.setCantidadMinima(cantidadMinima);
        inventario.setCantidadActual(BigDecimal.ZERO);
        
        return inventarioRepository.save(inventario);
    }
    
    // Ajustar inventario (entrada/salida manual)
    public ProductoInventario ajustarInventario(AjusteInventarioDTO ajuste) {
        ProductoInventario inventario = obtenerInventario(ajuste.getProductoId(), ajuste.getSucursalId());
        
        if (ajuste.getTipoAjuste().equals("ABSOLUTO")) {
            inventario.actualizarCantidad(ajuste.getCantidad());
        } else {
            inventario.ajustarCantidad(ajuste.getCantidad());
        }
        
        return inventarioRepository.save(inventario);
    }
    
    // Descontar inventario (para ventas)
    public void descontarInventario(Long productoId, Long sucursalId, BigDecimal cantidad) {
        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);
        
        if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
            throw new IllegalStateException("Stock insuficiente. Disponible: " + inventario.getCantidadActual());
        }
        
        inventario.ajustarCantidad(cantidad.negate());
        inventarioRepository.save(inventario);
    }
    
    // Obtener productos bajo mínimo
    public List<ProductoInventario> obtenerBajoMinimos(Long sucursalId) {
        return inventarioRepository.findBajoMinimosBySucursal(sucursalId);
    }
    
    // Verificar disponibilidad
    public boolean verificarDisponibilidad(Long productoId, Long sucursalId, BigDecimal cantidadRequerida) {
        try {
            ProductoInventario inventario = obtenerInventario(productoId, sucursalId);
            return inventario.getCantidadActual().compareTo(cantidadRequerida) >= 0;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    // Obtener todos los inventarios de una sucursal
    public List<ProductoInventario> obtenerInventarioPorSucursal(Long sucursalId) {
        return inventarioRepository.findBySucursalIdAndEstadoTrue(sucursalId);
    }

    // Obtener inventario de un producto en todas las sucursales
    public List<ProductoInventario> obtenerInventarioPorProducto(Long productoId) {
        return inventarioRepository.findByProductoIdAndEstadoTrue(productoId);
    }
}