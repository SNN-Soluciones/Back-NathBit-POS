package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una persona/comensal dentro de una orden
 * Permite dividir la cuenta entre varias personas
 * 
 * Ejemplo: Mesa 4 con 3 personas (Andrés, Pelón, Macha)
 * Cada uno puede tener sus propios items asignados
 */
@Entity
@Table(name = "orden_personas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(nullable = false, length = 100)
    private String nombre; // "Andrés", "Pelón", "Macha"

    @Column(length = 7)
    @Builder.Default
    private String color = "#3B82F6"; // Color hex para UI (#FF5733)

    @Column(name = "orden")
    @Builder.Default
    private Integer ordenVisualizacion = 0; // Para ordenar visualmente

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relación inversa: items de esta persona
    @OneToMany(mappedBy = "ordenPersona", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdenItem> items = new ArrayList<>();

    // ==================== MÉTODOS HELPER ====================

    /**
     * Calcula el total de items de esta persona
     */
    @Transient
    public java.math.BigDecimal getTotal() {
        return items.stream()
            .map(OrdenItem::getTotal)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Cuenta cuántos items tiene esta persona
     */
    @Transient
    public int getCantidadItems() {
        return items.size();
    }

    /**
     * Verifica si todos los items de esta persona están pagados
     */
    @Transient
    public boolean todoPagado() {
        return !items.isEmpty() && items.stream().allMatch(OrdenItem::estaPagado);
    }

    /**
     * Verifica si tiene al menos un item pagado
     */
    @Transient
    public boolean tienePagosParciales() {
        return items.stream().anyMatch(OrdenItem::estaPagado);
    }
}