package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "factura_detalles")
@ToString(exclude = {"factura", "producto"})
public class FacturaDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal cantidad;
    
    @Column(name = "unidad_medida", length = 15, nullable = false)
    private String unidadMedida = "Unid";
    
    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioUnitario;
    
    @Column(name = "monto_descuento", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal subtotal;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal impuesto;
    
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal total;
    
    // Campos para facturación electrónica
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;
    
    @Column(name = "detalle", length = 200)
    private String detalle;

    @OneToMany(mappedBy = "facturaDetalle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<FacturaDescuento> descuentos = new ArrayList<>();

    // ========== CAMPOS ADICIONALES HACIENDA ==========

    /**
     * Código de tarifa IVA según nota 8.1:
     * 01 - Tarifa 0% (Exento)
     * 02 - Tarifa reducida 1%
     * 03 - Tarifa reducida 2%
     * 04 - Tarifa reducida 4%
     * 05 - Transitorio 0%
     * 06 - Transitorio 4%
     * 07 - Transitorio 8%
     * 08 - Tarifa general 13%
     * 09 - Tarifa reducida 0.5%
     * 10 - Tarifa Exenta
     * 11 - Tarifa 0% sin derecho a crédito
     */
    @Column(name = "codigo_tarifa_iva", length = 2)
    private String codigoTarifaIVA = "08"; // Por defecto 13%

    /**
     * Base imponible para cálculos especiales
     * Por defecto es el subtotal
     */
    @Column(name = "base_imponible", precision = 18, scale = 5)
    private BigDecimal baseImponible;

    /**
     * Monto total de la línea (subtotal + impuesto neto)
     * Requerido por Hacienda
     */
    @Column(name = "monto_total_linea", precision = 18, scale = 5)
    private BigDecimal montoTotalLinea;

    /**
     * Total de descuentos aplicados a esta línea
     */
    @Column(name = "total_descuentos_linea", precision = 18, scale = 5)
    private BigDecimal totalDescuentosLinea = BigDecimal.ZERO;

    // ========== MÉTODOS HELPER ==========

    /**
     * Agrega un descuento a la línea
     */
    public void agregarDescuento(FacturaDescuento descuento) {
        if (descuentos.size() >= 5) {
            throw new IllegalStateException(
                "Máximo 5 descuentos permitidos por línea"
            );
        }
        descuentos.add(descuento);
        descuento.setFacturaDetalle(this);
        descuento.setOrden(descuentos.size());
    }

    /**
     * Calcula el total de descuentos de la línea
     */
    public BigDecimal getTotalDescuentosLinea() {
        return descuentos.stream()
            .map(FacturaDescuento::getMontoDescuento)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula totales según Hacienda 4.4
     */
    @PrePersist
    @PreUpdate
    public void calcularTotales() {
        if (cantidad != null && precioUnitario != null) {
            // 1. Monto total = cantidad × precio
            BigDecimal montoTotal = cantidad.multiply(precioUnitario);

            // 2. Aplicar descuentos en cascada
            BigDecimal montoConDescuentos = montoTotal;
            totalDescuentosLinea = BigDecimal.ZERO;

            // Ordenar y aplicar descuentos
            descuentos.sort((d1, d2) -> d1.getOrden().compareTo(d2.getOrden()));

            for (FacturaDescuento desc : descuentos) {
                if (desc.getPorcentaje() != null && desc.getPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
                    // Calcular descuento sobre el monto actual
                    desc.calcularMonto(montoConDescuentos);
                }
                // Restar el descuento
                montoConDescuentos = montoConDescuentos.subtract(desc.getMontoDescuento());
                totalDescuentosLinea = totalDescuentosLinea.add(desc.getMontoDescuento());
            }

            // 3. Subtotal = monto total - descuentos
            this.subtotal = montoConDescuentos;

            // 4. Base imponible (por defecto = subtotal)
            if (this.baseImponible == null) {
                this.baseImponible = this.subtotal;
            }

            // 5. Calcular impuesto según tarifa
            BigDecimal tasaImpuesto = obtenerTasaImpuesto();
            this.impuesto = baseImponible.multiply(tasaImpuesto)
                .divide(new BigDecimal("100"), 5, BigDecimal.ROUND_HALF_UP);

            // 6. Total de la línea = subtotal + impuesto
            this.total = subtotal.add(impuesto);

            // 7. Monto total línea para Hacienda
            this.montoTotalLinea = this.total;
        }
    }

    /**
     * Obtiene la tasa de impuesto según el código de tarifa
     */
    private BigDecimal obtenerTasaImpuesto() {
        return switch (codigoTarifaIVA) {
            case "01", "05", "10", "11" -> BigDecimal.ZERO;        // 0%
            case "09" -> new BigDecimal("0.5");                     // 0.5%
            case "02" -> BigDecimal.ONE;                             // 1%
            case "03" -> new BigDecimal("2");                       // 2%
            case "04", "06" -> new BigDecimal("4");                 // 4%
            case "07" -> new BigDecimal("8");                       // 8%
            case "08" -> new BigDecimal("13");                      // 13%
            default -> new BigDecimal("13");                        // Por defecto 13%
        };
    }

    /**
     * Valida si el producto aplica servicio 10%
     */
    public boolean aplicaServicio() {
        return producto != null && Boolean.TRUE.equals(producto.getAplicaServicio());
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
        FacturaDetalle that = (FacturaDetalle) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}