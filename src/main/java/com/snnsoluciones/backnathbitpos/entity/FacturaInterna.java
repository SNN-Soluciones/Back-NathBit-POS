package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factura interna SIMPLIFICADA - Solo para control interno
 * Sin requisitos de Hacienda, sin XML, sin claves, sin nada complejo
 */
@Entity
@Table(name = "factura_interna")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class FacturaInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== DATOS BÁSICOS =====
    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero; // Ej: "INT-2024-0001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // ===== MESERO Y MESA (Opcional) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id")
    private Usuario mesero; // Usuario que atendió (puede ser igual al cajero)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario cajero;

    // ===== CLIENTE (Opcional) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "nombre_cliente", length = 255)
    private String nombreCliente; // Si no hay cliente registrado

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_usuario_id")
    private SesionCajaUsuario sesionCajaUsuario;

    // ===== FECHAS =====
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    // ===== MONTOS SIMPLES =====
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal; // Suma de líneas

    @Column(name = "descuento_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO; // Porcentaje de descuento global (0-100

    @Column(name = "descuento", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    // ===== IMPUESTO DE SERVICIO =====
    @Column(name = "porcentaje_servicio", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeServicio = BigDecimal.ZERO;

    @Column(name = "impuesto_servicio", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal impuestoServicio = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total; // subtotal - descuento

    @Column(name = "numero_viper", length = 50)
    private String numeroViper;

    // ===== PAGO =====
    @Column(name = "pago_recibido", precision = 15, scale = 2)
    private BigDecimal pagoRecibido; // Para calcular vuelto

    @Column(name = "vuelto", precision = 15, scale = 2)
    private BigDecimal vuelto;

    // ===== ESTADO =====
    @Column(name = "estado", length = 20, nullable = false)
    @Builder.Default
    private String estado = "PAGADA"; // PAGADA, ANULADA

    @Column(name = "anulada_por_id")
    private Long anuladaPorId; // ID del usuario que anuló

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    // ===== NOTAS =====
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    // ===== DETALLES =====
    @OneToMany(mappedBy = "facturaInterna", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaInternaDetalle> detalles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_digital_id")
    private PlataformaDigitalConfig plataformaDigital;

    // ===== MEDIOS DE PAGO =====
    @OneToMany(mappedBy = "facturaInterna", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaInternaMedioPago> mediosPago = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id", nullable = true)
    private SesionCaja sesionCaja;

    @Column(name = "condicion_venta", length = 20)
    @Builder.Default
    private String condicionVenta = "CONTADO";

    @Column(name = "plazo_credito")
    private Integer plazoCredito;

    // ===== AUDITORÍA =====
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== MÉTODOS HELPER =====

    /**
     * Calcula el vuelto basado en pago recibido
     */
    public void calcularVuelto() {
        if (this.pagoRecibido != null && this.total != null) {
            this.vuelto = this.pagoRecibido.subtract(this.total);
            if (this.vuelto.compareTo(BigDecimal.ZERO) < 0) {
                this.vuelto = BigDecimal.ZERO;
            }
        }
    }

    /**
     * Agrega un detalle a la factura
     */
    public void agregarDetalle(FacturaInternaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFacturaInterna(this);
    }

    /**
     * Agrega un medio de pago a la factura
     */
    public void agregarMedioPago(FacturaInternaMedioPago medioPago) {
        mediosPago.add(medioPago);
        medioPago.setFacturaInterna(this);
    }

    /**
     * Anula la factura
     */
    public void anular(Long usuarioId, String motivo) {
        this.estado = "ANULADA";
        this.anuladaPorId = usuarioId;
        this.fechaAnulacion = LocalDateTime.now();
        this.motivoAnulacion = motivo;
    }

    /**
     * Calcula el total basado en subtotal, descuento e impuesto de servicio
     */
    public void calcularTotal() {
        BigDecimal desc = this.descuento != null ? this.descuento : BigDecimal.ZERO;
        BigDecimal servicio = this.impuestoServicio != null ? this.impuestoServicio : BigDecimal.ZERO;
        this.total = this.subtotal.subtract(desc).add(servicio);
    }
}