package com.snnsoluciones.backnathbitpos.dto.factura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * Response detallado de validación de totales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionTotalesResponse {
    
    private boolean esValido;
    private String mensaje;
    
    // Totales calculados
    private BigDecimal subtotalCalculado;
    private BigDecimal totalDescuentosCalculado;
    private BigDecimal totalOtrosCargosCalculado;
    private BigDecimal totalImpuestosCalculado;
    private BigDecimal totalCalculado;
    
    // Desglose detallado
    @Builder.Default
    private List<DesglosePorLinea> desglosePorLinea = new ArrayList<>();
    
    // Warnings o sugerencias
    @Builder.Default
    private List<String> advertencias = new ArrayList<>();
}
