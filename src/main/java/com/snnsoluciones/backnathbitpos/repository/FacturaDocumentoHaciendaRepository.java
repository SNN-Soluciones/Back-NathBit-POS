package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaDocumentoHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.IndEstadoHacienda;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaDocumentoHaciendaRepository extends JpaRepository<FacturaDocumentoHacienda, Long> {

    // Búsquedas básicas
    Optional<FacturaDocumentoHacienda> findByClave(String clave);
    Optional<FacturaDocumentoHacienda> findByFacturaId(Long facturaId);

    // Para evitar carreras al procesar la misma clave en paralelo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from FacturaDocumentoHacienda d where d.clave = :clave")
    Optional<FacturaDocumentoHacienda> lockByClave(@Param("clave") String clave);

    // Pendientes de consulta a Hacienda (EN_PROCESO y ya venció la proxima_consulta)
    @Query("""
           select d
             from FacturaDocumentoHacienda d
            where d.indEstado = 'EN_PROCESO'
              and d.proximaConsulta <= :ahora
           order by d.proximaConsulta asc
           """)
    List<FacturaDocumentoHacienda> findPendientesConsulta(@Param("ahora") LocalDateTime ahora);

    // === Updates puntuales (propiedades reales de tu entidad) ===

    // Guardar ruta del XML firmado en S3
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.s3KeyXmlFirmado = :key,
                  d.fechaEnvio     = :fechaEnvio,
                  d.updatedAt      = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int updateXmlFirmado(@Param("clave") String clave,
        @Param("key") String s3KeyXmlFirmado,
        @Param("fechaEnvio") LocalDateTime fechaEnvio);

    // Guardar ruta de la respuesta + estado y http status
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.s3KeyXmlRespuesta = :key,
                  d.indEstado         = :indEstado,
                  d.httpStatusUltimo  = :httpStatus,
                  d.fechaEstado       = CURRENT_TIMESTAMP,
                  d.updatedAt         = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int updateRespuesta(@Param("clave") String clave,
        @Param("key") String s3KeyXmlRespuesta,
        @Param("indEstado") IndEstadoHacienda indEstado,
        @Param("httpStatus") Integer httpStatusUltimo);

    // Marcar aceptado (con respuesta ya subida)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.indEstado        = 'ACEPTADO',
                  d.s3KeyXmlRespuesta= :key,
                  d.httpStatusUltimo = :httpStatus,
                  d.fechaEstado      = CURRENT_TIMESTAMP,
                  d.updatedAt        = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int markAceptado(@Param("clave") String clave,
        @Param("key") String s3KeyXmlRespuesta,
        @Param("httpStatus") Integer httpStatusUltimo);

    // Marcar rechazado + diagnóstico (con respuesta ya subida)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.indEstado        = 'RECHAZADO',
                  d.s3KeyXmlRespuesta= :key,
                  d.httpStatusUltimo = :httpStatus,
                  d.codigoError      = :codigo,
                  d.detalleError     = :detalle,
                  d.fechaEstado      = CURRENT_TIMESTAMP,
                  d.updatedAt        = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int markRechazado(@Param("clave") String clave,
        @Param("key") String s3KeyXmlRespuesta,
        @Param("httpStatus") Integer httpStatusUltimo,
        @Param("codigo") String codigoError,
        @Param("detalle") String detalleError);

    // Guardar datos de recepción (Location/Ticket) y dejar EN_PROCESO
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.recepcionLocation = :location,
                  d.recepcionTicket   = :ticket,
                  d.indEstado         = 'EN_PROCESO',
                  d.httpStatusUltimo  = :httpStatus,
                  d.fechaEstado       = CURRENT_TIMESTAMP,
                  d.updatedAt         = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int markRecepcion(@Param("clave") String clave,
        @Param("location") String recepcionLocation,
        @Param("ticket") String recepcionTicket,
        @Param("httpStatus") Integer httpStatusUltimo);

    // Programar siguiente consulta (backoff) e incrementar contador
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update FacturaDocumentoHacienda d
              set d.proximaConsulta   = :proxima,
                  d.reintentosConsulta = :reintentos,
                  d.updatedAt          = CURRENT_TIMESTAMP
            where d.clave = :clave
           """)
    int scheduleNextPoll(@Param("clave") String clave,
        @Param("proxima") LocalDateTime proxima,
        @Param("reintentos") int reintentosConsulta);
}