package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Promocion;
import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    // En PromocionRepository.java — agregar este método:
    @Query("""
    SELECT DISTINCT p FROM Promocion p
    LEFT JOIN FETCH p.productos
    LEFT JOIN FETCH p.categorias
    LEFT JOIN FETCH p.familias
    WHERE p.activo = true
""")
    List<Promocion> findActivasConAlcance();

    // ── Consultas básicas ─────────────────────────────────────────────

    List<Promocion> findByActivoTrue();

    List<Promocion> findByTipo(TipoPromocion tipo);

    List<Promocion> findByActivoTrueAndTipo(
        TipoPromocion tipo);

    // ── Consultas por día ─────────────────────────────────────────────
    // Útil para que el frontend pida "promos activas de hoy"

    @Query("""
        SELECT p FROM Promocion p
        WHERE p.activo = true
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
    List<Promocion> findActivasByDia(@Param("dia") String dia);

    /**
     * Promos activas para un día y hora específicos.
     * Cubre también el caso donde horaInicio/horaFin son NULL (aplica todo el día).
     */
    @Query("""
        SELECT p FROM Promocion p
        WHERE p.activo = true
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
        @Param("dia")  String    dia,
        @Param("hora") LocalTime hora
    );

    // ── Utilidades ────────────────────────────────────────────────────

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Carga la promo con sus items en una sola query (evita N+1).
     */
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        LEFT JOIN FETCH p.items
        WHERE p.id = :id
        """)
    java.util.Optional<Promocion> findByIdWithItems(@Param("id") Long id);

    /**
     * Todas las promos activas con sus items precargados.
     */
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        LEFT JOIN FETCH p.items
        WHERE p.activo = true
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findActivasWithItems();

    @Query("""
    SELECT DISTINCT p FROM Promocion p
    LEFT JOIN FETCH p.items
    WHERE p.activo = true
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
        @Param("productoId")  Long      productoId,
        @Param("categoriaId") Long      categoriaId,
        @Param("familiaId")   Long      familiaId,
        @Param("dia")         String    dia,
        @Param("hora")        LocalTime hora
    );
}