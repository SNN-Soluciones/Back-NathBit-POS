package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_actividades",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cliente_actividad",
            columnNames = {"cliente_id", "codigo_actividad"}
        )
    },
    indexes = {
        @Index(name = "idx_cliente_actividades_cliente", columnList = "cliente_id"),
        @Index(name = "idx_cliente_actividades_codigo", columnList = "codigo_actividad")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", insertable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "codigo_actividad", nullable = false, length = 10)
    private String codigoActividad;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}