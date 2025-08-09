package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarrioRepository extends JpaRepository<Barrio, Integer> {
    
    @Query("""
    SELECT b FROM Barrio b
    WHERE b.codigoProvincia = :codigoProvincia
    AND b.codigoCanton = :codigoCanton
    AND b.codigoDistrito = :codigoDistrito
    """)
    List<Barrio> findByCodigoProvinciaAndCodigoCantonAndCodigoDistrito(
        @Param("codigoProvincia") Integer codigoProvincia,
        @Param("codigoCanton") Integer codigoCanton,
        @Param("codigoDistrito") Integer codigoDistrito
    );
    
    @Query("""
    SELECT b FROM Barrio b
    WHERE b.codigoProvincia = :codigoProvincia
    AND b.codigoCanton = :codigoCanton
    AND b.codigoDistrito = :codigoDistrito
    AND b.codigo = :codigo
    """)
    Optional<Barrio> findByCodigoProvinciaAndCodigoCantonAndCodigoDistritoAndCodigo(
        @Param("codigoProvincia") Integer codigoProvincia,
        @Param("codigoCanton") Integer codigoCanton,
        @Param("codigoDistrito") Integer codigoDistrito,
        @Param("codigo") Integer codigo
    );
}