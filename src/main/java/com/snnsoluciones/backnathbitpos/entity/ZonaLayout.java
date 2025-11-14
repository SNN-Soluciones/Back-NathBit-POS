// ZonaLayout.java
package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

/**
 * Entidad para almacenar el layout (distribución) de mesas en una zona.
 * Guarda las posiciones y tamaños en formato JSON para flexibilidad.
 */
@Entity
@Table(name = "zona_layout",
    uniqueConstraints = @UniqueConstraint(name = "uk_zona_layout", columnNames = {"zona_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaLayout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_zona_layout_zona"))
    private ZonaMesa zona;

    /**
     * JSON con el array de layouts de mesas:
     * [
     *   {"mesaId": 1, "x": 100, "y": 50, "width": 120, "height": 120},
     *   {"mesaId": 2, "x": 250, "y": 50, "width": 120, "height": 120}
     * ]
     */
    @Column(name = "layout_json", columnDefinition = "TEXT", nullable = false)
    private String layoutJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
