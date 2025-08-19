package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.SituacionDocumento;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true)
    private String clave; // Puede ser null para documentos internos

    @Column(length = 20, nullable = false, unique = true)
    private String consecutivo;

    @Column(name = "tipo_documento", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    @Column(name = "fecha_emision", nullable = false)
    private String fechaEmision;

    @Column(name = "estado", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado = EstadoFactura.GENERADA;

    // Referencias
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCaja sesionCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    // Datos de venta
    @Column(name = "condicion_venta", nullable = false)
    @Enumerated(EnumType.STRING)
    private CondicionVenta condicionVenta = CondicionVenta.CONTADO;

    @Column(name = "plazo_credito")
    private Integer plazoCredito; // En días

    // Medios de pago múltiples
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaMedioPago> mediosPago = new ArrayList<>();

    @Column(name = "situacion", nullable = false)
    @Enumerated(EnumType.STRING)
    private SituacionDocumento situacion = SituacionDocumento.NORMAL;

    // Montos
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal descuentos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal total = BigDecimal.ZERO;

    private String observaciones;

    // Detalles
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaDetalle> detalles = new ArrayList<>();

    // ========== CAMPOS DE MONEDA ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_moneda", length = 3, nullable = false)
    private Moneda moneda = Moneda.CRC; // Por defecto Colones

    @Column(name = "tipo_cambio", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambio = BigDecimal.ONE; // Por defecto 1.00

    @Column(name = "total_moneda_local", precision = 18, scale = 5)
    private BigDecimal totalMonedaLocal; // Total convertido a CRC si moneda != CRC

    // ========== CAMPOS DE DESCUENTO GLOBAL ==========
    @Column(name = "descuento_global_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoGlobalPorcentaje = BigDecimal.ZERO;

    @Column(name = "monto_descuento_global", precision = 18, scale = 5)
    private BigDecimal montoDescuentoGlobal = BigDecimal.ZERO;

    @Column(name = "motivo_descuento_global", length = 200)
    private String motivoDescuentoGlobal;

    // ========== RELACIÓN CON OTROS CARGOS ==========
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OtroCargo> otrosCargos = new ArrayList<>();

    // ========== CAMPOS ADICIONALES HACIENDA ==========
    @Column(name = "codigo_seguridad", length = 8)
    private String codigoSeguridad; // 8 dígitos para la clave

    @Column(name = "situacion_comprobante", length = 1, nullable = false)
    private String situacionComprobante = "1"; // 1=Normal, 2=Contingencia, 3=Sin Internet

    // Total de otros cargos para cálculo rápido
    @Column(name = "total_otros_cargos", precision = 18, scale = 5)
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO;

    // Total de descuentos (líneas + global) para Hacienda
    @Column(name = "total_descuentos", precision = 18, scale = 5)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    // Auditoría
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helpers
    public void agregarDetalle(FacturaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public void agregarMedioPago(FacturaMedioPago medioPago) {
        mediosPago.add(medioPago);
        medioPago.setFactura(this);
    }

    public BigDecimal getTotalMediosPago() {
        return mediosPago.stream()
            .map(FacturaMedioPago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean esElectronica() {
        return tipoDocumento != null && tipoDocumento.isElectronico();
    }


    /**
     * Agrega un otro cargo a la factura
     */
    public void agregarOtroCargo(OtroCargo otroCargo) {
        otrosCargos.add(otroCargo);
        otroCargo.setFactura(this);
        otroCargo.setNumeroLinea(otrosCargos.size());
    }

    /**
     * Calcula el total de otros cargos
     */
    public BigDecimal calcularTotalOtrosCargos() {
        return otrosCargos.stream()
            .map(OtroCargo::getMontoCargo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total de descuentos (líneas + global)
     */
    public BigDecimal calcularTotalDescuentos() {
        // Descuentos de líneas
        BigDecimal descuentosLineas = detalles.stream()
            .map(FacturaDetalle::getTotalDescuentosLinea)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Más descuento global
        return descuentosLineas.add(montoDescuentoGlobal);
    }

    /**
     * Aplica descuento global basado en porcentaje
     */
    public void aplicarDescuentoGlobal() {
        if (descuentoGlobalPorcentaje != null &&
            descuentoGlobalPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
            // Calcular sobre subtotal neto (después de descuentos de línea)
            BigDecimal base = subtotal.subtract(
                detalles.stream()
                    .map(FacturaDetalle::getTotalDescuentosLinea)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            );

            montoDescuentoGlobal = base
                .multiply(descuentoGlobalPorcentaje)
                .divide(new BigDecimal("100"), 5, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Convierte el total a moneda local si aplica
     */
    public void calcularTotalMonedaLocal() {
        if (moneda != Moneda.CRC && tipoCambio != null) {
            totalMonedaLocal = total.multiply(tipoCambio);
        } else {
            totalMonedaLocal = total;
        }
    }

    /**
     * Genera código de seguridad aleatorio de 8 dígitos
     */
    public void generarCodigoSeguridad() {
        this.codigoSeguridad = String.format("%08d",
            new Random().nextInt(100000000));
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
        Factura factura = (Factura) o;
        return getId() != null && Objects.equals(getId(), factura.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}