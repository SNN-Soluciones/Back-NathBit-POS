// ReporteVentasTipoPagoRequest.java
package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
public class ReporteVentasTipoPagoRequest {

  @NotNull(message = "La sucursal es requerida")
  private Long sucursalId;

  @NotNull(message = "La fecha desde es requerida")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate fechaDesde;

  @NotNull(message = "La fecha hasta es requerida")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate fechaHasta;

  @Builder.Default
  private FormatoReporte formato = FormatoReporte.PDF;

  // Opcional: incluir anuladas
  @Builder.Default
  private boolean incluirAnuladas = false;

  // Opcional: filtrar por cajero específico
  private Long cajeroId;
}
