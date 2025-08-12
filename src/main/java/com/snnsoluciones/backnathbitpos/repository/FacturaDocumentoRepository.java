package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaDocumentoRepository extends JpaRepository<FacturaDocumento, Long> {
    
    // Buscar documento específico
    Optional<FacturaDocumento> findByClaveAndTipoArchivo(String clave, TipoArchivoFactura tipo);
    
    // Todos los documentos de una factura
    List<FacturaDocumento> findByClave(String clave);
    
    // Verificar si ya existe un tipo de documento para una factura
    boolean existsByClaveAndTipoArchivo(String clave, TipoArchivoFactura tipo);
}