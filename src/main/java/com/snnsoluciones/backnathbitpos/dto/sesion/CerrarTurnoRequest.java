package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerrarTurnoRequest {

    @NotNull(message = "El monto contado es requerido")
    private BigDecimal montoContado;

    @NotNull(message = "Monto retirado es requerido")
    @Min(value = 0, message = "Monto retirado debe ser mayor o igual a 0")
    private BigDecimal montoRetirado;

    @NotNull(message = "Fondo de caja es requerido")
    @Min(value = 0, message = "Fondo de caja debe ser mayor o igual a 0")
    private BigDecimal fondoCaja;

    // Totales declarados por el cajero (referenciales)
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
    private BigDecimal totalSinpe; // opcional, se compara pero no bloquea el cierre

    private String observaciones;

    // Datafonos declarados — se comparan contra facturas del turno
    @Valid
    private List<DatafonoDTO> datafonos;

    @NotNull(message = "Las denominaciones son requeridas")
    private List<CerrarSesionRequest.DenominacionDTO> denominaciones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatafonoDTO {
        @NotNull private String datafono;
        @NotNull private BigDecimal monto;
    }
}