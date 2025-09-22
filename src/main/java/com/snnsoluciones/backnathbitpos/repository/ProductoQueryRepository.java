package com.snnsoluciones.backnathbitpos.repository;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated(since = "2.0", forRemoval = true)
@Repository
public class ProductoQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Busca productos con filtros múltiples
     */
    public List<Object[]> buscarProductosConFiltros(Long empresaId, Long categoriaId,
        BigDecimal precioMin, BigDecimal precioMax,
        Boolean esServicio, Boolean aplicaServicio,
        String busqueda, int limit, int offset) {

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p.id, p.codigoInterno, p.nombre, p.precioVenta, ");
        jpql.append("c.nombre, m.simbolo, p.esServicio, p.activo ");
        jpql.append("FROM Producto p ");
        jpql.append("LEFT JOIN p.categoria c ");
        jpql.append("LEFT JOIN p.moneda m ");
        jpql.append("WHERE p.empresa.id = :empresaId ");

        Map<String, Object> params = new HashMap<>();
        params.put("empresaId", empresaId);

        if (categoriaId != null) {
            jpql.append("AND p.categoria.id = :categoriaId ");
            params.put("categoriaId", categoriaId);
        }

        if (precioMin != null) {
            jpql.append("AND p.precioVenta >= :precioMin ");
            params.put("precioMin", precioMin);
        }

        if (precioMax != null) {
            jpql.append("AND p.precioVenta <= :precioMax ");
            params.put("precioMax", precioMax);
        }

        if (esServicio != null) {
            jpql.append("AND p.esServicio = :esServicio ");
            params.put("esServicio", esServicio);
        }

        if (aplicaServicio != null) {
            jpql.append("AND p.aplicaServicio = :aplicaServicio ");
            params.put("aplicaServicio", aplicaServicio);
        }

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            jpql.append("AND (LOWER(p.nombre) LIKE :busqueda ");
            jpql.append("OR LOWER(p.codigoInterno) LIKE :busqueda ");
            jpql.append("OR LOWER(p.codigoBarras) LIKE :busqueda) ");
            params.put("busqueda", "%" + busqueda.toLowerCase() + "%");
        }

        jpql.append("ORDER BY p.nombre");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    /**
     * Estadísticas de productos por empresa
     */
    public Map<String, Object> obtenerEstadisticasProductos(Long empresaId) {
        Map<String, Object> stats = new HashMap<>();

        // Total productos
        String totalQuery = "SELECT COUNT(p) FROM Producto p WHERE p.empresa.id = :empresaId AND p.activo = true";
        Long total = entityManager.createQuery(totalQuery, Long.class)
            .setParameter("empresaId", empresaId)
            .getSingleResult();
        stats.put("totalProductos", total);

        // Total servicios
        String serviciosQuery = "SELECT COUNT(p) FROM Producto p WHERE p.empresa.id = :empresaId AND p.aplicaServicio = true AND p.activo = true";
        Long servicios = entityManager.createQuery(serviciosQuery, Long.class)
            .setParameter("empresaId", empresaId)
            .getSingleResult();
        stats.put("totalServicios", servicios);

        // Productos por categoría
        String categoriaQuery = "SELECT c.nombre, COUNT(p) FROM Producto p " +
            "JOIN p.categorias c " +
            "WHERE p.empresa.id = :empresaId AND p.activo = true " +
            "GROUP BY c.id, c.nombre " +
            "ORDER BY COUNT(p) DESC";
        List<Object[]> porCategoria = entityManager.createQuery(categoriaQuery, Object[].class)
            .setParameter("empresaId", empresaId)
            .setMaxResults(10)
            .getResultList();
        stats.put("productosPorCategoria", porCategoria);

        // Productos sin categoría
        String sinCategoriaQuery = "SELECT COUNT(p) FROM Producto p WHERE p.empresa.id = :empresaId AND p.categorias.size IS NULL AND p.activo = true";
        Long sinCategoria = entityManager.createQuery(sinCategoriaQuery, Long.class)
            .setParameter("empresaId", empresaId)
            .getSingleResult();
        stats.put("productosSinCategoria", sinCategoria);

        return stats;
    }
}