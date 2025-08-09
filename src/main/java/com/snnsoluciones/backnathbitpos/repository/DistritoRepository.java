package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Integer> {
    
    @Query("""
    SELECT d FROM Distrito d
    WHERE d.codigoProvincia = :codigoProvincia
    AND d.codigoCanton = :codigoCanton
    """)
    List<Distrito> findByCodigoProvinciaAndCodigoCanton(
        @Param("codigoProvincia") Integer codigoProvincia,
        @Param("codigoCanton") Integer codigoCanton
    );
    
    @Query("""
    SELECT d FROM Distrito d
    WHERE d.codigoProvincia = :codigoProvincia
    AND d.codigoCanton = :codigoCanton
    AND d.codigo = :codigo
    """)
    Optional<Distrito> findByCodigoProvinciaAndCodigoCantonAndCodigo(
        @Param("codigoProvincia") Integer codigoProvincia,
        @Param("codigoCanton") Integer codigoCanton,
        @Param("codigo") Integer codigo
    );
}