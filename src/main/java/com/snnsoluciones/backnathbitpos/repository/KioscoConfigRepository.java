package com.snnsoluciones.backnathbitpos.repository;
 
import com.snnsoluciones.backnathbitpos.entity.KioscoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
 
@Repository
public interface KioscoConfigRepository extends JpaRepository<KioscoConfig, Long> {
    Optional<KioscoConfig> findBySucursalId(Long sucursalId);
}