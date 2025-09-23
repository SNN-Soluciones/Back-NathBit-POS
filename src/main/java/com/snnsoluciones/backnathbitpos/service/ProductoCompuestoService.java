// ProductoCompuestoService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
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
    /**
     * Calcula el precio total según las opciones seleccionadas
     */
    CalculoPrecioResponse calcularPrecio(Long productoId, Long sucursalId, List<Long> opcionesSeleccionadas);

    /**
     * Valida que la selección cumpla las reglas y tenga stock
     */
    ValidacionSeleccionResponse validarSeleccion(Long productoId, Long sucursalId, List<Long> opcionesSeleccionadas);

    /**
     * Filtra compuestos por disponibilidad en sucursal
     */
    List<ProductoCompuestoDto> filtrarPorDisponibilidadSucursal(List<ProductoCompuestoDto> compuestos, Long sucursalId);

    /**
     * Actualiza disponibilidad global de una opción
     */
    void actualizarDisponibilidadGlobal(Long opcionId, Boolean disponible);
}