package com.snnsoluciones.backnathbitpos.entity.global;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa la relación entre un usuario y una empresa,
 * incluyendo el rol del usuario en esa empresa.
 */
@Entity
@Table(name = "usuario_empresas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "empresa", "usuarioSucursales"})
@EqualsAndHashCode(exclude = {"usuario", "empresa", "usuarioSucursales"})
@IdClass(UsuarioEmpresa.UsuarioEmpresaId.class)
public class UsuarioEmpresa {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioGlobal usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RolNombre rol;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por")
    private UsuarioGlobal asignadoPor;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(name = "es_propietario")
    private Boolean esPropietario = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuracion_rol", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configuracionRol = Map.of();

    // Relaciones
    @OneToMany(mappedBy = "usuarioEmpresa", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UsuarioSucursal> usuarioSucursales = new HashSet<>();

    // Métodos helper
    public boolean estaVigente() {
        if (!activo) return false;
        if (fechaExpiracion == null) return true;
        return fechaExpiracion.isAfter(LocalDateTime.now());
    }

    public boolean puedeAdministrar() {
        return rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN;
    }

    public boolean puedeGestionarCajas() {
        return puedeAdministrar() || rol == RolNombre.JEFE_CAJAS;
    }

    public boolean puedeCobrar() {
        return puedeGestionarCajas() || rol == RolNombre.CAJERO;
    }

    public boolean puedeTomarOrdenes() {
        return puedeCobrar() || rol == RolNombre.MESERO;
    }

    public Set<EmpresaSucursal> getSucursalesConAcceso() {
        Set<EmpresaSucursal> sucursales = new HashSet<>();
        usuarioSucursales.stream()
                .filter(UsuarioSucursal::getActivo)
                .forEach(us -> sucursales.add(us.getSucursal()));
        return sucursales;
    }

    public UsuarioSucursal getAccesoSucursalPrincipal() {
        return usuarioSucursales.stream()
                .filter(UsuarioSucursal::getActivo)
                .filter(UsuarioSucursal::getEsPrincipal)
                .findFirst()
                .orElse(null);
    }

    // Clase para clave compuesta
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioEmpresaId implements Serializable {
        private UUID usuario;
        private UUID empresa;
    }
}