package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumentoHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.IndEstadoHacienda;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FacturaDocumentoHaciendaService {

    // Crear/obtener por clave (1–1 por clave)
    FacturaDocumentoHacienda crearSiNoExiste(Long facturaId, String clave, AmbienteHacienda ambiente);
    Optional<FacturaDocumentoHacienda> findByClave(String clave);

    // Recepción inicial (POST /recepcion)
    void registrarRecepcion(String clave, String location, String ticket, Integer httpStatus);

    // Artefactos
    void actualizarXmlFirmado(String clave, String s3KeyXmlFirmado);
    void actualizarXmlRespuesta(String clave, String s3KeyXmlRespuesta);
    void actualizarMensajeReceptor(String clave, String s3KeyMensajeReceptor);

    // Estado DGT + auditoría
    void actualizarEstado(String clave, IndEstadoHacienda indEstado, String codError, String detalleError);
    void setFechaEnvio(String clave, LocalDateTime fechaEnvio);
    void setFechaEstado(String clave, LocalDateTime fechaEstado);

    // Polling / reintentos
    void agendarProximaConsulta(String clave, LocalDateTime proxima, int reintentosAcumulados, Integer ultimoHttpStatus);
}