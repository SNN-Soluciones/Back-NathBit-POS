package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;


@Data @Builder
public class AlertasResponse {
    private List<Alerta> alertas;
    private int totalAlertas;
    @Data @Builder
    public static class Alerta {
        private String tipo;          // SIN_VENTAS | CAJA_ABIERTA_MUCHO | ERROR_FE
        private String severidad;     // info | warning | error
        private String empresa;
        private String sucursal;
        private String mensaje;
        private LocalDateTime desde;
    }
}