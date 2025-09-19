package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoImpuestoDto;
import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;

import java.util.List;

@Deprecated(since = "2.0", forRemoval = true)
public interface ProductoImpuestoService {
    
    // Gestión de impuestos
    List<ProductoImpuesto> obtenerImpuestos(Long productoId);
    ProductoImpuesto agregarImpuesto(Long productoId, ProductoImpuestoDto dto);
    void actualizarImpuestos(Long productoId, List<ProductoImpuestoDto> impuestos);
    void quitarImpuesto(Long productoId, Long impuestoId);
    void quitarTodosLosImpuestos(Long productoId);
}