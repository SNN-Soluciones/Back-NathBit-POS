package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TipoCodigoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCodigoProductoRepository extends JpaRepository<TipoCodigoProducto, Long> {
    
    Optional<TipoCodigoProducto> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    List<TipoCodigoProducto> findByActivoTrueOrderByCodigo();
    
    // Código interno (más usado)
    @Query("SELECT t FROM TipoCodigoProducto t WHERE t.codigo = '04' AND t.activo = true")
    Optional<TipoCodigoProducto> findCodigoInterno();
    
    // Código de barras
    @Query("SELECT t FROM TipoCodigoProducto t WHERE t.codigo = '03' AND t.activo = true")
    Optional<TipoCodigoProducto> findCodigoBarras();
    
    // Código del vendedor
    @Query("SELECT t FROM TipoCodigoProducto t WHERE t.codigo = '01' AND t.activo = true")
    Optional<TipoCodigoProducto> findCodigoVendedor();
}