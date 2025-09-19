// ProductoCompuestoRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCompuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoCompuestoRepository extends JpaRepository<ProductoCompuesto, Long> {
    Optional<ProductoCompuesto> findByProductoId(Long productoId);
    boolean existsByProductoId(Long productoId);
}