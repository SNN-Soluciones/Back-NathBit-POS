//package com.snnsoluciones.backnathbitpos.repository;
//
//import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionAutomatica;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//public interface FacturaRecepcionAutomaticaRepository extends JpaRepository<FacturaRecepcionAutomatica, Long> {
//
//    boolean existsByClave(String clave);
//
//    Page<FacturaRecepcionAutomatica> findBySucursalIdAndConvertidaACompra(
//            Long sucursalId, boolean convertida, Pageable pageable);
//
//    @Query("SELECT f FROM FacturaRecepcionAutomatica f " +
//           "WHERE f.empresa.id = :empresaId AND f. = :convertida")
//    Page<FacturaRecepcionAutomatica> findByEmpresaIdAndConvertidaACompra(
//            @Param("empresaId") Long empresaId,
//            @Param("convertida") boolean convertida,
//            Pageable pageable);
//
//    Page<FacturaRecepcionAutomatica> findByConvertidaACompra(boolean convertida, Pageable pageable);
//}