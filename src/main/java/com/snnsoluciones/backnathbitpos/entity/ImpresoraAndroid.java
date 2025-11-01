package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "impresoras_android")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpresoraAndroid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoImpresoraAndroid tipo;

    @Column(nullable = false, length = 50)
    private String ip;

    @Column(nullable = false)
    private Integer puerto = 9100;

    @Column(name = "ancho_papel", nullable = false)
    private Integer anchoPapel = 80;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_uso", length = 20)
    private TipoUsoImpresora tipoUso;

    @Column(nullable = false)
    private Boolean predeterminada = false;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creacion_id")
    private Usuario usuarioCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_actualizacion_id")
    private Usuario usuarioActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Enums internos
    public enum TipoImpresoraAndroid {
        RED,
        WIFI
    }

    public enum TipoUsoImpresora {
        FACTURAS,
        COCINA,
        BARRA,
        GENERAL
    }
}