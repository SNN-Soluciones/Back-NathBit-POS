package com.snnsoluciones.backnathbitpos.entity.global;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa los permisos específicos de un usuario en una sucursal.
 * Define qué puede hacer el usuario en cada sucursal específica.
 */
@Entity
@Table(name = "usuario_sucursales", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuarioEmpresa", "sucursal", "asignadoPor"})
@EqualsAndHashCode(exclude = {"usuarioEmpresa", "sucursal", "asignadoPor"})
@IdClass(UsuarioSucursal.UsuarioSucursalId.class)
public class UsuarioSucursal {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private EmpresaSucursal sucursal;

    // Relación con UsuarioEmpresa usando las claves foráneas
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "usuario_id", referencedColumnName = "usuario_id", insertable = false, updatable = false),
        @JoinColumn(name = "empresa_id", referencedColumnName = "empresa_id", insertable = false, updatable = false)
    })
    private UsuarioEmpresa usuarioEmpresa;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Builder.Default
    @Column(name = "puede_leer", nullable = false)
    private Boolean puedeLeer = true;

    @Builder.Default
    @Column(name = "puede_escribir", nullable = false)
    private Boolean puedeEscribir = true;

    @Builder.Default
    @Column(name = "puede_eliminar", nullable = false)
    private Boolean puedeEliminar = false;

    @Builder.Default
    @Column(name = "puede_aprobar", nullable = false)
    private Boolean puedeAprobar = false;

    @Builder.Default
    @Column(name = "es_principal")
    private Boolean esPrincipal = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por")
    private UsuarioGlobal asignadoPor;

    // Métodos helper
    public boolean tienePermisoCompleto() {
        return puedeLeer && puedeEscribir && puedeEliminar && puedeAprobar;
    }

    public boolean esSoloLectura() {
        return puedeLeer && !puedeEscribir && !puedeEliminar && !puedeAprobar;
    }

    public RolNombre getRol() {
        return usuarioEmpresa != null ? usuarioEmpresa.getRol() : null;
    }

    public UsuarioGlobal getUsuario() {
        return usuarioEmpresa != null ? usuarioEmpresa.getUsuario() : null;
    }

    public String getDescripcionPermisos() {
        StringBuilder permisos = new StringBuilder();
        if (puedeLeer) permisos.append("Lectura, ");
        if (puedeEscribir) permisos.append("Escritura, ");
        if (puedeEliminar) permisos.append("Eliminación, ");
        if (puedeAprobar) permisos.append("Aprobación, ");
        
        // Remover última coma
        if (permisos.length() > 2) {
            permisos.setLength(permisos.length() - 2);
        }
        
        return permisos.toString();
    }

    // Clase para clave compuesta
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioSucursalId implements Serializable {
        private UUID usuarioId;
        private UUID sucursal;
    }
}