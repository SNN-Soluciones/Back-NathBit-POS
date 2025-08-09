package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Canton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CantonRepository extends JpaRepository<Canton, Integer> {
    
    List<Canton> findByCodigoProvincia(Integer codigoProvincia);
    
    @Query("""
    SELECT c FROM Canton c
    WHERE c.codigoProvincia = :codigoProvincia
    AND c.codigo = :codigo
    """)
    Optional<Canton> findByCodigoProvinciaAndCodigo(
        @Param("codigoProvincia") Integer codigoProvincia,
        @Param("codigo") Integer codigo
    );
}