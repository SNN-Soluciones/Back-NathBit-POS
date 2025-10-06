package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestión de bitácora de facturación electrónica
 */
@Repository
public interface FacturaBitacoraRepository extends JpaRepository<FacturaBitacora, Long>,
    JpaSpecificationExecutor<FacturaBitacora> {

  @Query("""
      select b.id as id,
             b.facturaId as facturaId,
             b.clave as clave,
             b.xmlFirmadoPath as xmlFirmadoPath,
             b.xmlRespuestaPath as xmlRespuestaPath
      from FacturaBitacora b
        join Factura f on f.id = b.facturaId
      where b.estado = com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora.ACEPTADA
        and b.createdAt between :desde and :hasta
        and f.tipoDocumento = com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento.FACTURA_ELECTRONICA
    """)
  List<BitacoraMin> findAceptadasTipoFacturaBetween(LocalDateTime desde, LocalDateTime hasta);

  interface BitacoraMin {
    Long getId();
    Long getFacturaId();
    String getClave();
    String getXmlFirmadoPath();
    String getXmlRespuestaPath();
  }

  @Query("SELECT b FROM FacturaBitacora b WHERE " +
      "(b.estado = 'PENDIENTE' OR b.estado = 'ERROR') AND " +
      "(b.proximoIntento IS NULL OR b.proximoIntento <= :ahora) AND " +
      "b.intentos < 3 ORDER BY b.createdAt")
  List<FacturaBitacora> findFacturasPendientesProcesar(
      @Param("ahora") LocalDateTime ahora,
      Pageable pageable
  );

  Optional<FacturaBitacora> findByClave(String clave);

  Optional<FacturaBitacora> findByFacturaId(Long facturaId);

}