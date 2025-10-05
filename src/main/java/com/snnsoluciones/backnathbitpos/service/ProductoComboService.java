// ProductoComboService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoComboDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoComboRequest;
import java.math.BigDecimal;
import java.util.List;

public interface ProductoComboService {
    
    // ========== OPERACIONES CRUD ==========
    ProductoComboDto crear(Long empresaId, Long productoId, ProductoComboRequest request);
    
    ProductoComboDto actualizar(Long empresaId, Long productoId, ProductoComboRequest request);
    
    void eliminar(Long empresaId, Long productoId);
    
    // ========== CONSULTAS ==========
    ProductoComboDto buscarPorProductoId(Long empresaId, Long productoId);
    
    List<ProductoComboDto> listarPorEmpresa(Long empresaId);
    
    // ========== VALIDACIONES ==========
    boolean tieneStock(Long productoId, Long sucursalId);
}