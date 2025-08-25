package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;

import java.util.List;
import java.util.Optional;

public interface FacturaDocumentoService {

    // Registrar artefacto genérico (PDF, XML_UNSIGNED, XML_SIGNED, etc.)
    FacturaDocumento registrarDocumento(Long facturaId,
                                        String clave,
                                        TipoArchivoFactura tipo,
                                        String s3Bucket,
                                        String s3Key,
                                        long tamanioBytes,
                                        String contentType);

    // Búsquedas típicas
    Optional<FacturaDocumento> findById(Long id);
    Optional<FacturaDocumento> findByClaveAndTipo(String clave, TipoArchivoFactura tipo);
    List<FacturaDocumento> findByClave(String clave);
    Optional<FacturaDocumento> findByS3Key(String s3Key);

    boolean existsByClaveAndTipo(String clave, TipoArchivoFactura tipo);

    // Updates puntuales
    void actualizarS3(Long id, String bucket, String key, long tamanioBytes, String contentType);
    void eliminarFisicoYLogico(Long id); // opcional si implementas borrado en S3 + DB
}