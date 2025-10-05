package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MetricaCompraMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricaCompraMensualRepository extends JpaRepository<MetricaCompraMensual, Long> {

    Optional<MetricaCompraMensual> findByEmpresaIdAndSucursalIdAndAnioAndMes(
        Long empresaId,
        Long sucursalId,
        Integer anio,
        Integer mes
    );
}