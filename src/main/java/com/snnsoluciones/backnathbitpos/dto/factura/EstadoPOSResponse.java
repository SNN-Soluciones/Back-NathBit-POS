package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EstadoPOSResponse {
    private Long terminalId;
    private String terminalNombre;
    private boolean terminalActiva;
    private Long sucursalId;
    private String sucursalNombre;
    private boolean sesionAbierta;
    private Long sesionId;
    private String cajeroNombre;
    private BigDecimal montoApertura;
    private LocalDateTime fechaApertura;
}