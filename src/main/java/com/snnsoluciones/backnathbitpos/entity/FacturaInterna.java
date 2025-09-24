package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_interna")
@Data
public class FacturaInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true)
    private String numeroFactura; // Formato: FI-2024-00001

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // Puede ser null para ventas de mostrador

    @Column(name = "nombre_cliente")
    private String nombreCliente; // Para cuando no hay cliente registrado

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // Cajero que realizó la venta

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    // Totales
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal; // Suma de líneas sin descuentos ni cargos

    @Column(name = "total_descuentos", precision = 15, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_otros_cargos", precision = 15, scale = 2)
    private BigDecimal totalOtrosCargos = BigDecimal.ZERO; // Incluye servicio

    @Column(name = "total_venta", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalVenta; // Total final a pagar

    // Usando EstadoFactura existente - solo estados básicos
    @Column(name = "estado", nullable = false)
    private String estado = "ACTIVA"; // ACTIVA, ANULADA, BORRADOR

    @Column(name = "notas")
    private String notas;

    @Column(name = "anulada_por")
    private Long anuladaPor; // ID del usuario que anuló

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaInternaOtrosCargos> facturaInternaOtrosCargos;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaInternaDescuentos> facturaInternaDescuentos;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    private List<FacturaInternaDetalle> facturaInternaDetalles;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FacturaInternaMediosPago> mediosPago = new ArrayList<>();
}