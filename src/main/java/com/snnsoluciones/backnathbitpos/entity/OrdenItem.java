package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoPagoItem;
import jakarta.persistence.*;
import java.math.RoundingMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cantidad = BigDecimal.ONE;

    // Precio al momento de la venta (puede cambiar en el futuro)
    @Column(name = "precio_unitario", precision = 18, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    // Descuento aplicado
    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "monto_descuento", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    // Impuesto
    @Column(name = "tarifa_impuesto", precision = 5, scale = 2)
    private BigDecimal tarifaImpuesto;

    @Column(name = "monto_impuesto", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoImpuesto = BigDecimal.ZERO;

    // Totales
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "total_descuento", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(name = "total_impuesto", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal total;

    // Para productos compuestos
    @OneToMany(mappedBy = "ordenItemPadre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrdenItemOpcion> opciones = new ArrayList<>();

    // Notas especiales del item
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Estado del item para cocina
    @Column(name = "enviado_cocina")
    @Builder.Default
    private Boolean enviadoCocina = false;

    @Column(name = "fecha_envio_cocina")
    private LocalDateTime fechaEnvioCocina;

    @Column(name = "preparado")
    @Builder.Default
    private Boolean preparado = false;

    @Column(name = "fecha_preparado")
    private LocalDateTime fechaPreparado;

    @Column(name = "entregado")
    @Builder.Default
    private Boolean entregado = false;

    @Column(name = "fecha_entregado")
    private LocalDateTime fechaEntregado;

    // Para splits de cuenta
    @Column(name = "split_personas")
    private String splitPersonas; // "1,3" significa personas 1 y 3 comparten este item

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 20)
    @Builder.Default
    private EstadoPagoItem estadoPago = EstadoPagoItem.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_interna_id")
    private FacturaInterna facturaInterna;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    // Métodos de cálculo
    public void calcularTotales() {
        // Subtotal base
        this.subtotal = precioUnitario.multiply(cantidad);

        // Calcular descuento
        if (porcentajeDescuento != null && porcentajeDescuento.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDescuento = subtotal
                .multiply(porcentajeDescuento)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (montoDescuento != null && montoDescuento.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDescuento = montoDescuento.multiply(cantidad);
        } else {
            this.totalDescuento = BigDecimal.ZERO;
        }

        // Base imponible = subtotal - descuentos
        BigDecimal baseImponible = subtotal.subtract(totalDescuento);

        // Calcular IVA sobre la base imponible
        BigDecimal montoIVA = BigDecimal.ZERO;
        if (tarifaImpuesto != null && tarifaImpuesto.compareTo(BigDecimal.ZERO) > 0) {
            montoIVA = baseImponible
                .multiply(tarifaImpuesto)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // 🎯 Calcular impuesto de SERVICIO (10%) SOLO si el producto es servicio
        BigDecimal montoServicio = BigDecimal.ZERO;
        if (producto != null && Boolean.TRUE.equals(producto.getEsServicio())) {
            montoServicio = baseImponible
                .multiply(new BigDecimal("10"))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // Total de impuestos = IVA + Servicio (si aplica)
        this.totalImpuesto = montoIVA.add(montoServicio);

        // Total final = Base imponible + Impuestos
        this.total = baseImponible.add(this.totalImpuesto);
    }

    public void agregarOpcion(OrdenItemOpcion opcion) {
        opciones.add(opcion);
        opcion.setOrdenItemPadre(this);
    }

    public void marcarEnviadoCocina() {
        this.enviadoCocina = true;
        this.fechaEnvioCocina = LocalDateTime.now();
    }

    public void marcarPreparado() {
        this.preparado = true;
        this.fechaPreparado = LocalDateTime.now();
    }

    public void marcarEntregado() {
        this.entregado = true;
        this.fechaEntregado = LocalDateTime.now();
    }

    /**
     * Verifica si el item está pendiente de pago
     */
    public boolean estaPendiente() {
        return estadoPago == null || estadoPago == EstadoPagoItem.PENDIENTE;
    }

    /**
     * Verifica si el item ya fue pagado
     */
    public boolean estaPagado() {
        return estadoPago == EstadoPagoItem.PAGADO;
    }

    /**
     * Marca el item como pagado con factura ELECTRÓNICA
     */
    public void marcarPagadoConFactura(Factura factura) {
        this.estadoPago = EstadoPagoItem.PAGADO;
        this.factura = factura;
        this.facturaInterna = null; // Exclusividad
        this.fechaPago = LocalDateTime.now();
    }

    /**
     * Marca el item como pagado con factura INTERNA
     */
    public void marcarPagadoConFacturaInterna(FacturaInterna facturaInterna) {
        this.estadoPago = EstadoPagoItem.PAGADO;
        this.facturaInterna = facturaInterna;
        this.factura = null; // Exclusividad
        this.fechaPago = LocalDateTime.now();
    }

    /**
     * Obtiene el ID de la factura asociada (electrónica o interna)
     */
    @Transient
    public Long getFacturaAsociadaId() {
        if (factura != null) return factura.getId();
        if (facturaInterna != null) return facturaInterna.getId();
        return null;
    }

    /**
     * Obtiene el tipo de documento con el que se pagó
     */
    @Transient
    public String getTipoDocumentoPago() {
        if (factura != null) return factura.getTipoDocumento().name();
        if (facturaInterna != null) return "FACTURA_INTERNA";
        return null;
    }
}