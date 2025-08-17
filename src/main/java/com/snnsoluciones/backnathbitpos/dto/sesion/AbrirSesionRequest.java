package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AbrirSesionRequest {
    @NotNull(message = "Terminal es requerida")
    private Long terminalId;
    
    @NotNull(message = "Monto inicial es requerido")
    @Min(value = 0, message = "Monto inicial debe ser mayor o igual a 0")
    private BigDecimal montoInicial;
    
    private String observaciones;
}