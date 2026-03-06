package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OrdenItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long> {

  @Modifying
  @Query("UPDATE OrdenItem i SET i.orden.id = :ordenDestinoId WHERE i.orden.id = :ordenOrigenId")
  int reasignarItemsAOrden(@Param("ordenOrigenId") Long ordenOrigenId,
      @Param("ordenDestinoId") Long ordenDestinoId);
    
}