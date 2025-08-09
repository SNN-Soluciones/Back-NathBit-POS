package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Usuario actualizada con rol global único
 */
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuarios_email", columnList = "email"),
    @Index(name = "idx_usuarios_username", columnList = "username"),
    @Index(name = "idx_usuarios_rol", columnList = "rol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuarioEmpresas"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 20)
    private String identificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolNombre rol;  // ROL GLOBAL ÚNICO

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private TipoUsuario tipoUsuario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueado = false;

    @Column(name = "intentos_fallidos")
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "fecha_ultimo_intento")
    private LocalDateTime fechaUltimoIntento;

    @Column(name = "fecha_desbloqueo")
    private LocalDateTime fechaDesbloqueo;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "ultimo_cambio_password")
    private LocalDateTime ultimoCambioPassword;

    @Column(name = "password_temporal")
    @Builder.Default
    private Boolean passwordTemporal = false;

    @Column(name = "token_recuperacion")
    private String tokenRecuperacion;

    @Column(name = "fecha_token_recuperacion")
    private LocalDateTime fechaTokenRecuperacion;

    @Column(name = "foto_url")
    private String fotoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    // Métodos de utilidad

    /**
     * Verifica si el usuario es de tipo SISTEMA (ROOT o SOPORTE)
     */
    public boolean esRolSistema() {
        return rol == RolNombre.ROOT || rol == RolNombre.SOPORTE;
    }

    /**
     * Verifica si el usuario es de tipo administrativo
     */
    public boolean esRolAdministrativo() {
        return rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN;
    }

    /**
     * Verifica si el usuario es de tipo operativo
     */
    public boolean esRolOperativo() {
        return rol == RolNombre.CAJERO || rol == RolNombre.MESERO ||
            rol == RolNombre.JEFE_CAJAS || rol == RolNombre.COCINA;
    }

    /**
     * Verifica si el usuario requiere selección de contexto
     */
    public boolean requiereSeleccionContexto() {
        // ROOT y SOPORTE no requieren selección
        if (esRolSistema()) return false;

        // Los operativos no requieren selección si tienen una sola empresa/sucursal
        if (esRolOperativo() && usuarioEmpresas.size() == 1) {
            return false;
        }

        // SUPER_ADMIN y ADMIN siempre requieren selección
        // Operativos con múltiples asignaciones también
        return true;
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    public String getNombreCompleto() {
        if (apellidos != null && !apellidos.isEmpty()) {
            return nombre + " " + apellidos;
        }
        return nombre;
    }

    /**
     * Verifica si el usuario puede cambiar de contexto
     */
    public boolean puedeCambiarContexto() {
        return !esRolOperativo() || usuarioEmpresas.size() > 1;
    }
}