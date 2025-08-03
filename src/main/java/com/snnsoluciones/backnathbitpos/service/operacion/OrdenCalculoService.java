// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/service/operacion/OrdenCalculoService.java

package com.snnsoluciones.backnathbitpos.service.operacion;

import com.snnsoluciones.backnathbitpos.entity.operacion.Orden;
import com.snnsoluciones.backnathbitpos.entity.operacion.OrdenDetalle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenCalculoService {
    
    /**
     * Calcula los totales de una línea de detalle de orden
     */
    public void calcularTotalesDetalle(OrdenDetalle detalle) {
        if (detalle.getCantidad() == null || detalle.getPrecioUnitario() == null) {
            log.warn("No se pueden calcular totales sin cantidad o precio unitario");
            return;
        }
        
        // Subtotal = cantidad * precio unitario
        BigDecimal subtotal = detalle.getCantidad()
            .multiply(detalle.getPrecioUnitario())
            .setScale(2, RoundingMode.HALF_UP);
        detalle.setSubtotal(subtotal);
        
        // Calcular descuento
        BigDecimal montoDescuento = BigDecimal.ZERO;
        if (detalle.getPorcentajeDescuento() != null && 
            detalle.getPorcentajeDescuento().compareTo(BigDecimal.ZERO) > 0) {
            montoDescuento = subtotal
                .multiply(detalle.getPorcentajeDescuento())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            detalle.setMontoDescuento(montoDescuento);
        }
        
        BigDecimal subtotalConDescuento = subtotal.subtract(montoDescuento);
        
        // Calcular IVA
        BigDecimal montoIva = BigDecimal.ZERO;
        if (detalle.getTarifaIva() != null && 
            detalle.getTarifaIva().compareTo(BigDecimal.ZERO) > 0) {
            montoIva = subtotalConDescuento
                .multiply(detalle.getTarifaIva())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            detalle.setMontoIva(montoIva);
        }
        
        // Calcular total
        BigDecimal otrosImpuestos = detalle.getMontoOtrosImpuestos() != null ? 
            detalle.getMontoOtrosImpuestos() : BigDecimal.ZERO;
            
        BigDecimal total = subtotalConDescuento
            .add(montoIva)
            .add(otrosImpuestos);
        detalle.setTotal(total);
        
        log.debug("Totales calculados para detalle: subtotal={}, descuento={}, iva={}, total={}", 
            subtotal, montoDescuento, montoIva, total);
    }
    
    /**
     * Calcula los totales de una orden completa
     */
    public void calcularTotalesOrden(Orden orden) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        BigDecimal totalImpuestos = BigDecimal.ZERO;
        
        // Sumar todos los detalles
        for (OrdenDetalle detalle : orden.getDetalles()) {
            // Asegurar que cada detalle esté calculado
            calcularTotalesDetalle(detalle);
            
            subtotal = subtotal.add(detalle.getSubtotal());
            totalDescuentos = totalDescuentos.add(detalle.getMontoDescuento());
            totalImpuestos = totalImpuestos
                .add(detalle.getMontoIva())
                .add(detalle.getMontoOtrosImpuestos() != null ? 
                    detalle.getMontoOtrosImpuestos() : BigDecimal.ZERO);
        }
        
        // Actualizar totales de la orden
        orden.setSubtotal(subtotal);
        orden.setTotalDescuentos(totalDescuentos);
        orden.setTotalImpuestos(totalImpuestos);
        
        // Total = subtotal - descuentos + impuestos
        BigDecimal total = subtotal
            .subtract(totalDescuentos)
            .add(totalImpuestos);
        orden.setTotal(total);
        
        log.info("Totales calculados para orden {}: subtotal={}, descuentos={}, impuestos={}, total={}", 
            orden.getNumeroOrden(), subtotal, totalDescuentos, totalImpuestos, total);
    }
    
    /**
     * Valida si un detalle puede ser agregado a una orden
     */
    public boolean validarDetalle(OrdenDetalle detalle) {
        if (detalle.getProducto() == null) {
            log.error("El detalle debe tener un producto");
            return false;
        }
        
        if (detalle.getCantidad() == null || 
            detalle.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("La cantidad debe ser mayor a cero");
            return false;
        }
        
        if (detalle.getPrecioUnitario() == null || 
            detalle.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            log.error("El precio unitario no puede ser negativo");
            return false;
        }
        
        return true;
    }
    
    /**
     * Aplica un descuento global a toda la orden
     */
    public void aplicarDescuentoGlobal(Orden orden, BigDecimal porcentajeDescuento) {
        if (porcentajeDescuento == null || 
            porcentajeDescuento.compareTo(BigDecimal.ZERO) < 0 ||
            porcentajeDescuento.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        
        for (OrdenDetalle detalle : orden.getDetalles()) {
            detalle.setPorcentajeDescuento(porcentajeDescuento);
            calcularTotalesDetalle(detalle);
        }
        
        calcularTotalesOrden(orden);
    }
}