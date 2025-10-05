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
        String serviciosQuery = "SELECT COUNT(p) FROM Producto p WHERE p.empresa.id = :empresaId AND p.esServicio = true AND p.activo = true";
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