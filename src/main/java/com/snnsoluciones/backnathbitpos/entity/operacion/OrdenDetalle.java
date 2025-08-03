// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/operacion/OrdenDetalle.java

package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.catalogo.Producto;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrdenDetalle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDetalle extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "orden_id", nullable = false)
  private Orden orden;

  @Column(name = "numero_linea", nullable = false)
  private Integer numeroLinea;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producto_id", nullable = false)
  private Producto producto;

  @Column(nullable = false, precision = 10, scale = 3)
  @Builder.Default
  private BigDecimal cantidad = BigDecimal.ONE;

  @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 5)
  private BigDecimal precioUnitario;

  @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

  @Column(name = "monto_descuento", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal montoDescuento = BigDecimal.ZERO;

  @Column(name = "motivo_descuento", length = 200)
  private String motivoDescuento;

  // Impuestos
  @Column(name = "tarifa_iva", precision = 5, scale = 2)
  private BigDecimal tarifaIva;

  @Column(name = "monto_iva", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal montoIva = BigDecimal.ZERO;

  @Column(name = "monto_otros_impuestos", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal montoOtrosImpuestos = BigDecimal.ZERO;

  // Totales
  @Column(precision = 18, scale = 2, nullable = false)
  private BigDecimal subtotal;

  @Column(precision = 18, scale = 2, nullable = false)
  private BigDecimal total;

  // Estado y tracking
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private EstadoOrdenDetalle estado = EstadoOrdenDetalle.PENDIENTE;

  @Column(name = "fecha_pedido", nullable = false)
  private LocalDateTime fechaPedido;

  @Column(name = "fecha_preparacion")
  private LocalDateTime fechaPreparacion;

  @Column(name = "fecha_listo")
  private LocalDateTime fechaListo;

  @Column(name = "fecha_servido")
  private LocalDateTime fechaServido;

  @Column(name = "fecha_cancelado")
  private LocalDateTime fechaCancelado;

  // Información adicional
  @Column(columnDefinition = "TEXT")
  private String observaciones;

  @Column(name = "notas_cocina", columnDefinition = "TEXT")
  private String notasCocina;

  @Column(name = "es_cortesia")
  @Builder.Default
  private Boolean esCortesia = false;

  @Column(name = "es_modificador")
  @Builder.Default
  private Boolean esModificador = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "detalle_padre_id")
  private OrdenDetalle detallePadre; // Para modificadores/extras

  // Promociones aplicadas
  @Column(name = "codigo_promocion", length = 50)
  private String codigoPromocion;

  @Column(name = "nombre_promocion", length = 200)
  private String nombrePromocion;

  // Para control de cocina
  @Column(name = "tiempo_preparacion_estimado")
  private Integer tiempoPreparacionEstimado; // En minutos

  @Column(name = "estacion_cocina", length = 50)
  private String estacionCocina; // Ej: "Cocina Caliente", "Cocina Fría", "Bar"

  @Column(name = "prioridad")
  @Builder.Default
  private Integer prioridad = 0; // 0=Normal, 1=Alta, 2=Urgente

  // Métodos helper
  public void marcarEnPreparacion() {
    this.estado = EstadoOrdenDetalle.EN_PREPARACION;
    this.fechaPreparacion = LocalDateTime.now();
  }

  public void marcarListo() {
    this.estado = EstadoOrdenDetalle.LISTO;
    this.fechaListo = LocalDateTime.now();
  }

  public void marcarServido() {
    this.estado = EstadoOrdenDetalle.SERVIDO;
    this.fechaServido = LocalDateTime.now();
  }

  public void marcarCancelado(String motivo) {
    this.estado = EstadoOrdenDetalle.CANCELADO;
    this.fechaCancelado = LocalDateTime.now();
    this.observaciones = motivo;
  }

  public boolean esCancelable() {
    return estado == EstadoOrdenDetalle.PENDIENTE ||
        estado == EstadoOrdenDetalle.EN_PREPARACION;
  }

  public BigDecimal getMontoSinImpuestos() {
    return subtotal.subtract(montoDescuento);
  }
}