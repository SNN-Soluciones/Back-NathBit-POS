// ProductoAvanzadoServiceV2.java - Servicio consolidado para operaciones avanzadas
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductoAvanzadoServiceV2 {
    
    // ========== GESTIÓN DE IMPUESTOS ==========
    void asignarImpuestos(Long empresaId, Long productoId, List<ProductoImpuestoDto> impuestos);
    
    void eliminarImpuesto(Long empresaId, Long productoId, Long impuestoId);
    
    List<ProductoImpuestoDto> obtenerImpuestos(Long empresaId, Long productoId);
    
    // ========== GESTIÓN DE CATEGORÍAS ==========
    void asignarCategorias(Long empresaId, Long productoId, Set<Long> categoriaIds);
    
    void quitarCategoria(Long empresaId, Long productoId, Long categoriaId);
    
    // ========== GESTIÓN DE INVENTARIO ==========
    void ajustarInventario(Long empresaId, Long productoId, Long sucursalId, 
                          BigDecimal cantidad, String motivo);
    
    void transferirInventario(Long empresaId, Long productoId, 
                            Long sucursalOrigen, Long sucursalDestino, BigDecimal cantidad);
    
    BigDecimal obtenerStockDisponible(Long productoId, Long sucursalId);
    
    // ========== OPERACIONES MASIVAS ==========
    void actualizarPreciosMasivo(Long empresaId, List<Long> productoIds, 
                                BigDecimal porcentaje, boolean esAumento);
    
    void desactivarMasivo(Long empresaId, List<Long> productoIds);
    
    void activarMasivo(Long empresaId, List<Long> productoIds);
    
    // ========== RECETAS (si aplica) ==========
    void asignarReceta(Long empresaId, Long productoId, List<RecetaIngredienteDto> ingredientes);
    
    List<RecetaIngredienteDto> obtenerReceta(Long empresaId, Long productoId);
    
    boolean puedeProducirse(Long productoId, Long sucursalId, BigDecimal cantidad);
}