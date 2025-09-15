package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "mensaje_receptor_bitacora")
@Data
public class MensajeReceptorBitacora {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "compra_id", nullable = false)
    private Long compraId;
    
    @Column(name = "clave", nullable = false, length = 50)
    private String clave; // Clave del documento recibido
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoBitacora estado;
    
    @Column(name = "tipo_mensaje", length = 2)
    private String tipoMensaje; // 05, 06, 07

    @Column(name = "justificacion")
    private String justificacion;
    
    @Column(name = "consecutivo", length = 20)
    private String consecutivo;
    
    // Rutas de archivos
    @Column(name = "xml_path")
    private String xmlPath;
    
    @Column(name = "xml_firmado_path")
    private String xmlFirmadoPath;
    
    @Column(name = "xml_respuesta_path")
    private String xmlRespuestaPath;
    
    // Control de reintentos
    @Column(name = "intentos")
    private Integer intentos = 0;
    
    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;
    
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;
    
    @Column(name = "hacienda_mensaje", columnDefinition = "TEXT")
    private String haciendaMensaje;
    
    // Auditoría
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}