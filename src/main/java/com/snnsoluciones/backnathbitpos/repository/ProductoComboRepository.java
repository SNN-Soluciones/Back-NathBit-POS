// ProductoComboRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoComboRepository extends JpaRepository<ProductoCombo, Long> {
    Optional<ProductoCombo> findByProductoId(Long productoId);
    boolean existsByProductoId(Long productoId);
}