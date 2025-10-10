package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cliente_actividades")
public class ClienteActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, insertable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "codigo_actividad", length = 10)
    private String codigoActividad;

    @Column(name = "codigo_ciiu4", length = 20)
    private String codigoCiiu4;

    @Column(name = "codigo_ciiu3", length = 20)
    private String codigoCiiu3;

    @Column(name = "tipo", length = 1)
    private String tipo;

    @Column(name = "estado", length = 1)
    private String estado;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}