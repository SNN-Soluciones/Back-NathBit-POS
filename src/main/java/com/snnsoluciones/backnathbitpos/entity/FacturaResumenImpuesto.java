package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad para el resumen de impuestos por factura Agrupa totales por código de impuesto para el
 * XML Parte de la Arquitectura La Jachuda 🚀
 */
@Data
@Entity
@Table(name = "factura_resumen_impuesto",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"factura_id", "codigo_impuesto", "codigo_tarifa_iva"}
    ))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "factura")
public class FacturaResumenImpuesto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "factura_id", nullable = false)
  private Factura factura;

  /**
   * Código del impuesto según nota 8 Se agrupa por este código
   */
  @Column(name = "codigo_impuesto", length = 2, nullable = false)
  private String codigoImpuesto;

  /**
   * Código de tarifa IVA (solo para código 01) Según nota 8.1
   */
  @Column(name = "codigo_tarifa_iva", length = 2)
  private String codigoTarifaIVA;

  /**
   * Total del monto de impuesto para este código Suma de todos los impuestos con este código
   */
  @Column(name = "total_monto_impuesto", nullable = false, precision = 18, scale = 5)
  private BigDecimal totalMontoImpuesto;

  /**
   * Total base imponible para este impuesto
   */
  @Column(name = "total_base_imponible", precision = 18, scale = 5)
  private BigDecimal totalBaseImponible;

  /**
   * Total exonerado para este impuesto
   */
  @Column(name = "total_monto_exoneracion", precision = 18, scale = 5)
  private BigDecimal totalMontoExoneracion = BigDecimal.ZERO;

  /**
   * Total neto (impuesto - exoneraciones)
   */
  @Column(name = "total_impuesto_neto", nullable = false, precision = 18, scale = 5)
  private BigDecimal totalImpuestoNeto;

  /**
   * Número de líneas que tienen este impuesto
   */
  @Column(name = "cantidad_lineas", nullable = false)
  private Integer cantidadLineas = 0;

  // Índice único para evitar duplicad

  /**
   * Calcula el impuesto neto
   */
  @PrePersist
  @PreUpdate
  public void calcularNeto() {
    if (totalMontoExoneracion == null) {
      totalMontoExoneracion = BigDecimal.ZERO;
    }

    totalImpuestoNeto = totalMontoImpuesto.subtract(totalMontoExoneracion);

    if (totalImpuestoNeto.compareTo(BigDecimal.ZERO) < 0) {
      totalImpuestoNeto = BigDecimal.ZERO;
    }
  }

  /**
   * Agrega montos de una línea de detalle
   */
  public void agregarMontos(BigDecimal montoImpuesto, BigDecimal montoExoneracion,
      BigDecimal baseImponible) {
    // Sumar impuesto
    if (montoImpuesto != null) {
      this.totalMontoImpuesto = this.totalMontoImpuesto.add(montoImpuesto);
    }

    // Sumar exoneración
    if (montoExoneracion != null) {
      this.totalMontoExoneracion = this.totalMontoExoneracion.add(montoExoneracion);
    }

    // Sumar base
    if (baseImponible != null) {
      if (this.totalBaseImponible == null) {
        this.totalBaseImponible = BigDecimal.ZERO;
      }
      this.totalBaseImponible = this.totalBaseImponible.add(baseImponible);
    }

    // Incrementar contador
    this.cantidadLineas++;

    // Recalcular neto
    calcularNeto();
  }
}