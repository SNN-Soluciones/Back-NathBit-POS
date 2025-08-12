package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "factura_documentos", indexes = {
    @Index(name = "idx_factura_doc_clave", columnList = "clave"),
    @Index(name = "idx_factura_doc_tipo", columnList = "tipo_archivo")
})
public class FacturaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String clave;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "tipo_archivo", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoArchivoFactura tipoArchivo;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;

    @Column(nullable = false)
    private Long tamanio; // En bytes

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper para generar la key de S3 con estructura de empresa
    public static String generarS3Key(String empresaNombre, String clave, TipoArchivoFactura tipo) {
        // Limpiar nombre de empresa
        String empresaLimpia = empresaNombre.replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9_-]", "");

        LocalDateTime ahora = LocalDateTime.now();
        String mesNombre = obtenerNombreMes(ahora.getMonthValue());

        return String.format("NathBit-POS/%s/documentos/%d/%s/%02d/%s_%s.%s",
            empresaLimpia,
            ahora.getYear(),
            mesNombre,
            ahora.getDayOfMonth(),
            clave,
            tipo.name(),
            tipo.getExtension()
        );
    }

    private static String obtenerNombreMes(int mes) {
        return switch (mes) {
            case 1 -> "enero";
            case 2 -> "febrero";
            case 3 -> "marzo";
            case 4 -> "abril";
            case 5 -> "mayo";
            case 6 -> "junio";
            case 7 -> "julio";
            case 8 -> "agosto";
            case 9 -> "septiembre";
            case 10 -> "octubre";
            case 11 -> "noviembre";
            case 12 -> "diciembre";
            default -> "mes" + mes;
        };
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        FacturaDocumento that = (FacturaDocumento) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}