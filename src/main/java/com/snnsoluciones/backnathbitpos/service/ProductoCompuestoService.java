// ProductoCompuestoService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import java.util.List;

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

    List<OpcionSlotDTO> obtenerOpcionesSlot(Long slotId, Long sucursalId);
}