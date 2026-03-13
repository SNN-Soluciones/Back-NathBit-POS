package com.snnsoluciones.backnathbitpos.dto.sesiones;

import com.snnsoluciones.backnathbitpos.enums.EstadoCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionCajaDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long sucursalId;
    private String sucursalNombre;
    private LocalDateTime fechaHoraApertura;
    private LocalDateTime fechaHoraCierre;
    private BigDecimal montoInicial;
    private BigDecimal montoFinal;
    private BigDecimal totalVentas;
    private EstadoSesion estado;
    private String observaciones;

    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
    private BigDecimal totalOtros;
    private BigDecimal totalPlataformas;   // ← agregar junto a los otros totales
    private BigDecimal montoEsperado;        // efectivo esperado al cierre
    private BigDecimal diferenciaCierre;     // montoCierre - montoEsperado
    private Long terminalId;
    private String terminalNombre;
}