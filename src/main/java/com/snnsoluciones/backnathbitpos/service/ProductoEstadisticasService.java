package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.estadisticas.ProductoEstadisticasV2Dto;
import com.snnsoluciones.backnathbitpos.dto.estadisticas.ProductoEstadisticasV2Dto.CategoriaEstadistica;
import com.snnsoluciones.backnathbitpos.dto.estadisticas.ProductoEstadisticasV2Dto.ProductoTopVenta;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoEstadisticasService {
    
    private final EntityManager entityManager;
    private final ProductoRepository productoRepository;
    
    /**
     * Obtiene estadísticas a nivel EMPRESA
     */
    @Transactional(readOnly = true)
    public ProductoEstadisticasV2Dto obtenerEstadisticasEmpresa(Long empresaId, String periodo) {
        log.info("Obteniendo estadísticas de productos para empresa: {}, periodo: {}", empresaId, periodo);
        
        ProductoEstadisticasV2Dto estadisticas = new ProductoEstadisticasV2Dto();
        estadisticas.setEmpresaId(empresaId);
        estadisticas.setPeriodo(periodo);
        
        // 1. Totales generales
        obtenerTotalesGenerales(estadisticas, empresaId, null);
        
        // 2. Por tipo de producto
        obtenerEstadisticasPorTipo(estadisticas, empresaId, null);
        
        // 3. Top categorías
        obtenerTopCategorias(estadisticas, empresaId, null);
        
        // 4. Productos más vendidos (si periodo != TOTAL)
        if (!"TOTAL".equals(periodo)) {
            obtenerTopProductosVendidos(estadisticas, empresaId, null, periodo);
        }
        
        // 5. Sin categoría
        contarSinCategoria(estadisticas, empresaId, null);
        
        return estadisticas;
    }
    
    /**
     * Obtiene estadísticas a nivel SUCURSAL
     */
    @Transactional(readOnly = true)
    public ProductoEstadisticasV2Dto obtenerEstadisticasSucursal(Long sucursalId, String periodo) {
        log.info("Obteniendo estadísticas de productos para sucursal: {}, periodo: {}", sucursalId, periodo);
        
        // Primero obtener la empresa de la sucursal
        String empresaQuery = "SELECT s.empresa.id FROM Sucursal s WHERE s.id = :sucursalId";
        Long empresaId = entityManager.createQuery(empresaQuery, Long.class)
            .setParameter("sucursalId", sucursalId)
            .getSingleResult();
        
        ProductoEstadisticasV2Dto estadisticas = new ProductoEstadisticasV2Dto();
        estadisticas.setEmpresaId(empresaId);
        estadisticas.setSucursalId(sucursalId);
        estadisticas.setPeriodo(periodo);
        
        // Similar a empresa pero filtrando por sucursal
        obtenerTotalesGenerales(estadisticas, empresaId, sucursalId);
        obtenerEstadisticasPorTipo(estadisticas, empresaId, sucursalId);
        obtenerTopCategorias(estadisticas, empresaId, sucursalId);
        
        if (!"TOTAL".equals(periodo)) {
            obtenerTopProductosVendidos(estadisticas, empresaId, sucursalId, periodo);
        }
        
        contarSinCategoria(estadisticas, empresaId, sucursalId);
        
        return estadisticas;
    }
    
    private void obtenerTotalesGenerales(ProductoEstadisticasV2Dto stats, Long empresaId, Long sucursalId) {
        String baseQuery = "SELECT COUNT(p), " +
                          "SUM(CASE WHEN p.activo = true THEN 1 ELSE 0 END), " +
                          "SUM(CASE WHEN p.activo = false THEN 1 ELSE 0 END) " +
                          "FROM Producto p WHERE p.empresa.id = :empresaId";
        
        if (sucursalId != null) {
            baseQuery += " AND (p.sucursal.id = :sucursalId OR (p.sucursal IS NULL AND p.empresa.productosPorSucursal = false))";
        }
        
        TypedQuery<Object[]> query = entityManager.createQuery(baseQuery, Object[].class)
            .setParameter("empresaId", empresaId);
            
        if (sucursalId != null) {
            query.setParameter("sucursalId", sucursalId);
        }
        
        Object[] result = query.getSingleResult();
        stats.setTotal((Long) result[0]);
        stats.setActivos((Long) result[1]);
        stats.setInactivos((Long) result[2]);
    }
    
    private void obtenerEstadisticasPorTipo(ProductoEstadisticasV2Dto stats, Long empresaId, Long sucursalId) {
        String baseQuery = "SELECT p.tipo, COUNT(p) FROM Producto p " +
                          "WHERE p.empresa.id = :empresaId AND p.activo = true ";
        
        if (sucursalId != null) {
            baseQuery += "AND (p.sucursal.id = :sucursalId OR (p.sucursal IS NULL AND p.empresa.productosPorSucursal = false)) ";
        }
        
        baseQuery += "GROUP BY p.tipo";
        
        TypedQuery<Object[]> query = entityManager.createQuery(baseQuery, Object[].class)
            .setParameter("empresaId", empresaId);
            
        if (sucursalId != null) {
            query.setParameter("sucursalId", sucursalId);
        }
        
        List<Object[]> results = query.getResultList();
        
        // Inicializar mapa con todos los tipos en 0
        Map<TipoProducto, Long> porTipo = new EnumMap<>(TipoProducto.class);
        for (TipoProducto tipo : TipoProducto.values()) {
            porTipo.put(tipo, 0L);
        }
        
        // Llenar con resultados reales
        for (Object[] row : results) {
            TipoProducto tipo = (TipoProducto) row[0];
            Long cantidad = (Long) row[1];
            porTipo.put(tipo, cantidad);
        }
        
        stats.setPorTipo(porTipo);
    }
    
    private void obtenerTopCategorias(ProductoEstadisticasV2Dto stats, Long empresaId, Long sucursalId) {
        String baseQuery = "SELECT c.id, c.nombre, COUNT(p), " +
                          "(COUNT(p) * 100.0 / :total) as porcentaje " +
                          "FROM Producto p JOIN p.categorias c " +
                          "WHERE p.empresa.id = :empresaId AND p.activo = true ";
        
        if (sucursalId != null) {
            baseQuery += "AND (p.sucursal.id = :sucursalId OR (p.sucursal IS NULL AND p.empresa.productosPorSucursal = false)) ";
        }
        
        baseQuery += "GROUP BY c.id, c.nombre ORDER BY COUNT(p) DESC";
        
        TypedQuery<Object[]> query = entityManager.createQuery(baseQuery, Object[].class)
            .setParameter("empresaId", empresaId)
            .setParameter("total", stats.getActivos() > 0 ? stats.getActivos() : 1L)
            .setMaxResults(10);
            
        if (sucursalId != null) {
            query.setParameter("sucursalId", sucursalId);
        }
        
        List<Object[]> results = query.getResultList();
        List<CategoriaEstadistica> topCategorias = new ArrayList<>();
        
        for (Object[] row : results) {
            CategoriaEstadistica cat = CategoriaEstadistica.builder()
                .categoriaId((Long) row[0])
                .categoriaNombre((String) row[1])
                .cantidadProductos((Long) row[2])
                .porcentajeDelTotal(((Double) row[3]))
                .build();
            topCategorias.add(cat);
        }
        
        stats.setTopCategorias(topCategorias);
    }
    
    private void obtenerTopProductosVendidos(ProductoEstadisticasV2Dto stats, Long empresaId, 
                                           Long sucursalId, String periodo) {
        // Por ahora, retornamos lista vacía hasta tener el módulo de ventas
        // TODO: Implementar cuando tengamos tabla de ventas/ordenes
        
        log.info("Top productos vendidos pendiente de implementar - esperando módulo de ventas");
        stats.setTopProductosVendidos(new ArrayList<>());
        
        /* Ejemplo de implementación futura:
        LocalDateTime fechaInicio = calcularFechaInicio(periodo);
        
        String query = "SELECT p.id, p.codigoInterno, p.nombre, p.tipo, " +
                      "SUM(dv.cantidad) as cantidadVendida, " +
                      "SUM(dv.cantidad * dv.precioUnitario) as montoTotal, " +
                      "MAX(v.fechaCreacion) as ultimaVenta " +
                      "FROM DetalleVenta dv " +
                      "JOIN dv.producto p " +
                      "JOIN dv.venta v " +
                      "WHERE v.empresa.id = :empresaId " +
                      "AND v.fechaCreacion >= :fechaInicio " +
                      "AND v.estado = 'COMPLETADA' ";
                      
        if (sucursalId != null) {
            query += "AND v.sucursal.id = :sucursalId ";
        }
        
        query += "GROUP BY p.id, p.codigoInterno, p.nombre, p.tipo " +
                "ORDER BY cantidadVendida DESC";
        */
    }
    
    private void contarSinCategoria(ProductoEstadisticasV2Dto stats, Long empresaId, Long sucursalId) {
        String baseQuery = "SELECT COUNT(p) FROM Producto p " +
                          "WHERE p.empresa.id = :empresaId " +
                          "AND p.activo = true " +
                          "AND p.categorias IS EMPTY ";
        
        if (sucursalId != null) {
            baseQuery += "AND (p.sucursal.id = :sucursalId OR (p.sucursal IS NULL AND p.empresa.productosPorSucursal = false))";
        }
        
        TypedQuery<Long> query = entityManager.createQuery(baseQuery, Long.class)
            .setParameter("empresaId", empresaId);
            
        if (sucursalId != null) {
            query.setParameter("sucursalId", sucursalId);
        }
        
        stats.setSinCategoria(query.getSingleResult());
    }
    
    private LocalDateTime calcularFechaInicio(String periodo) {
        LocalDateTime ahora = LocalDateTime.now();
        
        return switch (periodo) {
            case "HOY" -> ahora.toLocalDate().atStartOfDay();
            case "SEMANA" -> ahora.minusDays(7);
            case "MES" -> ahora.minusDays(30);
            default -> ahora.minusYears(10); // "TOTAL" - prácticamente todo
        };
    }
}