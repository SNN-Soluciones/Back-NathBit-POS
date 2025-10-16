package com.snnsoluciones.backnathbitpos.dto.sesion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FondoCajaResponse {
    private Long terminalId;
    private String terminalNombre;
    private BigDecimal fondoCaja;
    private Long ultimaSesionId;
    private LocalDateTime fechaUltimaSesion;
    private String mensaje; // Ej: "Fondo disponible de última sesión cerrada"
}