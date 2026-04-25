package com.snnsoluciones.backnathbitpos.entity;
 
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "kiosco_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KioscoConfig {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "sucursal_id", nullable = false, unique = true)
    private Long sucursalId;
 
    // ── Template ──────────────────────────────────────────────────────────
    @Column(name = "template_id", length = 20, nullable = false)
    @Builder.Default
    private String templateId = "CLEAN_LIGHT";
    // Valores: FAST_FOOD | DARK_PREMIUM | CLEAN_LIGHT | BRANDED
 
    // ── 9 Design Tokens ───────────────────────────────────────────────────
    @Column(name = "color_primary",         length = 7) @Builder.Default private String colorPrimary        = "#1F4E79";
    @Column(name = "color_secondary",       length = 7) @Builder.Default private String colorSecondary      = "#2E75B6";
    @Column(name = "color_background",      length = 7) @Builder.Default private String colorBackground     = "#FFFFFF";
    @Column(name = "color_surface",         length = 7) @Builder.Default private String colorSurface        = "#F4F4F4";
    @Column(name = "color_text_primary",    length = 7) @Builder.Default private String colorTextPrimary    = "#222222";
    @Column(name = "color_text_secondary",  length = 7) @Builder.Default private String colorTextSecondary  = "#888888";
    @Column(name = "color_accent",          length = 7) @Builder.Default private String colorAccent         = "#FFD60A";
    @Column(name = "color_success",         length = 7) @Builder.Default private String colorSuccess        = "#2EC4B6";
    @Column(name = "color_danger",          length = 7) @Builder.Default private String colorDanger         = "#E71D36";
 
    // ── Assets ────────────────────────────────────────────────────────────
    @Column(name = "logo_url",             length = 500) private String logoUrl;
    @Column(name = "imagen_bienvenida_url",length = 500) private String imagenBienvenidaUrl;
 
    @Column(name = "texto_bienvenida",     length = 200)
    @Builder.Default
    private String textoBienvenida = "¡Bienvenido! Toca para comenzar";
 
    // ── Comportamiento ────────────────────────────────────────────────────
    @Column(name = "tiempo_inactividad")
    @Builder.Default
    private Integer tiempoInactividad = 60; // segundos
 
    @Column(name = "mostrar_precios", nullable = false)
    @Builder.Default
    private Boolean mostrarPrecios = true;
 
    @Column(name = "requiere_pago_en_caja", nullable = false)
    @Builder.Default
    private Boolean requierePagoEnCaja = true;
 
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}