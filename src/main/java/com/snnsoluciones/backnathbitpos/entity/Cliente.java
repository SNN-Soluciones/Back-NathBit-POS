package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clientes", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cliente_sucursal_identificacion_emails",
            columnNames = {"sucursal_id", "numero_identificacion", "emails"}
        )
    },
    indexes = {
        @Index(name = "idx_cliente_sucursal_numero", columnList = "sucursal_id, numero_identificacion"),
        @Index(name = "idx_cliente_sucursal_nombre", columnList = "sucursal_id, razon_social")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", nullable = false)
    private TipoIdentificacion tipoIdentificacion;
    
    @Column(name = "numero_identificacion", nullable = false, length = 20)
    private String numeroIdentificacion;
    
    @Column(name = "razon_social", nullable = false, length = 100)
    private String razonSocial;

    @Column(name = "inscrito_hacienda")
    private Boolean inscritoHacienda = false;

    @Column(name = "fecha_verificacion_hacienda")
    private LocalDateTime fechaVerificacionHacienda;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ClienteEmail> clienteEmails = new HashSet<>();
    
    @Column(name = "telefono_codigo_pais", length = 3)
    private String telefonoCodigoPais;
    
    @Column(name = "telefono_numero", length = 20)
    private String telefonoNumero;
    
    @Column(name = "permite_credito", nullable = false)
    @Builder.Default
    private Boolean permiteCredito = false;
    
    @Column(name = "tiene_exoneracion", nullable = false)
    @Builder.Default
    private Boolean tieneExoneracion = false;
    
    @Column(columnDefinition = "TEXT")
    private String observaciones;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "dias_credito")
    private Integer diasCredito = 30; // Por defecto 30 días

    @Column(name = "estado_credito", length = 50)
    private String estadoCredito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = true)
    private Sucursal sucursal;

    @Column(name = "saldo_actual", precision = 18, scale = 5)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Column(name = "bloqueado_por_mora")
    private Boolean bloqueadoPorMora = false;

    @Column(name = "fecha_ultimo_pago")
    private LocalDateTime fechaUltimoPago;

    @Column(name = "limite_credito", precision = 18, scale = 5)
    private BigDecimal limiteCredito = BigDecimal.ZERO; // 0 = sin límite
    
    // Relaciones
// reemplaza el mappedBy
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "ubicacion_id", referencedColumnName = "id", nullable = true, unique = true)
    private ClienteUbicacion ubicacion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @Builder.Default
    private Set<ClienteActividad> actividades = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "cliente_id", nullable = false) // FK en clientes_exoneraciones
    @Builder.Default
    private Set<ClienteExoneracion> exoneraciones = new HashSet<>();

    public Optional<ClienteExoneracion> getExoneracionVigente() {
        if (this.getExoneraciones() == null || this.getExoneraciones().isEmpty()) return Optional.empty();

        // 1) si tienes campo boolean vigente, priorízalo
        var vigente = this.getExoneraciones().stream()
            .filter(ClienteExoneracion::estaVigente) // ajusta al nombre real del getter
            .findFirst();
        if (vigente.isPresent()) return vigente;

        // 2) si no hay flag, toma la más “reciente” por fechaEmision (o por id máx)
        return this.getExoneraciones().stream()
            .filter(e -> e.getFechaEmision() != null)
            .max(Comparator.comparing(ClienteExoneracion::getFechaEmision));
    }
}