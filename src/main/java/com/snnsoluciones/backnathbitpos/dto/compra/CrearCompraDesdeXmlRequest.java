package com.snnsoluciones.backnathbitpos.dto.compra;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

// DTO para crear compra desde XML
@Data
public class CrearCompraDesdeXmlRequest {
    private Long proveedorId;
    private String xmlContent; // Base64 o XML plano
    private String observaciones;
    private Boolean procesarInventario = true; // Si actualiza inventario automáticamente
    private Boolean crearProductosSiNoExisten = false; // Si crea productos nuevos

    // Campos para mensaje receptor
    @Pattern(regexp = "^(05|06|07)$", message = "Tipo de mensaje inválido (05=Aceptación, 06=Aceptación parcial, 07=Rechazo)")
    private String tipoMensajeReceptor = "05"; // Por defecto aceptación total

    @Size(max = 160, message = "La justificación no puede exceder 160 caracteres")
    private String justificacionRechazo; // Requerido si es rechazo (07) o parcial (06)

    private BigDecimal montoTotalImpuestoAceptado; // Solo para aceptación parcial (06)
}