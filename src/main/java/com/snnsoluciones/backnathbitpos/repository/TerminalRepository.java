package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    Optional<Terminal> findFirstBySucursalId(Long sucursalId);


    // Buscar terminales por sucursal
    List<Terminal> findBySucursalId(Long sucursalId);
    

    // Contar terminales activas por sucursal
    @Query("SELECT COUNT(t) FROM Terminal t WHERE t.sucursal.id = :sucursalId AND t.activa = true")
    long countActivasBySucursalId(@Param("sucursalId") Long sucursalId);
    
    // Verificar si existe terminal ocupada (con sesión abierta)
    @Query("""
    SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END
    FROM SesionCaja sc
    WHERE sc.terminal.id = :terminalId
    AND sc.estado = 'ABIERTA'
    """)
    boolean isTerminalOcupada(@Param("terminalId") Long terminalId);
    
    // Obtener el máximo número de terminal por sucursal
    @Query("""
    SELECT MAX(CAST(t.numeroTerminal AS integer))
    FROM Terminal t
    WHERE t.sucursal.id = :sucursalId
    """)
    Integer findMaxNumeroTerminalBySucursalId(@Param("sucursalId") Long sucursalId);
}