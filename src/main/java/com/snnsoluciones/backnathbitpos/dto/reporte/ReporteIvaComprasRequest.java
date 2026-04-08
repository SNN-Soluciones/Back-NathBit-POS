package com.snnsoluciones.backnathbitpos.dto.reporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parámetros de filtro para el reporte de IVA por tarifa en compras")
public class ReporteIvaComprasRequest {

    @NotNull(message = "El ID de sucursal es requerido")
    @Schema(description = "ID de la sucursal a consultar", example = "2", required = true)
    private Long sucursalId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de emisión desde (inclusive). Default: primer día del mes anterior",
            example = "2026-01-01")
    private LocalDate fechaEmisionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de emisión hasta (inclusive). Default: último día del mes anterior",
            example = "2026-01-31")
    private LocalDate fechaEmisionHasta;

    @Schema(description = "Estado interno de la factura. Default: ACEPTADA",
            example = "ACEPTADA",
            allowableValues = {"ACEPTADA", "RECHAZADA", "ACEPTADA_PARCIAL", "PENDIENTE_DECISION"})
    private String estadoInterno;

    @Schema(description = "Tipos de documento a incluir. " +
                          "Default: [FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_CREDITO]",
            example = "[\"FACTURA_ELECTRONICA\", \"NOTA_CREDITO\"]")
    private List<String> tiposDocumento;

    // ─────────────────────────────────────────────────
    //  Defaults
    // ─────────────────────────────────────────────────

    public void aplicarDefaults() {
        if (fechaEmisionDesde == null) {
            fechaEmisionDesde = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        }
        if (fechaEmisionHasta == null) {
            fechaEmisionHasta = LocalDate.now().minusMonths(1)
                .withDayOfMonth(LocalDate.now().minusMonths(1).lengthOfMonth());
        }
        if (estadoInterno == null || estadoInterno.isBlank()) {
            estadoInterno = "ACEPTADA";
        }
        if (tiposDocumento == null || tiposDocumento.isEmpty()) {
            tiposDocumento = List.of("FACTURA_ELECTRONICA", "TIQUETE_ELECTRONICO", "NOTA_CREDITO");
        }
    }

    /** Convierte fechaEmisionDesde a LocalDateTime inicio del día */
    public LocalDateTime getFechaDesdeDateTime() {
        return fechaEmisionDesde.atStartOfDay();
    }

    /** Convierte fechaEmisionHasta a LocalDateTime fin del día */
    public LocalDateTime getFechaHastaDateTime() {
        return fechaEmisionHasta.atTime(23, 59, 59);
    }
}