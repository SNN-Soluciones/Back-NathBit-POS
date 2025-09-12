package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FacturaInternaSearchRequest {
    private Long empresaId;
    private Long sucursalId;
    private String estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String numeroFactura;
    private Long clienteId;
}