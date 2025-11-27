// src/main/java/com/snnsoluciones/backnathbitpos/dto/ResumenPagosEmpresaDTO.java
package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenPagosEmpresaDTO {
    
    private Long totalSucursales;
    private Long sucursalesActivas;
    private Long sucursalesSuspendidas;
    private BigDecimal montoMensualTotal;
}