package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "empresa_actividades",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "actividad_id"}))
@ToString(exclude = {"empresa", "actividad"})
public class EmpresaActividad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private ActividadEconomica actividad;

    @Column(name = "codigo_actividad", length = 10)
    private String codigoActividad;

    @Column(name = "descripcion_actividad", length = 500)
    private String descripcionActividad;
    
    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;
    
    @Column(name = "orden")
    private Integer orden = 0;
    
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
    
    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        
        // Solo puede haber una actividad principal
        if (esPrincipal && empresa != null) {
            empresa.getActividades().stream()
                .filter(a -> !a.equals(this) && Boolean.TRUE.equals(a.getEsPrincipal()))
                .forEach(a -> a.setEsPrincipal(false));
        }
    }
}