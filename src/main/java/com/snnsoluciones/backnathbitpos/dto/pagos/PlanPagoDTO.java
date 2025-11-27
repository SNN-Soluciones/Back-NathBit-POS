// src/main/java/com/snnsoluciones/backnathbitpos/dto/PlanPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPagoDTO {
    
    private Long id;
    private Long sucursalId;
    private String nombreSucursal;
    private Long empresaId;
    private String nombreEmpresa;
    private BigDecimal cuotaMensual;
    private LocalDate fechaInicio;
    private Integer diaVencimiento;
    private String estado;
    private Integer diasGracia;
    private LocalDate fechaUltimoPago;
    private LocalDate fechaProximoVencimiento;
    private String notas;
}