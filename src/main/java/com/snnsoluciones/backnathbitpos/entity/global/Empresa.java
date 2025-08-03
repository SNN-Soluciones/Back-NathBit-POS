package com.snnsoluciones.backnathbitpos.entity.global;

import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import com.snnsoluciones.backnathbitpos.enums.TipoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

/**
 * Entidad que representa una empresa/franquicia en el sistema.
 * Una empresa puede tener múltiples sucursales (tenants).
 */
@Entity
@Table(name = "empresas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sucursales", "usuarioEmpresas"})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(length = 50)
    private String provincia;

    @Column(length = 50)
    private String canton;

    @Column(length = 50)
    private String distrito;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoEmpresa tipo = TipoEmpresa.RESTAURANTE;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configuracion = Map.of();

    @Column(name = "plan_suscripcion", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanSuscripcion planSuscripcion = PlanSuscripcion.BASICO;

    @Column(name = "fecha_vencimiento_plan")
    private LocalDate fechaVencimientoPlan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "tipo_identificacion")
    private TipoIdentificacion tipoIdentificacion = TipoIdentificacion.CEDULA_JURIDICA;

    @Column(name = "identificacion")
    private String identificacion;

    @Column(name = "tipo_facturacion")
    private TipoFacturacion tipoFacturacion = TipoFacturacion.REGIMEN_TRADICIONAL;

    // Relaciones
    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<EmpresaSucursal> sucursales = new HashSet<>();

    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    // Métodos helper
    public String getNombreMostrar() {
        return nombreComercial != null ? nombreComercial : nombre;
    }

    public boolean tieneSucursalesActivas() {
        return sucursales.stream().anyMatch(EmpresaSucursal::getActiva);
    }

    public EmpresaSucursal getSucursalPrincipal() {
        return sucursales.stream()
                .filter(EmpresaSucursal::getEsPrincipal)
                .findFirst()
                .orElse(null);
    }

    public int getCantidadSucursalesActivas() {
        return (int) sucursales.stream()
                .filter(EmpresaSucursal::getActiva)
                .count();
    }

    public int getCantidadUsuariosActivos() {
        return (int) usuarioEmpresas.stream()
                .filter(UsuarioEmpresa::getActivo)
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

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        Empresa empresa = (Empresa) o;
        return getId() != null && Objects.equals(getId(), empresa.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}