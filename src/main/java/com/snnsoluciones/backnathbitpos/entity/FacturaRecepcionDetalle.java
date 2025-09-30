package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factura_recepcion_detalle",
       uniqueConstraints = @UniqueConstraint(columnNames = {"factura_recepcion_id", "numero_linea"}))
@Data
@EqualsAndHashCode(exclude = {"facturaRecepcion", "impuestos"})
@ToString(exclude = {"facturaRecepcion", "impuestos"})
public class FacturaRecepcionDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcionAutomatica facturaRecepcion;
    
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;
    
    @Column(name = "tipo_linea", length = 10)
    private String tipoLinea;
    
    // Códigos
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;
    
    @Column(name = "codigo_comercial_tipo", length = 2)
    private String codigoComercialTipo;
    
    @Column(name = "codigo_comercial_codigo", length = 20)
    private String codigoComercialCodigo;
    
    @Column(name = "partido_arancelaria", length = 12)
    private String partidoArancelaria;
    
    // Descripción y cantidades
    @Column(precision = 18, scale = 5, nullable = false)
    private BigDecimal cantidad;
    
    @Column(name = "unidad_medida", length = 20, nullable = false)
    private String unidadMedida;
    
    @Column(name = "unidad_medida_comercial", length = 20)
    private String unidadMedidaComercial;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String detalle;
    
    // Precios
    @Column(name = "precio_unitario", precision = 18, scale = 5, nullable = false)
    private BigDecimal precioUnitario;
    
    @Column(name = "monto_total", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotal;
    
    // Descuentos
    @Column(name = "monto_descuento", precision = 18, scale = 5)
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    
    @Column(name = "naturaleza_descuento", columnDefinition = "TEXT")
    private String naturalezaDescuento;
    
    // Subtotales
    @Column(name = "sub_total", precision = 18, scale = 5, nullable = false)
    private BigDecimal subTotal;
    
    @Column(name = "base_imponible", precision = 18, scale = 5)
    private BigDecimal baseImponible;
    
    // Montos finales
    @Column(name = "impuesto_neto", precision = 18, scale = 5)
    private BigDecimal impuestoNeto = BigDecimal.ZERO;
    
    @Column(name = "monto_total_linea", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotalLinea;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Relaciones
    @OneToMany(mappedBy = "facturaDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaRecepcionDetalleImpuesto> impuestos = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}