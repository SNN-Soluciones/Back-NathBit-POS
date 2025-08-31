package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_emails",
    indexes = {
        @Index(name = "idx_cliente_email_cliente", columnList = "cliente_id"),
        @Index(name = "idx_cliente_email_email", columnList = "email"),
        @Index(name = "idx_cliente_email_ultimo_uso", columnList = "ultimo_uso")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "es_principal")
    @Builder.Default
    private Boolean esPrincipal = false;

    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;

    @Column(name = "veces_usado")
    @Builder.Default
    private Integer vecesUsado = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Método helper para registrar uso
    public void registrarUso() {
        this.ultimoUso = LocalDateTime.now();
        this.vecesUsado++;
    }
}