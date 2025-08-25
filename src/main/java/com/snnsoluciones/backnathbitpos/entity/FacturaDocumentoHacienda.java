package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.IndEstadoHacienda;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "factura_documento_hacienda",
    indexes = {
        @Index(name = "ix_hda_clave", columnList = "clave", unique = true),
        @Index(name = "ix_hda_ind_estado", columnList = "ind_estado"),
        @Index(name = "ix_hda_proxima_consulta", columnList = "proxima_consulta")
    })
public class FacturaDocumentoHacienda {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "clave", nullable = false, length = 50)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    private AmbienteHacienda ambiente;

    // Estado de la DGT: ACEPTADO / RECHAZADO / EN_PROCESO
    @Enumerated(EnumType.STRING)
    @Column(name = "ind_estado", nullable = false, length = 20)
    private IndEstadoHacienda indEstado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clave", referencedColumnName = "clave", insertable = false, updatable = false)
    private Factura factura;

    // Datos de recepción/trace
    @Column(name = "recepcion_location", length = 512)
    private String recepcionLocation;

    @Column(name = "recepcion_ticket", length = 128)
    private String recepcionTicket; // si aplica

    @Column(name = "http_status_ultimo")
    private Integer httpStatusUltimo;

    // Artefactos S3 relacionados a Hacienda
    @Column(name = "s3_key_xml_firmado", length = 512)
    private String s3KeyXmlFirmado;

    @Column(name = "s3_key_xml_respuesta", length = 512)
    private String s3KeyXmlRespuesta;

    @Column(name = "s3_key_mensaje_receptor", length = 512)
    private String s3KeyMensajeReceptor; // si lo guardas

    // Tiempos y reintentos/polling
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_estado")
    private LocalDateTime fechaEstado;

    @Column(name = "reintentos_consulta", nullable = false)
    private int reintentosConsulta;

    @Column(name = "proxima_consulta")
    private LocalDateTime proximaConsulta;

    // Errores/textos de diagnóstico (no guardar XML aquí)
    @Column(name = "codigo_error", length = 60)
    private String codigoError;

    @Column(name = "detalle_error", length = 2000)
    private String detalleError;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (indEstado == null) indEstado = IndEstadoHacienda.EN_PROCESO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}