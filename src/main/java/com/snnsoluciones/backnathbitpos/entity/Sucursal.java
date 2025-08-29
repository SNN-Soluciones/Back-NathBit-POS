package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "sucursales")
@ToString(exclude = {"empresa", "usuarioEmpresas", "terminales", "provincia", "canton", "distrito", "barrio"})
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY)
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    // ===== NUEVOS CAMPOS PARA FACTURACIÓN =====

    // Número para formar el consecutivo (001, 002, etc.)
    @Column(name = "numero_sucursal", length = 3, nullable = false)
    private String numeroSucursal;

    // Modo de facturación
    @Enumerated(EnumType.STRING)
    @Column(name = "modo_facturacion", nullable = false)
    private ModoFacturacion modoFacturacion = ModoFacturacion.SOLO_INTERNO;

    // Ubicación (puede ser diferente a la empresa)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id")
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canton_id")
    private Canton canton;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distrito_id")
    private Distrito distrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id")
    private Barrio barrio;

    @Column(name = "otras_senas", length = 500)
    private String otrasSenas;

    // ===== NUEVA RELACIÓN =====
    @OneToMany(mappedBy = "sucursal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Terminal> terminales = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Generar número de sucursal automático si no se proporciona
        if (numeroSucursal == null) {
            numeroSucursal = generarNumeroSucursal();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PreRemove
    protected void preRemove() {
        // Validar que no tenga terminales activas
        if (terminales != null && terminales.stream().anyMatch(Terminal::getActiva)) {
            throw new IllegalStateException("No se puede eliminar una sucursal con terminales activas");
        }
    }

    /**
     * Genera el próximo número de sucursal disponible
     */
    private String generarNumeroSucursal() {
        if (empresa == null || empresa.getSucursales() == null) {
            return "001";
        }

        int maxNumero = empresa.getSucursales().stream()
            .filter(s -> s.getNumeroSucursal() != null)
            .mapToInt(s -> {
                try {
                    return Integer.parseInt(s.getNumeroSucursal());
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max()
            .orElse(0);

        return String.format("%03d", maxNumero + 1);
    }

    /**
     * Genera el próximo número de terminal disponible
     */
    public String generarProximoNumeroTerminal() {
        int maxNumero = terminales.stream()
            .filter(t -> t.getNumeroTerminal() != null)
            .mapToInt(t -> {
                try {
                    return Integer.parseInt(t.getNumeroTerminal());
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max()
            .orElse(0);

        return String.format("%05d", maxNumero + 1);
    }

    /**
     * Valida si puede usar facturación electrónica
     */
    public boolean puedeFacturarElectronicamente() {
        return modoFacturacion == ModoFacturacion.ELECTRONICO &&
            empresa.getRequiereHacienda() != null &&
            empresa.getRequiereHacienda();
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
        Sucursal sucursal = (Sucursal) o;
        return getId() != null && Objects.equals(getId(), sucursal.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}