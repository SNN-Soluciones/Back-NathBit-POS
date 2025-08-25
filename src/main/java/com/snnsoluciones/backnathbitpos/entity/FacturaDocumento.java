package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "factura_documento",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_doc_factura_tipo", columnNames = {"factura_id", "tipo_archivo"})
    },
    indexes = {
        @Index(name = "ix_doc_clave", columnList = "clave"),
        @Index(name = "ix_doc_tipo", columnList = "tipo_archivo"),
        @Index(name = "ix_doc_s3key", columnList = "s3_key")
    })
public class FacturaDocumento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "clave", nullable = false, length = 50)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_archivo", nullable = false, length = 40)
    private TipoArchivoFactura tipoArchivo;

    @Column(name = "s3_bucket", length = 128)
    private String s3Bucket;

    @Column(name = "s3_key", length = 512)
    private String s3Key;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "tamanio", nullable = false)
    private long tamanio;

    @Column(name = "sha256", length = 64)
    private String sha256; // opcional para idempotencia/verificación

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Helper para naming (opcional, si no lo tienes centralizado):
    public static String generarS3Key(String basePrefix, String clave, TipoArchivoFactura tipo) {
        return basePrefix + "clave-" + clave + "_" + tipo.name() + ".xml";
    }
}