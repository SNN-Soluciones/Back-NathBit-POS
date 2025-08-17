package com.snnsoluciones.backnathbitpos.dto.sesion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SesionCajaResponse {
    private Long id;
    private Long terminalId;
    private String terminalNombre;
    private String sucursalNombre;
    private String usuarioNombre;
    private LocalDateTime fechaApertura;
    private BigDecimal montoInicial;
    private BigDecimal totalVentas;
    private String estado;
    private Integer cantidadFacturas;
}