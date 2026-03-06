package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    // ── Consultas básicas ─────────────────────────────────────────────

    List<Promocion> findByActivoTrue();

    List<Promocion> findByTipo(com.snnsoluciones.nathbitbusinesscore.model.enums.TipoPromocion tipo);

    List<Promocion> findByActivoTrueAndTipo(
        com.snnsoluciones.nathbitbusinesscore.model.enums.TipoPromocion tipo);

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

    /**
     * Busca todas las promociones activas que aplican a un producto,
     * considerando su categoría y familia.
     *
     * Una promo aplica al producto si cumple AL MENOS UNO de:
     *   1. No tiene ningún alcance definido (aplica a todo)
     *   2. El producto está en promocion_productos
     *   3. La categoría del producto está en promocion_categorias
     *   4. La familia del producto está en promocion_familias
     *
     * Además filtra por día comercial y hora si se pasan.
     * Si hora es NULL, devuelve todas las del día sin importar horario.
     */
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
            -- Sin alcance definido = aplica a todo
            (
                NOT EXISTS (SELECT 1 FROM PromocionProducto  pp WHERE pp.promocion = p)
                AND NOT EXISTS (SELECT 1 FROM PromocionCategoria pc WHERE pc.promocion = p)
                AND NOT EXISTS (SELECT 1 FROM PromocionFamilia  pf WHERE pf.promocion = p)
            )
            -- O el producto está directamente en el alcance
            OR EXISTS (
                SELECT 1 FROM PromocionProducto pp
                WHERE pp.promocion = p AND pp.productoId = :productoId
            )
            -- O la categoría del producto está en el alcance
            OR EXISTS (
                SELECT 1 FROM PromocionCategoria pc
                WHERE pc.promocion = p AND pc.categoriaId = :categoriaId
            )
            -- O la familia del producto está en el alcance
            OR EXISTS (
                SELECT 1 FROM PromocionFamilia pf
                WHERE pf.promocion = p AND pf.familiaId = :familiaId
            )
        )
        ORDER BY p.nombre ASC
        """)
    List<Promocion> findPromocionesParaProducto(
        @Param("productoId") Long      productoId,
        @Param("categoriaId") Long     categoriaId,
        @Param("familiaId")   Long     familiaId,
        @Param("dia")         String   dia,
        @Param("hora")        LocalTime hora   // NULL = sin filtro de hora
    );
}