package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.mappers.MapJsonConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "slot_v2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Slot raíz → compuesto no null, opcionPadre null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compuesto_id")
    private ProductoCompuestoV2 compuesto;

    // Sub-slot → opcionPadre no null, compuesto null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_padre_id")
    private OpcionV2 opcionPadre;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private Boolean esRequerido;

    @Column(nullable = false)
    private Integer cantidadMinima;

    @Column(nullable = false)
    private Integer cantidadMaxima;

    @Column(nullable = false)
    private Boolean permiteCantidadPorOpcion = false;

    @Column
    private Integer maxTiposDiferentes;

    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<Long, BigDecimal> preciosOverride = new HashMap<>();

    @Column(nullable = false)
    private Integer orden;

    // Familia
    @Column(nullable = false)
    private Boolean usaFamilia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private FamiliaProducto familia;

    @Column(name = "precio_adicional_por_opcion")
    private BigDecimal precioAdicionalPorOpcion;

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<OpcionV2> opciones = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}