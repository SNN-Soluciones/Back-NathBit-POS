package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.PlanSuscripcion;
import com.snnsoluciones.backnathbitpos.enums.TipoEmpresa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "empresas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"sucursales", "usuarioEmpresaRoles"})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    @Column(name = "cedula_juridica", length = 50)
    private String cedulaJuridica;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoEmpresa tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_suscripcion", length = 30)
    @Builder.Default
    private PlanSuscripcion planSuscripcion = PlanSuscripcion.BASICO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configuracion = new HashMap<>();

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "fecha_inicio_operaciones")
    private LocalDateTime fechaInicioOperaciones;

    @Column(name = "zona_horaria", length = 50)
    @Builder.Default
    private String zonaHoraria = "America/Costa_Rica";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "plan")
    private PlanSuscripcion plan = PlanSuscripcion.BASICO;

    // Relaciones
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Sucursal> sucursales = new HashSet<>();

    @Column(name = "limite_usuarios")
    private Integer limiteUsuarios;

    @Column(name = "limite_sucursales")
    private Integer limiteSucursales;

    // Métodos de utilidad
    public void agregarSucursal(Sucursal sucursal) {
        sucursales.add(sucursal);
        sucursal.setEmpresa(this);
    }

    public void removerSucursal(Sucursal sucursal) {
        sucursales.remove(sucursal);
        sucursal.setEmpresa(null);
    }

    public boolean tieneSucursalActiva() {
        return sucursales.stream().anyMatch(Sucursal::getActiva);
    }

    public int getCantidadSucursalesActivas() {
        return (int) sucursales.stream()
                .filter(Sucursal::getActiva)
                .count();
    }

    public int getCantidadUsuariosActivos() {
        return (int) usuarioEmpresaRoles.stream()
                .filter(UsuarioEmpresaRol::getActivo)
                .count();
    }

    // Métodos para configuración
    public void setConfiguracion(String key, Object value) {
        if (configuracion == null) {
            configuracion = new HashMap<>();
        }
        configuracion.put(key, value);
    }

    public Object getConfiguracion(String key) {
        return configuracion != null ? configuracion.get(key) : null;
    }

    public <T> T getConfiguracion(String key, Class<T> type) {
        Object value = getConfiguracion(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public String getNombreParaMostrar() {
        return nombreComercial != null && !nombreComercial.isEmpty() 
            ? nombreComercial 
            : nombre;
    }

    public boolean tienelogo() {
        return logoUrl != null && !logoUrl.isEmpty();
    }

    public String getLogoOrDefault() {
        return tienelogo() ? logoUrl : "/assets/images/default-logo.png";
    }
}