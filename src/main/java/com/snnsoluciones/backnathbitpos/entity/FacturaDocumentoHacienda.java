package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar XMLs y PDFs del proceso con Hacienda
 * Guarda toda la documentación electrónica generada
 * Parte de la Arquitectura La Jachuda 🚀
 */
@Data
@Entity
@Table(name = "factura_documento_hacienda")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"factura", "xmlGenerado", "xmlFirmado", "xmlRespuesta", "pdfGenerado"})
public class FacturaDocumentoHacienda {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;
    
    /**
     * XML original generado (sin firmar)
     */
    @Lob
    @Column(name = "xml_generado", columnDefinition = "TEXT")
    private String xmlGenerado;
    
    @Column(name = "fecha_xml_generado")
    private LocalDateTime fechaXmlGenerado;
    
    /**
     * XML firmado digitalmente
     */
    @Lob
    @Column(name = "xml_firmado", columnDefinition = "TEXT")
    private String xmlFirmado;
    
    @Column(name = "fecha_xml_firmado")
    private LocalDateTime fechaXmlFirmado;
    
    /**
     * XML de respuesta de Hacienda
     */
    @Lob
    @Column(name = "xml_respuesta", columnDefinition = "TEXT")
    private String xmlRespuesta;
    
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
    
    /**
     * Estado de la respuesta de Hacienda
     * 1 - Aceptado
     * 2 - Aceptado con advertencias
     * 3 - Rechazado
     */
    @Column(name = "estado_hacienda", length = 1)
    private String estadoHacienda;
    
    /**
     * Mensaje de respuesta de Hacienda
     */
    @Column(name = "mensaje_hacienda", columnDefinition = "TEXT")
    private String mensajeHacienda;
    
    /**
     * Detalle de advertencias o errores
     */
    @Column(name = "detalle_respuesta", columnDefinition = "TEXT")
    private String detalleRespuesta;
    
    /**
     * PDF generado (base64)
     */
    @Lob
    @Column(name = "pdf_generado", columnDefinition = "TEXT")
    private byte[] pdfGenerado;
    
    @Column(name = "fecha_pdf_generado")
    private LocalDateTime fechaPdfGenerado;
    
    /**
     * Indica si se envió el correo al cliente
     */
    @Column(name = "correo_enviado", nullable = false)
    private Boolean correoEnviado = false;
    
    @Column(name = "fecha_correo_enviado")
    private LocalDateTime fechaCorreoEnviado;
    
    @Column(name = "correo_destino")
    private String correoDestino;
    
    /**
     * Número de consulta en Hacienda
     */
    @Column(name = "numero_consulta_hacienda")
    private String numeroConsultaHacienda;
    
    /**
     * URL del documento en Hacienda (si aplica)
     */
    @Column(name = "url_documento_hacienda")
    private String urlDocumentoHacienda;
    
    // Auditoría
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Métodos helper
    
    /**
     * Verifica si el documento fue aceptado
     */
    public boolean esAceptado() {
        return "1".equals(estadoHacienda) || "2".equals(estadoHacienda);
    }
    
    /**
     * Verifica si el documento fue rechazado
     */
    public boolean esRechazado() {
        return "3".equals(estadoHacienda);
    }
    
    /**
     * Verifica si tiene advertencias
     */
    public boolean tieneAdvertencias() {
        return "2".equals(estadoHacienda);
    }
    
    /**
     * Actualiza con la respuesta de Hacienda
     */
    public void actualizarConRespuesta(String xmlRespuesta, String estado, String mensaje, String detalle) {
        this.xmlRespuesta = xmlRespuesta;
        this.fechaRespuesta = LocalDateTime.now();
        this.estadoHacienda = estado;
        this.mensajeHacienda = mensaje;
        this.detalleRespuesta = detalle;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Registra el envío del correo
     */
    public void marcarCorreoEnviado(String destinatario) {
        this.correoEnviado = true;
        this.fechaCorreoEnviado = LocalDateTime.now();
        this.correoDestino = destinatario;
        this.updatedAt = LocalDateTime.now();
    }
}