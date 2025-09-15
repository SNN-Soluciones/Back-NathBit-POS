// CrearMensajeReceptorRequest.java
package com.snnsoluciones.backnathbitpos.dto.mensajereceptor;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class CrearMensajeReceptorRequest {
    
    @NotNull(message = "El tipo de mensaje es requerido")
    @Pattern(regexp = "^(05|06|07)$", message = "Tipo de mensaje inválido")
    private String tipoMensaje; // 05=Aceptación, 06=Parcial, 07=Rechazo
    
    @Size(min = 5, max = 160, message = "El detalle debe tener entre 5 y 160 caracteres")
    private String detalleMensaje; // Requerido si es rechazo o parcial
    
    // Solo para aceptación parcial (06)
    private BigDecimal montoTotalImpuestoAceptado;
    
    // Si queremos procesar de inmediato o dejar pendiente
    private Boolean procesarInmediatamente = false;
}