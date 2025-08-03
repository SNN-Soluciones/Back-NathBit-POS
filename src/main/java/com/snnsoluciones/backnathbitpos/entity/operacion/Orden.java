// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/entity/operacion/Orden.java

package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import com.snnsoluciones.backnathbitpos.entity.catalogo.Cliente;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orden extends BaseEntity {

  @Column(nullable = false, unique = true, length = 50)
  private String numeroOrden;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoOrden tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private EstadoOrden estado = EstadoOrden.PENDIENTE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mesa_id")
  private Mesa mesa;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mesero_id")
  private Usuario mesero;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "caja_id")
  private Caja caja;

  @Column(name = "fecha_orden", nullable = false)
  private LocalDateTime fechaOrden;

  @Column(name = "cantidad_personas")
  private Integer cantidadPersonas;

  // Totales
  @Column(name = "subtotal", precision = 18, scale = 2, nullable = false)
  @Builder.Default
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "total_descuentos", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal totalDescuentos = BigDecimal.ZERO;

  @Column(name = "total_impuestos", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal totalImpuestos = BigDecimal.ZERO;

  @Column(name = "total", precision = 18, scale = 2, nullable = false)
  @Builder.Default
  private BigDecimal total = BigDecimal.ZERO;

  @Column(columnDefinition = "TEXT")
  private String observaciones;

  // Relación con detalles
  @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<OrdenDetalle> detalles = new ArrayList<>();

  // Información de entrega (para llevar/delivery)
  @Column(name = "nombre_cliente_delivery", length = 100)
  private String nombreClienteDelivery;

  @Column(name = "telefono_delivery", length = 20)
  private String telefonoDelivery;

  @Column(name = "direccion_delivery", columnDefinition = "TEXT")
  private String direccionDelivery;

  @Column(name = "hora_entrega_estimada")
  private LocalDateTime horaEntregaEstimada;
}