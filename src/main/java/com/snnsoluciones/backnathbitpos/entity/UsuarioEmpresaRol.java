package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

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

    // Métodos de utilidad para permisos
    public boolean tienePermiso(String modulo, String accion) {
        // Si es ROOT, tiene todos los permisos
        if (rol == RolNombre.ROOT) {
            return true;
        }

        // Si es SUPER_ADMIN y es su empresa, tiene todos los permisos
        if (rol == RolNombre.SUPER_ADMIN) {
            return true;
        }

        // Permisos por defecto según rol
        return obtenerPermisosPorDefecto(modulo, accion);
    }

    private boolean obtenerPermisosPorDefecto(String modulo, String accion) {
        switch (rol) {
            case ADMIN:
                // Admin tiene todos los permisos en su empresa
                return true;
                
            case JEFE_CAJAS:
                // Jefe de cajas tiene permisos específicos
                if ("caja".equals(modulo)) return true;
                if ("reportes".equals(modulo) && "ver".equals(accion)) return true;
                if ("ordenes".equals(modulo)) return true;
                if ("descuentos".equals(modulo) && "autorizar".equals(accion)) return true;
                return false;
                
            case CAJERO:
                // Cajero tiene permisos limitados
                if ("ordenes".equals(modulo) && !"eliminar".equals(accion)) return true;
                if ("caja".equals(modulo) && !"cerrar".equals(accion)) return true;
                if ("clientes".equals(modulo) && ("ver".equals(accion) || "crear".equals(accion))) return true;
                return false;
                
            case MESERO:
                // Mesero solo puede manejar órdenes
                if ("ordenes".equals(modulo) && ("ver".equals(accion) || "crear".equals(accion) || "editar".equals(accion))) return true;
                if ("mesas".equals(modulo) && "ver".equals(accion)) return true;
                if ("productos".equals(modulo) && "ver".equals(accion)) return true;
                return false;
                
            case COCINA:
                // Cocina solo ve órdenes
                if ("ordenes".equals(modulo) && "ver".equals(accion)) return true;
                if ("productos".equals(modulo) && "ver".equals(accion)) return true;
                return false;
                
            default:
                return false;
        }
    }

    public boolean esValido() {
        return activo && 
               (fechaVencimiento == null || fechaVencimiento.isAfter(LocalDateTime.now()));
    }

    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append(rol.name());
        sb.append(" en ").append(empresa.getNombre());
        
        if (sucursal != null) {
            sb.append(" - ").append(sucursal.getNombre());
        } else {
            sb.append(" (Todas las sucursales)");
        }
        
        return sb.toString();
    }

    public boolean aplicaASucursal(Long sucursalId) {
        // Si no tiene sucursal específica, aplica a todas
        if (this.sucursal == null) {
            return true;
        }
        // Si tiene sucursal específica, solo aplica a esa
        return this.sucursal.getId().equals(sucursalId);
    }

    // Estructura de permisos sugerida
    public static Map<String, Map<String, Boolean>> getPermisosDefault(RolNombre rol) {
        Map<String, Map<String, Boolean>> permisos = new HashMap<>();
        
        switch (rol) {
            case ROOT:
            case SUPER_ADMIN:
            case ADMIN:
                // Todos los permisos
                agregarTodosLosPermisos(permisos);
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

    private static void agregarPermisosJefeCajas(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("caja", Map.of("ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", true));
        permisos.put("ordenes", Map.of("ver", true, "crear", true, "editar", true, "eliminar", true));
        permisos.put("reportes", Map.of("ver", true, "crear", false, "editar", false, "eliminar", false));
        permisos.put("descuentos", Map.of("ver", true, "crear", true, "editar", true, "eliminar", false, "autorizar", true));
        permisos.put("clientes", Map.of("ver", true, "crear", true, "editar", true, "eliminar", false));
    }

    private static void agregarPermisosCajero(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("caja", Map.of("ver", true, "crear", true, "editar", false, "eliminar", false));
        permisos.put("ordenes", Map.of("ver", true, "crear", true, "editar", true, "eliminar", false));
        permisos.put("clientes", Map.of("ver", true, "crear", true, "editar", false, "eliminar", false));
        permisos.put("productos", Map.of("ver", true, "crear", false, "editar", false, "eliminar", false));
    }

    private static void agregarPermisosMesero(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("ordenes", Map.of("ver", true, "crear", true, "editar", true, "eliminar", false));
        permisos.put("mesas", Map.of("ver", true, "crear", false, "editar", true, "eliminar", false));
        permisos.put("productos", Map.of("ver", true, "crear", false, "editar", false, "eliminar", false));
    }

    private static void agregarPermisosCocina(Map<String, Map<String, Boolean>> permisos) {
        permisos.put("ordenes", Map.of("ver", true, "crear", false, "editar", true, "eliminar", false));
        permisos.put("productos", Map.of("ver", true, "crear", false, "editar", false, "eliminar", false));
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
        UsuarioEmpresaRol that = (UsuarioEmpresaRol) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}