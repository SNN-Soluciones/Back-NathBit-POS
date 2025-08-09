package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "usuarios")
@ToString(exclude = {"usuarioEmpresas"})
@EqualsAndHashCode(exclude = {"usuarioEmpresas"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolNombre rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Métodos de utilidad
    public boolean esRolSistema() {
        return rol == RolNombre.ROOT || rol == RolNombre.SOPORTE;
    }

    public boolean esRolEmpresarial() {
        return rol == RolNombre.SUPER_ADMIN;
    }

    public boolean esRolGerencial() {
        return rol == RolNombre.ADMIN;
    }

    public boolean esRolOperativo() {
        return rol == RolNombre.CAJERO || rol == RolNombre.MESERO
            || rol == RolNombre.COCINA || rol == RolNombre.JEFE_CAJAS;
    }
}