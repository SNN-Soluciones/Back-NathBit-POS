// ProductoComboItemRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoComboItemRepository extends JpaRepository<ProductoComboItem, Long> {
    List<ProductoComboItem> findByComboIdOrderByOrden(Long comboId);
    void deleteByComboId(Long comboId);
}