// entity/Barra.java
package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.TipoFormaBarra;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "barra",
    uniqueConstraints = @UniqueConstraint(name = "uk_barra_codigo_zona", columnNames = {"zona_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Barra {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_barra_zona"))
    private ZonaMesa zona;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_barra_sucursal"))
    private Sucursal sucursal;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo; // Ej: "BARRA-1", "ISLA-A"

    @Column(name = "nombre", length = 60)
    private String nombre; // Ej: "Barra Principal"

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_forma", nullable = false, length = 30)
    @Builder.Default
    private TipoFormaBarra tipoForma = TipoFormaBarra.LINEA_RECTA;

    @Column(name = "cantidad_sillas", nullable = false)
    @Builder.Default
    private Integer cantidadSillas = 4;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @OneToMany(mappedBy = "barra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SillaBarra> sillas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ✅ Helper: Crear sillas automáticamente
    public void inicializarSillas() {
        this.sillas.clear();
        for (int i = 1; i <= this.cantidadSillas; i++) {
            SillaBarra silla = SillaBarra.builder()
                .barra(this)
                .numero(i)
                .build();
            this.sillas.add(silla);
        }
    }

    // ✅ Helper: Obtener sillas disponibles
    @Transient
    public List<SillaBarra> getSillasDisponibles() {
        return sillas.stream()
            .filter(SillaBarra::isDisponible)
            .toList();
    }

    // ✅ Helper: Obtener sillas ocupadas
    @Transient
    public List<SillaBarra> getSillasOcupadas() {
        return sillas.stream()
            .filter(SillaBarra::isOcupada)
            .toList();
    }

    // ✅ Helper: Contar sillas por estado
    @Transient
    public long contarSillasDisponibles() {
        return sillas.stream().filter(SillaBarra::isDisponible).count();
    }

    @Transient
    public long contarSillasOcupadas() {
        return sillas.stream().filter(SillaBarra::isOcupada).count();
    }
}