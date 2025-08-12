package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.Producto;

public interface ProductoCrudService {
    
    // CRUD básico
    ProductoDto crear(Long empresaId, ProductoCreateDto dto);
    ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto);
    ProductoDto obtenerPorId(Long empresaId, Long productoId);
    Producto obtenerEntidadPorId(Long productoId);
    void eliminar(Long empresaId, Long productoId);
    void activarDesactivar(Long empresaId, Long productoId, boolean activo);
    
    // Generación de código
    String generarCodigoInterno(Long empresaId);
}