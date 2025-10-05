package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Auditoría de emails enviados para facturas electrónicas
 * Registra intentos, errores y estado de envío
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_audit_log",
    indexes = {
        @Index(name = "idx_email_audit_factura", columnList = "factura_id"),
        @Index(name = "idx_email_audit_clave", columnList = "clave"),
        @Index(name = "idx_email_audit_estado", columnList = "estado")
    })
public class EmailAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "clave", nullable = false, length = 50)
    private String clave;

    @Column(name = "email_destino", nullable = false)
    private String emailDestino;

    @Column(name = "asunto", length = 500)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoEmail estado;

    @Builder.Default
    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "error_tipo", length = 100)
    private String errorTipo; // TRANSITORIO, PERMANENTE, AUTENTICACION

    // Metadata de los archivos adjuntos
    @Column(name = "adjunto_pdf_size")
    private Long adjuntoPdfSize;

    @Column(name = "adjunto_xml_size")
    private Long adjuntoXmlSize;

    @Column(name = "adjunto_respuesta_size")
    private Long adjuntoRespuestaSize;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (intentos == null) intentos = 0;
        if (estado == null) estado = EstadoEmail.PENDIENTE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Registra un intento fallido
     */
    public void registrarError(String mensaje, String tipo) {
        this.intentos++;
        this.errorMensaje = mensaje;
        this.errorTipo = tipo;
        this.estado = EstadoEmail.ERROR;
    }

    /**
     * Marca como enviado exitosamente
     */
    public void marcarEnviado() {
        this.estado = EstadoEmail.ENVIADO;
        this.fechaEnvio = LocalDateTime.now();
        this.errorMensaje = null;
        this.errorTipo = null;
    }
}