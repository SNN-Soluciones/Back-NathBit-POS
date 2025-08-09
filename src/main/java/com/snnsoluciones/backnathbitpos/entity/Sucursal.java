package com.snnsoluciones.backnathbitpos.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
@Table(name = "sucursales", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"empresa_id", "codigo"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"empresa", "usuarioEmpresaRoles"})
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(name = "es_principal")
    @Builder.Default
    private Boolean esPrincipal = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configuracion = new HashMap<>();

    // Datos de ubicación
    @Column(length = 10)
    private String provincia;

    @Column(length = 10)
    private String canton;

    @Column(length = 10)
    private String distrito;

    @Column(length = 50)
    private String barrio;

    @Column(name = "otras_senas", columnDefinition = "TEXT")
    private String otrasSenas;

    // Horarios
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "dias_operacion", length = 20)
    @Builder.Default
    private String diasOperacion = "L-D"; // L-V, L-S, L-D

    // Capacidad
    @Column(name = "cantidad_mesas")
    @Builder.Default
    private Integer cantidadMesas = 0;

    @Column(name = "capacidad_personas")
    @Builder.Default
    private Integer capacidadPersonas = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Métodos de utilidad
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        if (direccion != null && !direccion.isEmpty()) {
            sb.append(direccion);
        }
        if (barrio != null && !barrio.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Barrio ").append(barrio);
        }
        if (otrasSenas != null && !otrasSenas.isEmpty()) {
            if (sb.length() > 0) sb.append(". ");
            sb.append(otrasSenas);
        }
        return sb.toString();
    }

    public boolean estaAbierta() {
        if (!activa || horaApertura == null || horaCierre == null) {
            return false;
        }
        
        LocalTime ahora = LocalTime.now();
        return ahora.isAfter(horaApertura) && ahora.isBefore(horaCierre);
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

    public String getCodigoCompleto() {
        return empresa.getCodigo() + "-" + codigo;
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