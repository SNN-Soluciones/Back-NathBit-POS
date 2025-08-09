package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "usuarios_empresas_roles",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "empresa_id", "sucursal_id"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuario", "empresa", "sucursal"})
public class UsuarioEmpresaRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal; // null = todas las sucursales de la empresa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolNombre rol;

    // PERMISOS JSON - Cambio principal del modelo
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Map<String, Boolean>> permisos;

    @Column(name = "es_principal")
    @Builder.Default
    private Boolean esPrincipal = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_asignacion")
    @CreationTimestamp
    private LocalDateTime fechaAsignacion;

    @Column(name = "asignado_por")
    private Long asignadoPor;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Métodos helper para permisos
    public boolean tienePermiso(String modulo, String accion) {
        // Si es ROOT o SUPER_ADMIN, tiene todos los permisos
        if (this.rol == RolNombre.ROOT || this.rol == RolNombre.SUPER_ADMIN) {
            return true;
        }

        // Si no tiene permisos personalizados, usar los default del rol
        Map<String, Map<String, Boolean>> permisosActuales = this.permisos;
        if (permisosActuales == null || permisosActuales.isEmpty()) {
            permisosActuales = getPermisosDefault(this.rol);
        }

        // Verificar permiso específico
        Map<String, Boolean> moduloPermisos = permisosActuales.get(modulo);
        return moduloPermisos != null && Boolean.TRUE.equals(moduloPermisos.get(accion));
    }

    public boolean esValido() {
        return this.activo &&
            this.usuario != null &&
            this.usuario.getActivo() &&
            this.empresa != null &&
            this.empresa.getActiva() &&
            (this.sucursal == null || this.sucursal.getActiva()) &&
            (this.fechaVencimiento == null || this.fechaVencimiento.isAfter(LocalDateTime.now()));
    }

    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.rol.getDisplayName());
        sb.append(" en ");
        sb.append(this.empresa.getNombre());

        if (this.sucursal != null) {
            sb.append(" - ").append(this.sucursal.getNombre());
        } else {
            sb.append(" (Todas las sucursales)");
        }

        return sb.toString();
    }

    // Permisos por defecto según rol
    public static Map<String, Map<String, Boolean>> getPermisosDefault(RolNombre rol) {
        Map<String, Map<String, Boolean>> permisos = new HashMap<>();

        switch (rol) {
            case ROOT:
            case SUPER_ADMIN:
                agregarTodosLosPermisos(permisos);
                break;

            case ADMIN:
                agregarPermisosAdmin(permisos);
                break;

            case JEFE_CAJAS:
                agregarPermisosJefeCajas(permisos);
                break;

            case CAJERO:
                agregarPermisosCajero(permisos);
                break;

            case MESERO:
                agregarPermisosMesero(permisos);
                break;

            case COCINA:
                agregarPermisosCocina(permisos);
                break;

            default:
                // Sin permisos por defecto
                break;
        }

        return permisos;
    }

    private static void agregarTodosLosPermisos(Map<String, Map<String, Boolean>> permisos) {
        String[] modulos = {"productos", "ordenes", "caja", "reportes", "clientes",
            "usuarios", "inventario", "proveedores", "mesas", "descuentos"};
        String[] acciones = {"ver", "crear", "editar", "eliminar", "autorizar"};

        for (String modulo : modulos) {
            Map<String, Boolean> accionesModulo = new HashMap<>();
            for (String accion : acciones) {
                accionesModulo.put(accion, true);
            }
            permisos.put(modulo, accionesModulo);
        }
    }

    private static void agregarPermisosAdmin(Map<String, Map<String, Boolean>> permisos) {
        // Admin tiene casi todos los permisos excepto algunos críticos
        agregarTodosLosPermisos(permisos);

        // Restricciones específicas para Admin
        permisos.get("usuarios").put("eliminar", false);
    }

    private static void agregarPermisosJefeCajas(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("caja", Map.of(
            "ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", true
        ));
        permisos.put("ordenes", Map.of(
            "ver", true, "crear", true, "editar", true, "eliminar", true, "autorizar", true
        ));
        permisos.put("reportes", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", false
        ));
        permisos.put("descuentos", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", true
        ));
        permisos.put("clientes", Map.of(
            "ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", false
        ));
    }

    private static void agregarPermisosCajero(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("caja", Map.of(
            "ver", true, "crear", true, "editar", false, "eliminar", false, "autorizar", false
        ));
        permisos.put("ordenes", Map.of(
            "ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", false
        ));
        permisos.put("productos", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", false
        ));
        permisos.put("clientes", Map.of(
            "ver", true, "crear", true, "editar", false, "eliminar", false, "autorizar", false
        ));
    }

    private static void agregarPermisosMesero(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("ordenes", Map.of(
            "ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", false
        ));
        permisos.put("mesas", Map.of(
            "ver", true, "crear", false, "editar", true, "eliminar", false, "autorizar", false
        ));
        permisos.put("productos", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", false
        ));
        permisos.put("clientes", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", false
        ));
    }

    private static void agregarPermisosCocina(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("ordenes", Map.of(
            "ver", true, "crear", false, "editar", true, "eliminar", false, "autorizar", false
        ));
        permisos.put("productos", Map.of(
            "ver", true, "crear", false, "editar", false, "eliminar", false, "autorizar", false
        ));
    }

    // equals y hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioEmpresaRol that = (UsuarioEmpresaRol) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}