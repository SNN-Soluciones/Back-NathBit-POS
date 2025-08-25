package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaDocumentoRepository extends JpaRepository<FacturaDocumento, Long> {

    Optional<FacturaDocumento> findByClaveAndTipoArchivo(String clave, TipoArchivoFactura tipo);

    @Query("SELECT f FROM FacturaDocumento f " +
        "WHERE f.clave = :clave AND f.tipoArchivo = :tipo")
    Optional<FacturaDocumento> findOneByClaveAndTipoArchivo(
        @Param("clave") String clave,
        @Param("tipo") TipoArchivoFactura tipo
    );

    List<FacturaDocumento> findByClave(String clave);

    boolean existsByClaveAndTipoArchivo(String clave, TipoArchivoFactura tipo);

    Optional<FacturaDocumento> findByS3Key(String s3Key);
}