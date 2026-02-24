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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "opcion_v2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private SlotV2 slot;

    // Nombre manual (cuando no hay producto)
    private String nombre;

    // Producto asociado (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private BigDecimal precioAdicional;

    @Column(nullable = false)
    private Boolean esDefault;

    @Column(nullable = false)
    private Boolean disponible;

    @Column(nullable = false)
    private Integer orden;

    // Sub-slots de esta opción (recursivo)
    @OneToMany(mappedBy = "opcionPadre", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<SlotV2> subSlots = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getNombreEfectivo() {
      if (nombre != null && !nombre.isBlank()) {
        return nombre;
      }
      if (producto != null) {
        return producto.getNombre();
      }
        return "Sin nombre";
    }
}