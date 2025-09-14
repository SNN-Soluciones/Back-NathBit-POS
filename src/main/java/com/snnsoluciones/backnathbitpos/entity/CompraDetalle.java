package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "compra_detalles")
@Data
@ToString(exclude = {"compra", "producto"})
public class CompraDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;
    
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;
    
    // Producto (puede ser null si es un servicio o item no inventariable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;
    
    // Información del item (desnormalizada para histórico)
    @Column(name = "codigo", length = 50)
    private String codigo; // Código interno o del proveedor
    
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;
    
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "es_servicio", nullable = false)
    private Boolean esServicio = false;
    
    // Cantidades y unidades
    @Column(name = "cantidad", precision = 18, scale = 5, nullable = false)
    private BigDecimal cantidad;
    
    @Column(name = "unidad_medida", length = 50, nullable = false)
    private String unidadMedida; // Unid, Kg, Lt, etc.
    
    @Column(name = "unidad_medida_comercial", length = 50)
    private String unidadMedidaComercial; // Caja, Docena, etc.
    
    // Precios
    @Column(name = "precio_unitario", precision = 18, scale = 5, nullable = false)
    private BigDecimal precioUnitario;
    
    @Column(name = "monto_total", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotal; // cantidad * precio_unitario
    
    @Column(name = "monto_descuento", precision = 18, scale = 5)
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    
    @Column(name = "naturaleza_descuento", length = 100)
    private String naturalezaDescuento;
    
    @Column(name = "sub_total", precision = 18, scale = 5, nullable = false)
    private BigDecimal subTotal; // monto_total - descuento
    
    // Impuestos
    @Column(name = "base_imponible", precision = 18, scale = 5)
    private BigDecimal baseImponible;
    
    @Column(name = "codigo_tarifa_iva", length = 2)
    private String codigoTarifaIVA; // 01, 02, 03, 04, 08
    
    @Column(name = "tarifa_iva", precision = 5, scale = 2)
    private BigDecimal tarifaIVA; // 13%, 4%, etc.
    
    @Column(name = "monto_impuesto", precision = 18, scale = 5)
    private BigDecimal montoImpuesto = BigDecimal.ZERO;
    
    // Exoneraciones
    @Column(name = "tiene_exoneracion", nullable = false)
    private Boolean tieneExoneracion = false;
    
    @Column(name = "tipo_documento_exoneracion", length = 2)
    private String tipoDocumentoExoneracion;
    
    @Column(name = "numero_documento_exoneracion", length = 50)
    private String numeroDocumentoExoneracion;
    
    @Column(name = "porcentaje_exoneracion", precision = 5, scale = 2)
    private BigDecimal porcentajeExoneracion;
    
    @Column(name = "monto_exoneracion", precision = 18, scale = 5)
    private BigDecimal montoExoneracion = BigDecimal.ZERO;
    
    @Column(name = "impuesto_neto", precision = 18, scale = 5)
    private BigDecimal impuestoNeto = BigDecimal.ZERO; // monto_impuesto - monto_exoneracion
    
    // Total de la línea
    @Column(name = "monto_total_linea", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotalLinea; // sub_total + impuesto_neto
    
    // Información adicional para inventario
    @Column(name = "factor_conversion")
    private BigDecimal factorConversion; // Para convertir unidad comercial a unidad base
    
    @Column(name = "precio_sugerido_venta", precision = 18, scale = 5)
    private BigDecimal precioSugeridoVenta;
    
    @Column(name = "margen_utilidad", precision = 5, scale = 2)
    private BigDecimal margenUtilidad;
    
    // Partida arancelaria (para importaciones)
    @Column(name = "partida_arancelaria", length = 12)
    private String partidaArancelaria;
    
    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Métodos de cálculo
    public void calcularTotales() {
        // Calcular monto total
        if (cantidad != null && precioUnitario != null) {
            montoTotal = cantidad.multiply(precioUnitario);
        }
        
        // Calcular subtotal
        if (montoTotal != null && montoDescuento != null) {
            subTotal = montoTotal.subtract(montoDescuento);
        }
        
        // Base imponible es el subtotal para efectos de impuesto
        baseImponible = subTotal;
        
        // Calcular impuesto
        if (baseImponible != null && tarifaIVA != null) {
            montoImpuesto = baseImponible.multiply(tarifaIVA).divide(BigDecimal.valueOf(100));
        }
        
        // Calcular impuesto neto
        if (montoImpuesto != null && montoExoneracion != null) {
            impuestoNeto = montoImpuesto.subtract(montoExoneracion);
        }
        
        // Total de la línea
        if (subTotal != null && impuestoNeto != null) {
            montoTotalLinea = subTotal.add(impuestoNeto);
        }
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
        CompraDetalle that = (CompraDetalle) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}