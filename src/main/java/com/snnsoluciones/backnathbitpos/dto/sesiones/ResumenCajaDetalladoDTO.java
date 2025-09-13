package com.snnsoluciones.backnathbitpos.dto.sesiones;

import lombok.Data;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResumenCajaDetalladoDTO {
    // Identificación
    private Long sesionId;
    private String terminal;
    private String cajero;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    
    // Montos
    private BigDecimal montoInicial;
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasTarjeta;
    private BigDecimal ventasTransferencia;
    private BigDecimal ventasOtros;
    
    // Movimientos
    private BigDecimal entradasAdicionales;
    private BigDecimal vales;
    private BigDecimal depositos;
    
    // Totales
    private BigDecimal montoEsperado;
    private BigDecimal montoCierre;
    private BigDecimal diferencia;
    
    // Detalle
    private List<MovimientoCaja> movimientos;
    
    // Contadores
    private Integer cantidadFacturas;
    private Integer cantidadTiquetes;
    private Integer cantidadNotasCredito;
    
    // Método helper para calcular diferencia
    public BigDecimal getDiferencia() {
        if (montoCierre != null && montoEsperado != null) {
            return montoCierre.subtract(montoEsperado);
        }
        return BigDecimal.ZERO;
    }
}