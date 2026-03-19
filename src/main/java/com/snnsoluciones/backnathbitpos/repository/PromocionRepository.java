package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Promocion;
import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    // =========================================================================
    // MOTOR — carga alcance completo para evaluar promos en una orden
    // =========================================================================

    /**
     * Carga todas las promos activas de la empresa/sucursal con su alcance
     * (productos, categorías, familias) en una sola query para el motor.
     *
     * Regla de sucursal:
     *   sucursal IS NULL  → aplica a toda la empresa
     *   sucursal.id = X   → solo aplica a esa sucursal
     */
    @Query("""
    SELECT DISTINCT p FROM Promocion p
    LEFT JOIN FETCH p.productos
    WHERE p.activo = true
    AND p.empresa.id = :empresaId
    AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
""")
    List<Promocion> findActivasConProductos(
        @Param("empresaId")  Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    // ✅ Query 2: promos + categorias
    @Query("""
    SELECT DISTINCT p FROM Promocion p
    LEFT JOIN FETCH p.categorias
    WHERE p.activo = true
    AND p.empresa.id = :empresaId
    AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
""")
    List<Promocion> findActivasConCategorias(
        @Param("empresaId")  Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    // ✅ Query 3: promos + familias
    @Query("""
    SELECT DISTINCT p FROM Promocion p
    LEFT JOIN FETCH p.familias
    WHERE p.activo = true
    AND p.empresa.id = :empresaId
    AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
""")
    List<Promocion> findActivasConFamilias(
        @Param("empresaId")  Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    // =========================================================================
    // CONSULTAS BÁSICAS
    // =========================================================================

    List<Promocion> findByEmpresaIdAndActivoTrue(Long empresaId);

    List<Promocion> findByEmpresaIdAndTipo(Long empresaId, TipoPromocion tipo);

    List<Promocion> findByEmpresaIdAndActivoTrueAndTipo(Long empresaId, TipoPromocion tipo);

    // =========================================================================
    // POR DÍA
    // =========================================================================

    @Query("""
        SELECT p FROM Promocion p
        WHERE p.activo = true
        AND p.empresa.id = :empresaId
        AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
        AND (
            (:dia = 'LUNES'     AND p.lunes     = true) OR
            (:dia = 'MARTES'    AND p.martes    = true) OR
            (:dia = 'MIERCOLES' AND p.miercoles = true) OR
            (:dia = 'JUEVES'    AND p.jueves    = true) OR
            (:dia = 'VIERNES'   AND p.viernes   = true) OR
            (:dia = 'SABADO'    AND p.sabado    = true) OR
            (:dia = 'DOMINGO'   AND p.domingo   = true)
        )
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findActivasByDia(
        @Param("empresaId")  Long   empresaId,
        @Param("sucursalId") Long   sucursalId,
        @Param("dia")        String dia
    );

    // =========================================================================
    // POR DÍA Y HORA
    // =========================================================================

    /**
     * Promos activas para un día y hora específicos.
     * horaInicio IS NULL = aplica todo el día comercial.
     */
    @Query("""
        SELECT p FROM Promocion p
        WHERE p.activo = true
        AND p.empresa.id = :empresaId
        AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
        AND (
            (:dia = 'LUNES'     AND p.lunes     = true) OR
            (:dia = 'MARTES'    AND p.martes    = true) OR
            (:dia = 'MIERCOLES' AND p.miercoles = true) OR
            (:dia = 'JUEVES'    AND p.jueves    = true) OR
            (:dia = 'VIERNES'   AND p.viernes   = true) OR
            (:dia = 'SABADO'    AND p.sabado    = true) OR
            (:dia = 'DOMINGO'   AND p.domingo   = true)
        )
        AND (
            p.horaInicio IS NULL
            OR (:hora >= p.horaInicio AND :hora <= p.horaFin)
        )
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findActivasByDiaYHora(
        @Param("empresaId")  Long      empresaId,
        @Param("sucursalId") Long      sucursalId,
        @Param("dia")        String    dia,
        @Param("hora")       LocalTime hora
    );

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    boolean existsByEmpresaIdAndNombreIgnoreCase(Long empresaId, String nombre);

    /**
     * Carga la promo con sus items precargados (evita N+1).
     * Valida que pertenezca a la empresa correcta.
     */
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        LEFT JOIN FETCH p.items
        WHERE p.id = :id
        AND p.empresa.id = :empresaId
        """)
    Optional<Promocion> findByIdWithItems(
        @Param("id")        Long id,
        @Param("empresaId") Long empresaId
    );

    /**
     * Todas las promos activas con items precargados.
     */
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        LEFT JOIN FETCH p.items
        WHERE p.activo = true
        AND p.empresa.id = :empresaId
        AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findActivasWithItems(
        @Param("empresaId")  Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    // =========================================================================
    // BÚSQUEDA POR PRODUCTO (para validación en el frontend)
    // =========================================================================

    /**
     * Promos activas que aplican a un producto específico, considerando
     * su categoría, familia, día y hora.
     *
     * Un producto califica si la promo:
     *   - No tiene alcance definido (aplica a todo), O
     *   - Tiene el producto explícito en su alcance, O
     *   - Tiene la categoría del producto, O
     *   - Tiene la familia del producto
     */
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        LEFT JOIN FETCH p.items
        WHERE p.activo = true
        AND p.empresa.id = :empresaId
        AND (p.sucursal IS NULL OR p.sucursal.id = :sucursalId)
        AND (
            (:dia = 'LUNES'     AND p.lunes     = true) OR
            (:dia = 'MARTES'    AND p.martes    = true) OR
            (:dia = 'MIERCOLES' AND p.miercoles = true) OR
            (:dia = 'JUEVES'    AND p.jueves    = true) OR
            (:dia = 'VIERNES'   AND p.viernes   = true) OR
            (:dia = 'SABADO'    AND p.sabado    = true) OR
            (:dia = 'DOMINGO'   AND p.domingo   = true)
        )
        AND (
            :hora IS NULL
            OR p.horaInicio IS NULL
            OR (:hora >= p.horaInicio AND :hora <= p.horaFin)
        )
        AND (
            (
                NOT EXISTS (SELECT 1 FROM PromocionProducto  pp WHERE pp.promocion = p)
                AND NOT EXISTS (SELECT 1 FROM PromocionCategoria pc WHERE pc.promocion = p)
                AND NOT EXISTS (SELECT 1 FROM PromocionFamilia  pf WHERE pf.promocion = p)
            )
            OR EXISTS (
                SELECT 1 FROM PromocionProducto pp
                WHERE pp.promocion = p AND pp.productoId = :productoId
            )
            OR EXISTS (
                SELECT 1 FROM PromocionCategoria pc
                WHERE pc.promocion = p AND pc.categoriaId = :categoriaId
            )
            OR EXISTS (
                SELECT 1 FROM PromocionFamilia pf
                WHERE pf.promocion = p AND pf.familiaId = :familiaId
            )
        )
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findPromocionesParaProducto(
        @Param("empresaId")  Long      empresaId,
        @Param("sucursalId") Long      sucursalId,
        @Param("productoId") Long      productoId,
        @Param("categoriaId") Long     categoriaId,
        @Param("familiaId")  Long      familiaId,
        @Param("dia")        String    dia,
        @Param("hora")       LocalTime hora
    );
}