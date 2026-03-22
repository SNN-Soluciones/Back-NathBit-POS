package com.snnsoluciones.backnathbitpos.dto.reporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request para el reporte de IVA por tarifa en ventas.
 *
 * <p>Todos los filtros son opcionales excepto {@code sucursalId}. Los valores
 * por defecto cubren el caso de uso más frecuente: facturas del mes anterior
 * aceptadas entre el 1 de febrero y hoy.</p>
 *
 * <pre>
 * Filtros disponibles:
 *  - sucursalId          → obligatorio, viene del frontend
 *  - fechaEmisionDesde   → default: primer día del mes anterior
 *  - fechaEmisionHasta   → default: último día del mes anterior
 *  - tiposDocumento      → default: FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_CREDITO
 *  - estadoBitacora      → default: ACEPTADA
 *  - fechaAceptacionDesde → default: 2026-02-01
 *  - fechaAceptacionHasta → default: hoy (LocalDate.now())
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parámetros de filtro para el reporte de IVA por tarifa")
public class ReporteIvaVentasRequest {

    // ─────────────────────────────────────────────────
    //  CAMPO OBLIGATORIO
    // ─────────────────────────────────────────────────

    @NotNull(message = "El ID de sucursal es requerido")
    @Schema(description = "ID de la sucursal a consultar", example = "2", required = true)
    private Long sucursalId;

    // ─────────────────────────────────────────────────
    //  FILTRO: Rango de fecha de emisión del documento
    // ─────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "Fecha de emisión desde (inclusive). Default: primer día del mes anterior",
        example = "2026-01-01"
    )
    private LocalDate fechaEmisionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "Fecha de emisión hasta (inclusive). Default: último día del mes anterior",
        example = "2026-02-28"
    )
    private LocalDate fechaEmisionHasta;

    // ─────────────────────────────────────────────────
    //  FILTRO: Tipos de documento
    // ─────────────────────────────────────────────────

    @Schema(
        description = "Lista de tipos de documento a incluir. " +
                      "Default: [FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_CREDITO]",
        example = "[\"FACTURA_ELECTRONICA\", \"NOTA_CREDITO\"]"
    )
    private List<String> tiposDocumento;

    // ─────────────────────────────────────────────────
    //  FILTRO: Estado en bitácora de Hacienda
    // ─────────────────────────────────────────────────

    @Schema(
        description = "Estado del documento en la bitácora de Hacienda. " +
                      "Valores: ACEPTADA, RECHAZADA, PENDIENTE. Default: ACEPTADA",
        example = "ACEPTADA"
    )
    private String estadoBitacora;

    // ─────────────────────────────────────────────────
    //  FILTRO: Rango de fecha en que fue aceptada/procesada por Hacienda
    // ─────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "Fecha de aceptación/procesamiento desde (inclusive). Default: 2026-02-01",
        example = "2026-02-01"
    )
    private LocalDate fechaAceptacionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "Fecha de aceptación/procesamiento hasta (inclusive). Default: hoy",
        example = "2026-03-22"
    )
    private LocalDate fechaAceptacionHasta;

    // ─────────────────────────────────────────────────
    //  RESOLUCIÓN DE DEFAULTS  (llamar antes de usar)
    // ─────────────────────────────────────────────────

    /**
     * Aplica valores por defecto a todos los campos nulos.
     * Se invoca en el Service antes de construir la query.
     */
    public void aplicarDefaults() {

        // Mes anterior completo como rango de emisión
        LocalDate primerDiaMesAnterior = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate ultimoDiaMesAnterior = primerDiaMesAnterior.withDayOfMonth(
            primerDiaMesAnterior.lengthOfMonth()
        );

        if (fechaEmisionDesde == null) {
            fechaEmisionDesde = primerDiaMesAnterior;
        }
        if (fechaEmisionHasta == null) {
            fechaEmisionHasta = ultimoDiaMesAnterior;
        }

        if (tiposDocumento == null || tiposDocumento.isEmpty()) {
            tiposDocumento = List.of(
                "FACTURA_ELECTRONICA",
                "TIQUETE_ELECTRONICO",
                "NOTA_CREDITO"
            );
        }

        if (estadoBitacora == null || estadoBitacora.isBlank()) {
            estadoBitacora = "ACEPTADA";
        }

        if (fechaAceptacionDesde == null) {
            fechaAceptacionDesde = LocalDate.of(2026, 2, 1);
        }
        if (fechaAceptacionHasta == null) {
            fechaAceptacionHasta = LocalDate.now();
        }
    }
}