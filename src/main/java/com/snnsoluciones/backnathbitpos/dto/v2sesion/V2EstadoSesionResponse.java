// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2EstadoSesionResponse.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2EstadoSesionResponse {

    // Sesión
    private Long          sesionId;
    private String        terminal;
    private String        modoGaveta;
    private String        estado;
    private BigDecimal    montoInicial;
    private LocalDateTime fechaApertura;

    // Mi turno (del usuario que consulta)
    private MiTurnoDTO    miTurno;

    // Otros cajeros activos (sin montos — por privacidad)
    private List<OtroTurnoDTO> otrosTurnos;

    // Totales de la sesión completa
    private BigDecimal    totalEfectivo;
    private BigDecimal    totalTarjeta;
    private BigDecimal    totalSinpe;
    private BigDecimal    totalTransferencia;
    private BigDecimal    totalCredito;

    // Movimientos de la sesión
    private List<V2MovimientoResponse> movimientos;

    // ── Inner DTOs ────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiTurnoDTO {
        private Long       turnoId;
        private BigDecimal fondoInicio;
        private BigDecimal ventasEfectivo;
        private BigDecimal ventasTarjeta;
        private BigDecimal ventasSinpe;
        private BigDecimal ventasTransferencia;
        private BigDecimal ventasCredito;
        private BigDecimal montoEsperado;    // solo visible para supervisor
        private LocalDateTime fechaInicio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtroTurnoDTO {
        private Long   turnoId;
        private String cajeroNombre;
        private String estado;
        private LocalDateTime fechaInicio;
        // Sin montos — arqueo ciego
    }
}