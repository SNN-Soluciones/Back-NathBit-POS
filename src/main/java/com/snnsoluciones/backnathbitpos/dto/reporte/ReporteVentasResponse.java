package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response con los datos del reporte y totales calculados
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasResponse {
    
    // Metadatos del reporte
    private String empresaNombre;
    private String empresaIdentificacion;
    private String sucursalNombre;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private LocalDateTime fechaGeneracion;
    private String generadoPor;
    
    // Líneas del reporte
    private List<ReporteVentasLineaDTO> lineas;
    
    // Resumen de cantidades
    private Integer totalFacturas;
    private Integer totalTiquetes;
    private Integer totalNotasCredito;
    private Integer totalDocumentos;
    
    // Totales generales
    private BigDecimal totalMercanciasGravadas;
    private BigDecimal totalMercanciasExentas;
    private BigDecimal totalMercanciasExoneradas;
    private BigDecimal totalServiciosGravados;
    private BigDecimal totalServiciosExentos;
    private BigDecimal totalServiciosExonerados;
    
    // Subtotales agrupados
    private BigDecimal subtotalGravado;
    private BigDecimal subtotalExento;
    private BigDecimal subtotalExonerado;
    
    // Totales finales
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuestos;
    private BigDecimal totalDescuentos;
    private BigDecimal totalExonerado;
    private BigDecimal totalOtrosCargos;
    private BigDecimal totalGeneral;
    
    // Archivo generado (para descarga)
    private byte[] archivoGenerado;
    private String nombreArchivo;
    private String tipoContenido; // MIME type
    
    // Método helper para calcular totales desde las líneas
    public void calcularTotales() {
        if (lineas == null || lineas.isEmpty()) {
            inicializarTotalesEnCero();
            return;
        }
        
        // Contadores
        totalFacturas = 0;
        totalTiquetes = 0;
        totalNotasCredito = 0;
        
        // Inicializar acumuladores
        totalMercanciasGravadas = BigDecimal.ZERO;
        totalMercanciasExentas = BigDecimal.ZERO;
        totalMercanciasExoneradas = BigDecimal.ZERO;
        totalServiciosGravados = BigDecimal.ZERO;
        totalServiciosExentos = BigDecimal.ZERO;
        totalServiciosExonerados = BigDecimal.ZERO;
        totalVentaNeta = BigDecimal.ZERO;
        totalImpuestos = BigDecimal.ZERO;
        totalDescuentos = BigDecimal.ZERO;
        totalExonerado = BigDecimal.ZERO;
        totalOtrosCargos = BigDecimal.ZERO;
        totalGeneral = BigDecimal.ZERO;
        
        // Sumar línea por línea
        for (ReporteVentasLineaDTO linea : lineas) {
            // Contar por tipo
            switch (linea.getTipoDocumento()) {
                case "FACTURA_ELECTRONICA":
                    totalFacturas++;
                    break;
                case "TIQUETE_ELECTRONICO":
                    totalTiquetes++;
                    break;
                case "NOTA_CREDITO":
                    totalNotasCredito++;
                    break;
            }
            
            // Acumular totales
            totalMercanciasGravadas = sumar(totalMercanciasGravadas, linea.getTotalMercanciasGravadas());
            totalMercanciasExentas = sumar(totalMercanciasExentas, linea.getTotalMercanciasExentas());
            totalMercanciasExoneradas = sumar(totalMercanciasExoneradas, linea.getTotalMercanciasExoneradas());
            totalServiciosGravados = sumar(totalServiciosGravados, linea.getTotalServiciosGravados());
            totalServiciosExentos = sumar(totalServiciosExentos, linea.getTotalServiciosExentos());
            totalServiciosExonerados = sumar(totalServiciosExonerados, linea.getTotalServiciosExonerados());
            totalVentaNeta = sumar(totalVentaNeta, linea.getTotalVentaNeta());
            totalImpuestos = sumar(totalImpuestos, linea.getTotalImpuesto());
            totalDescuentos = sumar(totalDescuentos, linea.getTotalDescuentos());
            totalExonerado = sumar(totalExonerado, linea.getMontoTotalExonerado());
            totalOtrosCargos = sumar(totalOtrosCargos, linea.getTotalOtrosCargos());
            totalGeneral = sumar(totalGeneral, linea.getTotalComprobante());
        }
        
        // Calcular subtotales
        subtotalGravado = sumar(totalMercanciasGravadas, totalServiciosGravados);
        subtotalExento = sumar(totalMercanciasExentas, totalServiciosExentos);
        subtotalExonerado = sumar(totalMercanciasExoneradas, totalServiciosExonerados);
        
        totalDocumentos = totalFacturas + totalTiquetes + totalNotasCredito;
    }
    
    private BigDecimal sumar(BigDecimal acumulador, BigDecimal valor) {
        if (valor != null) {
            return acumulador.add(valor);
        }
        return acumulador;
    }
    
    private void inicializarTotalesEnCero() {
        totalFacturas = 0;
        totalTiquetes = 0;
        totalNotasCredito = 0;
        totalDocumentos = 0;
        totalMercanciasGravadas = BigDecimal.ZERO;
        totalMercanciasExentas = BigDecimal.ZERO;
        totalMercanciasExoneradas = BigDecimal.ZERO;
        totalServiciosGravados = BigDecimal.ZERO;
        totalServiciosExentos = BigDecimal.ZERO;
        totalServiciosExonerados = BigDecimal.ZERO;
        subtotalGravado = BigDecimal.ZERO;
        subtotalExento = BigDecimal.ZERO;
        subtotalExonerado = BigDecimal.ZERO;
        totalVentaNeta = BigDecimal.ZERO;
        totalImpuestos = BigDecimal.ZERO;
        totalDescuentos = BigDecimal.ZERO;
        totalExonerado = BigDecimal.ZERO;
        totalOtrosCargos = BigDecimal.ZERO;
        totalGeneral = BigDecimal.ZERO;
    }
}