// CerrarSesionRequest.java
package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CerrarSesionRequest {
    @NotNull
    private BigDecimal montoCierre;

    @NotNull(message = "Monto retirado es requerido")
    @Min(value = 0, message = "Monto retirado debe ser mayor o igual a 0")
    private BigDecimal montoRetirado;

    @NotNull(message = "Fondo de caja es requerido")
    @Min(value = 0, message = "Fondo de caja debe ser mayor o igual a 0")
    private BigDecimal fondoCaja;

    @Size(max = 500)
    private String observaciones;

    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
    private BigDecimal totalSinpe;

    @Valid
    private List<DatafonoDTO> datafonos;

    @NotNull
    private List<DenominacionDTO> denominaciones;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DenominacionDTO {
        @NotNull private BigDecimal valor;     // ej: 1000, 2000, 5000, etc
        @NotNull private Integer cantidad;     // ej: 3 billetes de 2000
    }
}