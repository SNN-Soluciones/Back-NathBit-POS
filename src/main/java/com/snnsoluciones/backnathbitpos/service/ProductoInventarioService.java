package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.AjusteInventarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Compra;
import com.snnsoluciones.backnathbitpos.entity.CompraDetalle;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoInventario;
import com.snnsoluciones.backnathbitpos.entity.ProductoMovimiento;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimiento;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoInventarioRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoMovimientoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductoInventarioService {
    
    private final ProductoInventarioRepository inventarioRepository;
    private final ProductoMovimientoRepository productoMovimientoRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    
    // Obtener inventario de un producto en una sucursal
    public ProductoInventario obtenerInventario(Long productoId, Long sucursalId) {
        return inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado"));
    }
    
    // Crear inventario inicial para un producto
    public ProductoInventario crearInventario(Long productoId, Long sucursalId, BigDecimal cantidadMinima) {
        // Verificar que no exista
        if (inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId).isPresent()) {
            throw new IllegalStateException("El inventario ya existe para este producto y sucursal");
        }
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        
        ProductoInventario inventario = new ProductoInventario();
        inventario.setProducto(producto);
        inventario.setSucursal(sucursal);
        inventario.setCantidadMinima(cantidadMinima);
        inventario.setCantidadActual(BigDecimal.ZERO);
        
        return inventarioRepository.save(inventario);
    }
    
    // Ajustar inventario (entrada/salida manual)
    public ProductoInventario ajustarInventario(AjusteInventarioDTO ajuste) {
        ProductoInventario inventario = obtenerInventario(ajuste.getProductoId(), ajuste.getSucursalId());
        
        if (ajuste.getTipoAjuste().equals("ABSOLUTO")) {
            inventario.actualizarCantidad(ajuste.getCantidad());
        } else {
            inventario.ajustarCantidad(ajuste.getCantidad());
        }
        
        return inventarioRepository.save(inventario);
    }
    
    // Descontar inventario (para ventas)
    public void descontarInventario(Long productoId, Long sucursalId, BigDecimal cantidad) {
        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);
        
        if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
            throw new IllegalStateException("Stock insuficiente. Disponible: " + inventario.getCantidadActual());
        }
        
        inventario.ajustarCantidad(cantidad.negate());
        inventarioRepository.save(inventario);
    }
    
    // Obtener productos bajo mínimo
    public List<ProductoInventario> obtenerBajoMinimos(Long sucursalId) {
        return inventarioRepository.findBajoMinimosBySucursal(sucursalId);
    }
    
    // Verificar disponibilidad
    public boolean verificarDisponibilidad(Long productoId, Long sucursalId, BigDecimal cantidadRequerida) {
        try {
            ProductoInventario inventario = obtenerInventario(productoId, sucursalId);
            return inventario.getCantidadActual().compareTo(cantidadRequerida) >= 0;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    // Obtener todos los inventarios de una sucursal
    public List<ProductoInventario> obtenerInventarioPorSucursal(Long sucursalId) {
        return inventarioRepository.findBySucursalIdAndEstadoTrue(sucursalId);
    }

    // Obtener inventario de un producto en todas las sucursales
    public List<ProductoInventario> obtenerInventarioPorProducto(Long productoId) {
        return inventarioRepository.findByProductoIdAndEstadoTrue(productoId);
    }

    /**
     * Procesa una compra y actualiza el inventario de la sucursal
     * @param compra La compra a procesar
     */
    @Transactional
    public void procesarCompra(Compra compra) {
        log.info("Procesando compra {} para actualizar inventario", compra.getId());

        // Validar que la compra esté en estado válido
        if (compra.getEstado() != EstadoCompra.ACEPTADA &&
            compra.getEstado() != EstadoCompra.ENVIADA) {
            log.warn("Compra {} no está en estado válido para procesar inventario", compra.getId());
            return;
        }

        // Procesar cada línea de detalle
        for (CompraDetalle detalle : compra.getDetalles()) {
            // Solo procesar si tiene producto asociado
            if (detalle.getProducto() == null) {
                log.debug("Línea {} sin producto asociado, saltando", detalle.getNumeroLinea());
                continue;
            }

            Producto producto = detalle.getProducto();

            // Solo procesar productos que manejan inventario
            if (Boolean.TRUE.equals(producto.getEsServicio()) ||
                !Boolean.TRUE.equals(producto.getRequiereInventario())) {
                log.debug("Producto {} no maneja inventario", producto.getId());
                continue;
            }

            // Buscar o crear inventario para el producto en la sucursal
            ProductoInventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), compra.getSucursal().getId())
                .orElseGet(() -> {
                    // Crear nuevo registro de inventario
                    ProductoInventario nuevoInventario = new ProductoInventario();
                    nuevoInventario.setProducto(producto);
                    nuevoInventario.setSucursal(compra.getSucursal());
                    nuevoInventario.setCantidadActual(BigDecimal.ZERO);
                    nuevoInventario.setCantidadMinima(BigDecimal.ZERO);
                    nuevoInventario.setUltimaActualizacion(LocalDateTime.now());
                    return nuevoInventario;
                });

            // Calcular cantidad a agregar considerando factor de conversión
            BigDecimal cantidadAgregar = detalle.getCantidad();

            if (detalle.getFactorConversion() != null &&
                detalle.getFactorConversion().compareTo(BigDecimal.ZERO) > 0) {
                // Si hay factor de conversión, multiplicar
                // Ej: Si compro 5 cajas y cada caja tiene 12 unidades = 60 unidades
                cantidadAgregar = cantidadAgregar.multiply(detalle.getFactorConversion());
            }

            // Actualizar cantidad disponible
            BigDecimal nuevaCantidad = inventario.getCantidadActual().add(cantidadAgregar);
            inventario.setCantidadActual(nuevaCantidad);
            inventario.setUltimaActualizacion(LocalDateTime.now());

            // Guardar inventario
            inventarioRepository.save(inventario);

            // Registrar movimiento
            ProductoMovimiento movimiento = new ProductoMovimiento();
            movimiento.setProducto(producto);
            movimiento.setSucursal(compra.getSucursal());
            movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA_COMPRA);
            movimiento.setCantidad(cantidadAgregar);
            movimiento.setPrecioUnitario(detalle.getPrecioUnitario());
            movimiento.setCostoTotal(detalle.getMontoTotalLinea());
            movimiento.setDocumentoReferencia("COMPRA-" + compra.getId());
            movimiento.setObservaciones("Compra #" + compra.getNumeroDocumento() +
                " - Proveedor: " + compra.getProveedor().getNombreComercial());
            movimiento.setUsuario(compra.getUsuario());
            movimiento.setFechaMovimiento(LocalDateTime.now());
            movimiento.setSaldoAnterior(inventario.getCantidadActual().subtract(cantidadAgregar));
            movimiento.setSaldoNuevo(nuevaCantidad);

            productoMovimientoRepository.save(movimiento);

            log.info("Inventario actualizado - Producto: {}, Sucursal: {}, Cantidad agregada: {}, Nueva cantidad: {}",
                producto.getNombre(),
                compra.getSucursal().getNombre(),
                cantidadAgregar,
                nuevaCantidad);
        }

        log.info("Compra {} procesada exitosamente", compra.getId());
    }

    /**
     * Reversa el inventario de una compra (en caso de anulación)
     * @param compra La compra a reversar
     */
    @Transactional
    public void reversarCompra(Compra compra) {
        log.info("Reversando compra {} del inventario", compra.getId());

        for (CompraDetalle detalle : compra.getDetalles()) {
            if (detalle.getProducto() == null ||
                Boolean.TRUE.equals(detalle.getProducto().getEsServicio()) ||
                !Boolean.TRUE.equals(detalle.getProducto().getRequiereInventario())) {
                continue;
            }

            ProductoInventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(
                    detalle.getProducto().getId(),
                    compra.getSucursal().getId()
                )
                .orElse(null);

            if (inventario == null) {
                log.warn("No se encontró inventario para producto {} en sucursal {}",
                    detalle.getProducto().getId(),
                    compra.getSucursal().getId());
                continue;
            }

            // Calcular cantidad a restar
            BigDecimal cantidadRestar = detalle.getCantidad();
            if (detalle.getFactorConversion() != null &&
                detalle.getFactorConversion().compareTo(BigDecimal.ZERO) > 0) {
                cantidadRestar = cantidadRestar.multiply(detalle.getFactorConversion());
            }

            // Validar que haya suficiente inventario
            if (inventario.getCantidadActual().compareTo(cantidadRestar) < 0) {
                throw new BadRequestException(
                    "No hay suficiente inventario para reversar. Producto: " +
                        detalle.getProducto().getNombre() +
                        ", Disponible: " + inventario.getCantidadActual() +
                        ", Intentando reversar: " + cantidadRestar
                );
            }

            // Actualizar inventario
            BigDecimal nuevaCantidad = inventario.getCantidadActual().subtract(cantidadRestar);
            inventario.setCantidadActual(nuevaCantidad);
            inventario.setUltimaActualizacion(LocalDateTime.now());

            inventarioRepository.save(inventario);

            // Registrar movimiento de reversa
            ProductoMovimiento movimiento = new ProductoMovimiento();
            movimiento.setProducto(detalle.getProducto());
            movimiento.setSucursal(compra.getSucursal());
            movimiento.setTipoMovimiento(TipoMovimiento.SALIDA_ANULACION);
            movimiento.setCantidad(cantidadRestar.negate()); // Negativo para salida
            movimiento.setPrecioUnitario(detalle.getPrecioUnitario());
            movimiento.setCostoTotal(detalle.getMontoTotalLinea().negate());
            movimiento.setDocumentoReferencia("ANULA-COMPRA-" + compra.getId());
            movimiento.setObservaciones("Anulación de compra #" + compra.getNumeroDocumento());
            movimiento.setUsuario(compra.getUsuario());
            movimiento.setFechaMovimiento(LocalDateTime.now());
            movimiento.setSaldoAnterior(inventario.getCantidadActual().add(cantidadRestar));
            movimiento.setSaldoNuevo(nuevaCantidad);

            productoMovimientoRepository.save(movimiento);
        }

        log.info("Compra {} reversada exitosamente", compra.getId());
    }
}