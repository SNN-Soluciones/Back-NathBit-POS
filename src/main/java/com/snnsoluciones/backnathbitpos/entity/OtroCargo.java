package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad para manejar Otros Cargos según Hacienda 4.4
 * Incluye cargos como servicio 10%, timbres, costos exportación, etc.
 */
@Data
@Entity
@Table(name = "factura_otros_cargos")
@ToString(exclude = "factura")
public class OtroCargo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    /**
     * Código según nota 16 de Hacienda:
     * 01 - Contribución parafiscal
     * 02 - Timbre Cruz Roja
     * 03 - Timbre Bomberos
     * 04 - Cobro de tercero
     * 05 - Costos exportación
     * 06 - Impuesto servicio 10%
     * 07 - Timbre colegios profesionales
     * 08 - Depósitos garantía
     * 09 - Multas o penalizaciones
     * 10 - Intereses moratorios
     * 99 - Otros cargos
     */
    @Column(name = "tipo_documento_oc", length = 2, nullable = false)
    private String tipoDocumentoOC;
    
    /**
     * Descripción cuando se usa código 99
     * Mínimo 5 caracteres, máximo 100
     */
    @Column(name = "tipo_documento_otros", length = 100)
    private String tipoDocumentoOTROS;
    
    /**
     * Nombre descriptivo del cargo
     * Ej: "Impuesto de servicio 10%"
     */
    @Column(name = "nombre_cargo", length = 100, nullable = false)
    private String nombreCargo;
    
    /**
     * Porcentaje del cargo si aplica
     * Ej: 10.00 para servicio 10%
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;
    
    /**
     * Monto total del cargo
     */
    @Column(name = "monto_cargo", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoCargo;
    
    /**
     * Número de línea para orden en el XML
     * 1-15 según Hacienda
     */
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;
    
    // Campos para terceros (cuando tipoDocumentoOC = '04')
    @Column(name = "tercero_tipo_identificacion", length = 2)
    private String terceroTipoIdentificacion;
    
    @Column(name = "tercero_numero_identificacion", length = 20)
    private String terceroNumeroIdentificacion;
    
    @Column(name = "tercero_nombre", length = 100)
    private String terceroNombre;
    
    @PrePersist
    @PreUpdate
    public void validar() {
        // Si es código 99, debe tener descripción
        if ("99".equals(tipoDocumentoOC) && 
            (tipoDocumentoOTROS == null || tipoDocumentoOTROS.trim().length() < 5)) {
            throw new IllegalStateException(
                "Código 99 requiere descripción de al menos 5 caracteres"
            );
        }
        
        // Si es código 04, debe tener datos del tercero
        if ("04".equals(tipoDocumentoOC) && 
            (terceroNumeroIdentificacion == null || terceroNombre == null)) {
            throw new IllegalStateException(
                "Código 04 requiere identificación del tercero"
            );
        }
        
        // Validar número de línea
        if (numeroLinea < 1 || numeroLinea > 15) {
            throw new IllegalStateException(
                "Número de línea debe estar entre 1 y 15"
            );
        }
    }
    
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass() 
            : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OtroCargo otroCargo = (OtroCargo) o;
        return getId() != null && Objects.equals(getId(), otroCargo.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass().hashCode()
            : getClass().hashCode();
    }
}