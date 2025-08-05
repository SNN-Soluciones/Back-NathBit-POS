package com.snnsoluciones.backnathbitpos.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DenominacionesDTO.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalle de denominaciones de billetes y monedas")
public class DenominacionesDTO {
    
    @Schema(description = "Billetes por denominación y cantidad")
    private Map<Integer, Integer> billetes; // {50000: 2, 20000: 5, 10000: 3}
    
    @Schema(description = "Monedas por denominación y cantidad")
    private Map<Integer, Integer> monedas; // {500: 10, 100: 20, 50: 5}
    
    /**
     * Calcula el total en colones
     */
    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (billetes != null) {
            for (Map.Entry<Integer, Integer> entry : billetes.entrySet()) {
                total = total.add(
                    BigDecimal.valueOf(entry.getKey())
                        .multiply(BigDecimal.valueOf(entry.getValue()))
                );
            }
        }
        
        if (monedas != null) {
            for (Map.Entry<Integer, Integer> entry : monedas.entrySet()) {
                total = total.add(
                    BigDecimal.valueOf(entry.getKey())
                        .multiply(BigDecimal.valueOf(entry.getValue()))
                );
            }
        }
        
        return total;
    }
}