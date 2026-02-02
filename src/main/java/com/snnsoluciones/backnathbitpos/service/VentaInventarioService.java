package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.ProductoCompuestoOpcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para descontar inventario en ventas (Facturas MH y Facturas Internas)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaInventarioService {

    private final ProductoInventarioService productoInventarioService;
    private final ProductoCompuestoOpcionRepository opcionRepository;

    /**
     * Descuenta inventario de una factura MH completa
     */
    @Transactional
    public void descontarInventarioFactura(Factura factura) {
        log.info("Descontando inventario de factura: {}", factura.getConsecutivo());

        Sucursal sucursal = factura.getSucursal();

        // Validar si sucursal maneja inventario
        if (!sucursal.getManejaInventario()) {
            log.debug("Sucursal {} no maneja inventario, skip", sucursal.getNombre());
            return;
        }

        // Procesar cada detalle
        for (FacturaDetalle detalle : factura.getDetalles()) {
            descontarDetalleFactura(detalle, sucursal, factura.getConsecutivo());
        }

        log.info("Inventario descontado exitosamente para factura {}", factura.getConsecutivo());
    }

    /**
     * Descuenta inventario de una factura interna completa
     */
    @Transactional
    public void descontarInventarioFacturaInterna(FacturaInterna factura) {
        log.info("Descontando inventario de factura interna: {}", factura.getNumero());

        Sucursal sucursal = factura.getSucursal();

        // Validar si sucursal maneja inventario
        if (!sucursal.getManejaInventario()) {
            log.debug("Sucursal {} no maneja inventario, skip", sucursal.getNombre());
            return;
        }

        // Procesar cada detalle
        for (FacturaInternaDetalle detalle : factura.getDetalles()) {
            descontarDetalleFacturaInterna(detalle, sucursal, factura.getNumero());
        }

        log.info("Inventario descontado exitosamente para factura interna {}", factura.getNumero());
    }

    /**
     * Descuenta inventario de un detalle de factura MH
     */
    private void descontarDetalleFactura(FacturaDetalle detalle, Sucursal sucursal, String consecutivo) {
        Producto producto = detalle.getProducto();

        // CASO 1: Producto COMPUESTO
        if (producto.getTipo() == com.snnsoluciones.backnathbitpos.enums.TipoProducto.COMPUESTO) {
            descontarOpcionesCompuesto(
                detalle.getOpcionesSeleccionadas(),
                detalle.getCantidad(),
                sucursal,
                "Factura " + consecutivo
            );
            return;
        }

        // CASO 2: Producto SIMPLE (VENTA, MIXTO, MATERIA_PRIMA)
        if (producto.requiereControlInventario()) {
            descontarProductoSimple(
                producto,
                detalle.getCantidad(),
                sucursal,
                "Factura " + consecutivo
            );
        }
    }

    /**
     * Descuenta inventario de un detalle de factura interna
     */
    private void descontarDetalleFacturaInterna(FacturaInternaDetalle detalle, Sucursal sucursal, String numero) {
        Producto producto = detalle.getProducto();

        // CASO 1: Producto COMPUESTO
        if (producto.getTipo() == com.snnsoluciones.backnathbitpos.enums.TipoProducto.COMPUESTO) {
            descontarOpcionesCompuesto(
                detalle.getOpcionesSeleccionadas(),
                detalle.getCantidad(),
                sucursal,
                "Factura Interna " + numero
            );
            return;
        }

        // CASO 2: Producto SIMPLE
        if (producto.requiereControlInventario()) {
            descontarProductoSimple(
                producto,
                detalle.getCantidad(),
                sucursal,
                "Factura Interna " + numero
            );
        }
    }

    /**
     * Descuenta inventario de opciones de un producto compuesto
     */
    private void descontarOpcionesCompuesto(
            List<Long> opcionesSeleccionadas,
            BigDecimal cantidadVendida,
            Sucursal sucursal,
            String motivo) {

        // Si no vienen opciones → Skip (backward compatibility)
        if (opcionesSeleccionadas == null || opcionesSeleccionadas.isEmpty()) {
            log.debug("Producto compuesto sin opciones seleccionadas, skip");
            return;
        }

        log.debug("Descontando {} opciones de producto compuesto", opcionesSeleccionadas.size());

        // Descontar cada opción
        for (Long opcionId : opcionesSeleccionadas) {
            try {
                // Cargar opción
                ProductoCompuestoOpcion opcion = opcionRepository
                    .findById(opcionId)
                    .orElse(null);

                if (opcion == null) {
                    log.warn("Opción {} no encontrada, skip", opcionId);
                    continue;
                }

                Producto productoOpcion = opcion.getProducto();

                // Solo si la opción tiene producto Y requiere inventario
                if (productoOpcion != null && productoOpcion.requiereControlInventario()) {
                    
                    productoInventarioService.reducirInventario(
                        productoOpcion.getId(),
                        sucursal.getId(),
                        cantidadVendida,
                        motivo + " - Opción: " + opcion.getNombreEfectivo()
                    );

                    log.debug("Descontado inventario de opción: {} (Producto: {})",
                        opcion.getNombreEfectivo(), productoOpcion.getNombre());
                }

            } catch (BusinessException e) {
                // Si NO permite negativos y no hay stock, ya lanzó error
                // Si SÍ permite negativos, se guardó en negativo
                log.warn("Error descontando opción {}: {}", opcionId, e.getMessage());
                // NO lanzamos el error para no romper la venta
                // Solo logueamos el warning
            }
        }
    }

    /**
     * Descuenta inventario de un producto simple (VENTA, MIXTO, MATERIA_PRIMA)
     */
    private void descontarProductoSimple(
            Producto producto,
            BigDecimal cantidad,
            Sucursal sucursal,
            String motivo) {

        try {
            productoInventarioService.reducirInventario(
                producto.getId(),
                sucursal.getId(),
                cantidad,
                motivo
            );

            log.debug("Descontado inventario de producto: {} (cantidad: {})",
                producto.getNombre(), cantidad);

        } catch (BusinessException e) {
            // Si NO permite negativos y no hay stock → Error ya lanzado
            // Si SÍ permite negativos → Guardado en negativo
            log.warn("Error descontando producto {}: {}", producto.getId(), e.getMessage());
            // NO lanzamos error para no romper la venta
        }
    }
}