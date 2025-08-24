// src/main/java/com/snnsoluciones/backnathbitpos/dto/cliente/ExoneracionClienteDto.java
package com.snnsoluciones.backnathbitpos.dto.cliente;

import java.util.List;
import java.util.Set;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExoneracionClienteDto implements Serializable {
  // Identificación / autorización
  private String codigoAutorizacion;        // Ej: "AL-00098890-23"
  private Integer numeroAutorizacion;       // Ej: 98890 (opcional)

  // Catálogo MH
  private String tipoDocumento;             // Código MH: "04", "05", etc.
  private String numeroDocumento;           // Número de exoneración (si aplica)
  private String nombreInstitucion;         // Quien otorga

  // Vigencia
  private LocalDate fechaEmision;
  private LocalDate fechaVencimiento;
  private boolean vigente;                  // Calculado en el mapper

  // Parámetros de aplicación
  private BigDecimal porcentajeExoneracion; // 0 - 100
  private Boolean poseeCabys;               // true: restringida por lista de CAByS
  private Integer totalCabysAutorizados;    // conteo de CAByS asociados (no se envía la lista completa)

  // Opcionales informativos
  private String categoriaCompra;           // si lo manejas
  private BigDecimal montoMaximo;

  private List<String> codigosCabys;
}