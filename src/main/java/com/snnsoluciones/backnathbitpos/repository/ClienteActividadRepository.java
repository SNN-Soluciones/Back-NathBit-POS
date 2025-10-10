package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ClienteActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ClienteActividadRepository extends JpaRepository<ClienteActividad, Long> {

    List<ClienteActividad> findByClienteId(Long clienteId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClienteActividad ca WHERE ca.cliente.id = :clienteId")
    void deleteByClienteId(Long clienteId);
}