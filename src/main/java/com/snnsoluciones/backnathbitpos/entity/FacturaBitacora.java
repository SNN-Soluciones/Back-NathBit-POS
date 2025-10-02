package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para gestionar el procesamiento asíncrono de facturas electrónicas
 * Maneja todo el ciclo: XML -> Firma -> Hacienda -> PDF -> Email
 */
@Entity
@Table(name = "factura_bitacora", indexes = {
    @Index(name = "idx_bitacora_factura", columnList = "factura_id", unique = true),
    @Index(name = "idx_bitacora_clave", columnList = "clave", unique = true),
    @Index(name = "idx_bitacora_estado", columnList = "estado"),
    @Index(name = "idx_bitacora_proximo", columnList = "proximo_intento"),
    @Index(name = "idx_bitacora_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID de la factura asociada
     */
    @Column(name = "factura_id", nullable = false, unique = true)
    private Long facturaId;

    /**
     * Clave numérica del documento (50 dígitos)
     */
    @Column(name = "clave", nullable = false, unique = true, length = 50)
    private String clave;

    /**
     * Estado actual del procesamiento
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoBitacora estado;

    /**
     * Número de intentos de procesamiento
     */
    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    /**
     * Próxima fecha/hora para intentar procesar (para reintentos)
     */
    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    // ========== RUTAS S3 ==========

    /**
     * Ruta del XML sin firmar en S3
     */
    @Column(name = "xml_path", length = 500)
    private String xmlPath;

    /**
     * Ruta del XML firmado en S3
     */
    @Column(name = "xml_firmado_path", length = 500)
    private String xmlFirmadoPath;

    /**
     * Ruta del XML de respuesta de Hacienda en S3
     */
    @Column(name = "xml_respuesta_path", length = 500)
    private String xmlRespuestaPath;

    // ========== RESPUESTA HACIENDA ==========

    /**
     * Mensaje de respuesta de Hacienda (aceptación/rechazo)
     */
    @Column(name = "hacienda_mensaje", columnDefinition = "TEXT")
    private String haciendaMensaje;

    // ========== TRACKING ERRORES ==========

    /**
     * Último error ocurrido durante el procesamiento
     */
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    // ========== TIMESTAMPS ==========

    /**
     * Fecha/hora de creación del registro
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha/hora de última actualización
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Fecha/hora cuando se completó el procesamiento (aceptada/rechazada)
     */
    @Column(name = "procesado_at")
    private LocalDateTime procesadoAt;

    // ========== LIFECYCLE CALLBACKS ==========

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoBitacora.PENDIENTE;
        }
        if (intentos == null) {
            intentos = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== MÉTODOS HELPER ==========

    /**
     * Verifica si se puede reintentar el procesamiento
     */
    public boolean puedeReintentar(int maxIntentos) {
        return intentos < maxIntentos &&
            (estado == EstadoBitacora.PENDIENTE || estado == EstadoBitacora.ERROR);
    }

    /**
     * Incrementa los intentos y calcula el próximo reintento con backoff exponencial
     */
    public void incrementarIntentos() {
        this.intentos++;
        // Backoff exponencial: 5min, 10min, 20min, 40min...
        int minutosEspera = 5 * (int) Math.pow(2, this.intentos - 1);
        // Cap máximo de 60 minutos
        minutosEspera = Math.min(minutosEspera, 60);
        this.proximoIntento = LocalDateTime.now().plusMinutes(minutosEspera);
    }

    /**
     * Marca como completado exitosamente
     */
    public void marcarCompletado(EstadoBitacora estadoFinal) {
        this.estado = estadoFinal;
        this.procesadoAt = LocalDateTime.now();
        this.proximoIntento = null;
    }

    /**
     * Verifica si está en un estado final
     */
    public boolean estaFinalizado() {
        return estado == EstadoBitacora.ACEPTADA ||
            estado == EstadoBitacora.RECHAZADA ||
            (estado == EstadoBitacora.ERROR && intentos >= 3);
    }
}