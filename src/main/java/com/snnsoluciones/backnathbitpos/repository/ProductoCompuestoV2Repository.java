package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoV2;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoCompuestoV2Repository extends JpaRepository<ProductoCompuestoV2, Long> {
    Optional<ProductoCompuestoV2> findByProductoId(Long productoId);
    boolean existsByProductoId(Long productoId);
}