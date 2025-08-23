package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes_exoneraciones",
    indexes = {
        @Index(name = "idx_exoneracion_cliente_activo", columnList = "cliente_id, activo")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteExoneracion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 2)
    private TipoDocumentoExoneracion tipoDocumento;
    
    @Column(name = "numero_documento", nullable = false, length = 50)
    private String numeroDocumento;
    
    @Column(name = "nombre_institucion", nullable = false, length = 100)
    private String nombreInstitucion;
    
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;
    
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    
    @Column(name = "porcentaje_exoneracion", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeExoneracion; // 0.00 a 100.00
    
    @Column(name = "categoria_compra", length = 100)
    private String categoriaCompra; // Ej: "Productos básicos", "Todo", etc.
    
    @Column(name = "monto_maximo", precision = 15, scale = 2)
    private BigDecimal montoMaximo; // Límite de exoneración si aplica
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @Column(columnDefinition = "TEXT")
    private String observaciones;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "codigo_autorizacion", length = 50, nullable = false)
    private String codigoAutorizacion;

    @Column(name = "numero_autorizacion")
    private Integer numeroAutorizacion; // opcional

    @Column(name = "posee_cabys", nullable = false)
    @Builder.Default
    private Boolean poseeCabys = false;

    @Column(name = "verificado_en")
    private LocalDateTime verificadoEn;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "origen", length = 30)
    private String origen; // ej. "HACIENDA_API"

    @OneToMany(mappedBy = "exoneracion", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ClienteExoneracionCabys> cabysAutorizados = new java.util.HashSet<>();
    
    public boolean estaVigente() {
        if (!activo) return false;
        if (fechaVencimiento == null) return true;
        return LocalDate.now().isBefore(fechaVencimiento) || 
               LocalDate.now().isEqual(fechaVencimiento);
    }

    public boolean aplicaParaCabys(String codigoCabys, java.util.function.Function<String, Boolean> existsInLista) {
        // No vigente o no activa => no aplica
        if (!estaVigente()) return false;

        // Si no posee lista de CAByS, aplica a todos (según política que definas)
        if (Boolean.FALSE.equals(poseeCabys)) return true;

        // Si posee lista, valida pertenencia
        return existsInLista.apply(codigoCabys);
    }
}