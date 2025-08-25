package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumento;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaDocumentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaDocumentoServiceImpl implements FacturaDocumentoService {

    private final FacturaDocumentoRepository repo;

    @Override
    @Transactional
    public FacturaDocumento registrarDocumento(Long facturaId,
                                               String clave,
                                               TipoArchivoFactura tipo,
                                               String s3Bucket,
                                               String s3Key,
                                               long tamanioBytes,
                                               String contentType) {
        FacturaDocumento d = new FacturaDocumento();
        d.setFacturaId(facturaId);
        d.setClave(clave);
        d.setTipoArchivo(tipo);
        d.setS3Bucket(s3Bucket);
        d.setS3Key(s3Key);
        d.setTamanio(tamanioBytes);
        d.setCreatedAt(LocalDateTime.now());
        return repo.save(d);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaDocumento> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaDocumento> findByClaveAndTipo(String clave, TipoArchivoFactura tipo) {
        return repo.findByClaveAndTipoArchivo(clave, tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaDocumento> findByClave(String clave) {
        return repo.findByClave(clave);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaDocumento> findByS3Key(String s3Key) {
        return repo.findByS3Key(s3Key);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClaveAndTipo(String clave, TipoArchivoFactura tipo) {
        return repo.existsByClaveAndTipoArchivo(clave, tipo);
    }

    @Override
    @Transactional
    public void actualizarS3(Long id, String bucket, String key, long tamanioBytes, String contentType) {
        FacturaDocumento d = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + id));
        d.setS3Bucket(bucket);
        d.setS3Key(key);
        d.setTamanio(tamanioBytes);
        repo.save(d);
    }

    @Override
    @Transactional
    public void eliminarFisicoYLogico(Long id) {
        // Si luego agregas un StorageService.delete(...), invócalo antes de borrar la fila.
        // Por ahora, eliminamos solo en BD.
        if (!repo.existsById(id)) return;
        repo.deleteById(id);
        log.info("Documento {} eliminado (BD). Asegúrate de borrar el archivo en S3 si aplica.", id);
    }
}