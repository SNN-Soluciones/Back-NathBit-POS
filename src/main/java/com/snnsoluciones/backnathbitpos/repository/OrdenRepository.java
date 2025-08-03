// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/repository/OrdenRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.operacion.Mesa;
import com.snnsoluciones.backnathbitpos.entity.operacion.Orden;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, UUID> {

  Optional<Orden> findByNumeroOrden(String numeroOrden);

  List<Orden> findByEstado(EstadoOrden estado);

  List<Orden> findByMesa(Mesa mesa);

  List<Orden> findByMesaAndEstado(Mesa mesa, EstadoOrden estado);

  List<Orden> findByMesero(Usuario mesero);

  List<Orden> findByTipo(TipoOrden tipo);

  @Query("SELECT o FROM Orden o WHERE o.fechaOrden BETWEEN :inicio AND :fin")
  List<Orden> findByFechaBetween(@Param("inicio") LocalDateTime inicio,
      @Param("fin") LocalDateTime fin);

  @Query("SELECT o FROM Orden o WHERE o.mesa.id = :mesaId AND o.estado IN :estados")
  List<Orden> findByMesaAndEstadoIn(@Param("mesaId") UUID mesaId,
      @Param("estados") List<EstadoOrden> estados);

  @Query("SELECT o FROM Orden o WHERE o.estado = :estado ORDER BY o.fechaOrden ASC")
  List<Orden> findOrdenesPendientes(@Param("estado") EstadoOrden estado);

  @Query("SELECT COUNT(o) FROM Orden o WHERE o.mesa.id = :mesaId AND o.estado = :estado")
  Long countByMesaAndEstado(@Param("mesaId") UUID mesaId,
      @Param("estado") EstadoOrden estado);

  @Query("SELECT o FROM Orden o JOIN FETCH o.detalles WHERE o.id = :id")
  Optional<Orden> findByIdWithDetalles(@Param("id") UUID id);

  Page<Orden> findByEstadoIn(List<EstadoOrden> estados, Pageable pageable);

  @Query("SELECT o FROM Orden o WHERE " +
      "(:numeroOrden IS NULL OR o.numeroOrden LIKE %:numeroOrden%) AND " +
      "(:estado IS NULL OR o.estado = :estado) AND " +
      "(:tipo IS NULL OR o.tipo = :tipo) AND " +
      "(:meseroId IS NULL OR o.mesero.id = :meseroId) AND " +
      "(:fechaInicio IS NULL OR o.fechaOrden >= :fechaInicio) AND " +
      "(:fechaFin IS NULL OR o.fechaOrden <= :fechaFin)")
  Page<Orden> buscarOrdenes(@Param("numeroOrden") String numeroOrden,
      @Param("estado") EstadoOrden estado,
      @Param("tipo") TipoOrden tipo,
      @Param("meseroId") UUID meseroId,
      @Param("fechaInicio") LocalDateTime fechaInicio,
      @Param("fechaFin") LocalDateTime fechaFin,
      Pageable pageable);
}