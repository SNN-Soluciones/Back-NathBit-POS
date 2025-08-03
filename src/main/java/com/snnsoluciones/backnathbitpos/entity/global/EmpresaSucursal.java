package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa una sucursal de una empresa.
 * Cada sucursal corresponde a un tenant (schema) en la base de datos.
 */
@Entity
@Table(name = "empresas_sucursales", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"empresa", "usuarioSucursales", "configuracionesAcceso"})
@EqualsAndHashCode(exclude = {"empresa", "usuarioSucursales", "configuracionesAcceso"})
public class EmpresaSucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "codigo_sucursal", nullable = false, length = 50)
    private String codigoSucursal;

    @Column(name = "nombre_sucursal", nullable = false, length = 200)
    private String nombreSucursal;

    @Column(name = "schema_name", unique = true, nullable = false, length = 50)
    private String schemaName; // Este es el tenant_id

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String provincia;

    @Column(length = 50)
    private String canton;

    @Column(length = 50)
    private String distrito;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coordenadas_gps", columnDefinition = "jsonb")
    private Map<String, Object> coordenadasGps;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    @Builder.Default
    @Column(name = "es_principal")
    private Boolean esPrincipal = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "horario_operacion", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> horarioOperacion = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configuracion = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UsuarioSucursal> usuarioSucursales = new HashSet<>();

    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<ConfiguracionAcceso> configuracionesAcceso = new HashSet<>();

    // Métodos helper
    public String getNombreCompleto() {
        return empresa.getNombreMostrar() + " - " + nombreSucursal;
    }

    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        if (direccion != null) sb.append(direccion);
        if (distrito != null) sb.append(", ").append(distrito);
        if (canton != null) sb.append(", ").append(canton);
        if (provincia != null) sb.append(", ").append(provincia);
        return sb.toString();
    }

    public boolean tieneConfiguracionAcceso() {
        return !configuracionesAcceso.isEmpty() && 
               configuracionesAcceso.stream().anyMatch(ConfiguracionAcceso::getActivo);
    }

    public int getCantidadUsuariosActivos() {
        return (int) usuarioSucursales.stream()
                .filter(UsuarioSucursal::getActivo)
                .count();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}