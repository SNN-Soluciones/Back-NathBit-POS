package com.snnsoluciones.backnathbitpos.entity.global;

import com.snnsoluciones.backnathbitpos.enums.TipoDeteccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad que representa las reglas de detección automática de sucursal.
 * Permite configurar cómo se detecta automáticamente a qué sucursal pertenece un acceso.
 */
@Entity
@Table(name = "configuracion_acceso", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sucursal"})
@EqualsAndHashCode(exclude = {"sucursal"})
public class ConfiguracionAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private EmpresaSucursal sucursal;

    @Column(name = "tipo_deteccion", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoDeteccion tipoDeteccion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuracion;

    @Column(length = 200)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer prioridad = 100;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos helper para acceder a configuraciones específicas
    public String getIpInicio() {
        if (tipoDeteccion != TipoDeteccion.IP_RANGE) return null;
        return (String) configuracion.get("ip_inicio");
    }

    public String getIpFin() {
        if (tipoDeteccion != TipoDeteccion.IP_RANGE) return null;
        return (String) configuracion.get("ip_fin");
    }

    public String getTerminalId() {
        if (tipoDeteccion != TipoDeteccion.TERMINAL_ID) return null;
        return (String) configuracion.get("terminal_id");
    }

    public String getMacAddress() {
        if (tipoDeteccion != TipoDeteccion.TERMINAL_ID) return null;
        return (String) configuracion.get("mac_address");
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}