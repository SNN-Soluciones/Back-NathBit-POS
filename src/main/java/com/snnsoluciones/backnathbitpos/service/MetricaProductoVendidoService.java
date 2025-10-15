package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.MetricaProductoVendidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricaProductoVendidoService {

    private final MetricaProductoVendidoRepository metricaRepository;

    /**
     * Actualiza métricas desde una Factura Electrónica
     */
    @Transactional
    public void actualizarDesdeFactura(Factura factura) {
        LocalDate fecha = LocalDate.parse(factura.getFechaEmision().substring(0, 10));
        
        for (FacturaDetalle detalle : factura.getDetalles()) {
            if (detalle.getProducto() != null) {
                actualizarMetrica(fecha, factura.getSucursal(), detalle.getProducto(), detalle.getCantidad());
            }
        }
    }

    /**
     * Actualiza métricas desde una Factura Interna
     */
    @Transactional
    public void actualizarDesdeFacturaInterna(FacturaInterna facturaInterna) {
        LocalDate fecha = facturaInterna.getFecha().toLocalDate();
        
        for (FacturaInternaDetalle detalle : facturaInterna.getDetalles()) {
            if (detalle.getProducto() != null) {
                actualizarMetrica(fecha, facturaInterna.getSucursal(), detalle.getProducto(), detalle.getCantidad());
            }
        }
    }

    /**
     * Método central: actualiza o crea la métrica
     */
    private void actualizarMetrica(LocalDate fecha, Sucursal sucursal, Producto producto, BigDecimal cantidad) {
        MetricaProductoVendido metrica = metricaRepository
            .findByFechaAndSucursalIdAndProductoId(fecha, sucursal.getId(), producto.getId())
            .orElseGet(() -> MetricaProductoVendido.builder()
                .fecha(fecha)
                .sucursal(sucursal)
                .producto(producto)
                .cantidadVendida(BigDecimal.ZERO)
                .build()
            );

        metrica.agregarCantidad(cantidad);
        metricaRepository.save(metrica);
        
        log.debug("✅ Métrica actualizada: {} unidades de {} el {}", 
            cantidad, producto.getNombre(), fecha);
    }
}