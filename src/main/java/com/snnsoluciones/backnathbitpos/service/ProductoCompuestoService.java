// ProductoCompuestoService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductoCompuestoService {
    
    // ========== OPERACIONES CRUD ==========
    ProductoCompuestoDto crear(Long empresaId, Long productoId, ProductoCompuestoRequest request);
    
    ProductoCompuestoDto actualizar(Long empresaId, Long productoId, ProductoCompuestoRequest request);
    
    void eliminar(Long empresaId, Long productoId);
    
    // ========== CONSULTAS ==========
    ProductoCompuestoDto buscarPorProductoId(Long empresaId, Long productoId);
    
    List<ProductoCompuestoDto> listarPorEmpresa(Long empresaId);
    
    // ========== VALIDACIONES ==========
    boolean esCompuesto(Long productoId);
    
    void validarSeleccion(Long productoId, Map<Long, List<Long>> seleccionPorSlot);
    
    // ========== CÁLCULO DE PRECIO ==========
    BigDecimal calcularPrecioTotal(Long productoId, Map<Long, List<Long>> seleccionPorSlot);
    
    // ========== OPERACIONES DE VENTA ==========
    void validarDisponibilidad(Long productoId, Long sucursalId, Map<Long, List<Long>> seleccionPorSlot);
    
    void descontarInventario(Long productoId, Long sucursalId, Map<Long, List<Long>> seleccionPorSlot);
    
    // ========== GESTIÓN DE OPCIONES ==========
    void habilitarOpcion(Long slotId, Long opcionId);
    
    void deshabilitarOpcion(Long slotId, Long opcionId);
    
    void establecerOpcionPorDefecto(Long slotId, Long opcionId);
}