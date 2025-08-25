package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumentoHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.IndEstadoHacienda;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaDocumentoHaciendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaDocumentoHaciendaServiceImpl implements FacturaDocumentoHaciendaService {

    private final FacturaDocumentoHaciendaRepository repo;

    @Override
    @Transactional
    public FacturaDocumentoHacienda crearSiNoExiste(Long facturaId, String clave, AmbienteHacienda ambiente) {
        return repo.findByClave(clave).orElseGet(() -> {
            FacturaDocumentoHacienda d = FacturaDocumentoHacienda.builder()
                    .facturaId(facturaId)
                    .clave(clave)
                    .ambiente(ambiente)
                    .indEstado(IndEstadoHacienda.EN_PROCESO)
                    .reintentosConsulta(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return repo.save(d);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FacturaDocumentoHacienda> findByClave(String clave) {
        return repo.findByClave(clave);
    }

    @Override
    @Transactional
    public void registrarRecepcion(String clave, String location, String ticket, Integer httpStatus) {
        FacturaDocumentoHacienda d = require(clave);
        d.setRecepcionLocation(location);
        d.setRecepcionTicket(ticket);
        d.setHttpStatusUltimo(httpStatus);
        d.setFechaEnvio(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void actualizarXmlFirmado(String clave, String s3KeyXmlFirmado) {
        FacturaDocumentoHacienda d = require(clave);
        d.setS3KeyXmlFirmado(s3KeyXmlFirmado);
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void actualizarXmlRespuesta(String clave, String s3KeyXmlRespuesta) {
        FacturaDocumentoHacienda d = require(clave);
        d.setS3KeyXmlRespuesta(s3KeyXmlRespuesta);
        d.setFechaEstado(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void actualizarMensajeReceptor(String clave, String s3KeyMensajeReceptor) {
        FacturaDocumentoHacienda d = require(clave);
        d.setS3KeyMensajeReceptor(s3KeyMensajeReceptor);
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void actualizarEstado(String clave, IndEstadoHacienda indEstado, String codError, String detalleError) {
        FacturaDocumentoHacienda d = require(clave);
        d.setIndEstado(indEstado);
        d.setCodigoError(codError);
        d.setDetalleError(detalleError);
        d.setFechaEstado(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void setFechaEnvio(String clave, LocalDateTime fechaEnvio) {
        FacturaDocumentoHacienda d = require(clave);
        d.setFechaEnvio(fechaEnvio);
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void setFechaEstado(String clave, LocalDateTime fechaEstado) {
        FacturaDocumentoHacienda d = require(clave);
        d.setFechaEstado(fechaEstado);
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    @Override
    @Transactional
    public void agendarProximaConsulta(String clave, LocalDateTime proxima, int reintentosAcumulados, Integer ultimoHttpStatus) {
        FacturaDocumentoHacienda d = require(clave);
        d.setProximaConsulta(proxima);
        d.setReintentosConsulta(reintentosAcumulados);
        d.setHttpStatusUltimo(ultimoHttpStatus);
        d.setUpdatedAt(LocalDateTime.now());
        repo.save(d);
    }

    private FacturaDocumentoHacienda require(String clave) {
        return repo.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Documento Hacienda no encontrado para clave: " + clave));
    }
}