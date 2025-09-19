package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.AjusteInventarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Compra;
import com.snnsoluciones.backnathbitpos.entity.CompraDetalle;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoInventario;
import com.snnsoluciones.backnathbitpos.entity.ProductoMovimiento;
import com.snnsoluciones.backnathbitpos.entity.ProductoReceta;
import com.snnsoluciones.backnathbitpos.entity.RecetaIngrediente;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimiento;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoInventarioRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoMovimientoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRecetaRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.descriptor.web.ContextService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UsuarioService usuarioService;
    private final ProductoRecetaRepository recetaRepository;

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

        if (producto.getSucursal() != null && !producto.getSucursal().getId().equals(sucursalId)) {
            throw new BusinessException(
                "Este producto es local de la sucursal " + producto.getSucursal().getNombre() +
                    " y no puede tener inventario en otras sucursales"
            );
        }
        
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
            if (Boolean.TRUE.equals(producto.getAplicaServicio()) ||
                !producto.requiereControlInventario()) {
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
                Boolean.TRUE.equals(detalle.getProducto().getAplicaServicio()) ||
                !detalle.getProducto().requiereControlInventario()) {
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

    /**
     * Reducir inventario de un producto
     *
     * @param productoId ID del producto
     * @param sucursalId ID de la sucursal
     * @param cantidad   Cantidad a reducir
     * @param motivo     Motivo de la reducción (venta, merma, etc)
     */
    @Transactional
    public void reducirInventario(Long productoId, Long sucursalId, BigDecimal cantidad, String motivo) {
        log.info("Reduciendo inventario - Producto: {}, Sucursal: {}, Cantidad: {}", productoId, sucursalId, cantidad);

        // Validar cantidad
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La cantidad a reducir debe ser mayor a cero");
        }

        // Obtener inventario actual
        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);

        // Verificar stock disponible
        if (inventario.getCantidadActual().compareTo(cantidad) < 0) {
            throw new BusinessException(
                String.format("Stock insuficiente. Disponible: %s, Solicitado: %s",
                    inventario.getCantidadActual(), cantidad)
            );
        }

        // Reducir cantidad
        BigDecimal nuevaCantidad = inventario.getCantidadActual().subtract(cantidad);
        inventario.setCantidadActual(nuevaCantidad);

        // Guardar inventario actualizado
        inventario = inventarioRepository.save(inventario);

        // Registrar movimiento
        ProductoMovimiento movimiento = new ProductoMovimiento();
        movimiento.setProducto(inventario.getProducto());
        movimiento.setSucursal(inventario.getSucursal());
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
        movimiento.setCantidad(cantidad.negate()); // Negativo para salida
        movimiento.setSaldoAnterior(inventario.getCantidadActual().add(cantidad));
        movimiento.setSaldoNuevo(inventario.getCantidadActual());
        movimiento.setObservaciones(motivo != null ? motivo : "Reducción de inventario");
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setUsuario(obtenerUsuarioActual());

        productoMovimientoRepository.save(movimiento);

        log.info("Inventario reducido exitosamente. Nueva cantidad: {}", nuevaCantidad);

    }

    /**
     * Bloquear cantidad de inventario temporalmente
     */
    @Transactional
    public void bloquearInventario(Long productoId, Long sucursalId, BigDecimal cantidad) {
        log.info("Bloqueando inventario - Producto: {}, Sucursal: {}, Cantidad: {}",
            productoId, sucursalId, cantidad);

        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);

        // Verificar disponibilidad real
        BigDecimal disponibleReal = inventario.getCantidadActual()
            .subtract(inventario.getCantidadBloqueada());

        if (disponibleReal.compareTo(cantidad) < 0) {
            throw new BusinessException(
                String.format("Stock insuficiente. Disponible: %s, Solicitado: %s",
                    disponibleReal, cantidad)
            );
        }

        // Actualizar cantidad bloqueada
        inventario.setCantidadBloqueada(
            inventario.getCantidadBloqueada().add(cantidad)
        );
        inventario.setUltimaActualizacionBloqueada(LocalDateTime.now());

        inventarioRepository.save(inventario);
    }

    /**
     * Liberar inventario bloqueado
     */
    @Transactional
    public void liberarInventario(Long productoId, Long sucursalId, BigDecimal cantidad) {
        log.info("Liberando inventario - Producto: {}, Sucursal: {}, Cantidad: {}",
            productoId, sucursalId, cantidad);

        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);

        // Actualizar cantidad bloqueada
        BigDecimal nuevoBloqueado = inventario.getCantidadBloqueada().subtract(cantidad);
        if (nuevoBloqueado.compareTo(BigDecimal.ZERO) < 0) {
            nuevoBloqueado = BigDecimal.ZERO;
        }

        inventario.setCantidadBloqueada(nuevoBloqueado);
        inventario.setUltimaActualizacionBloqueada(LocalDateTime.now());

        inventarioRepository.save(inventario);
    }

    /**
     * Confirmar inventario bloqueado (convertir a consumido)
     */
    @Transactional
    public void confirmarBloqueo(Long productoId, Long sucursalId, BigDecimal cantidad, String motivo) {
        log.info("Confirmando bloqueo - Producto: {}, Sucursal: {}, Cantidad: {}",
            productoId, sucursalId, cantidad);

        ProductoInventario inventario = obtenerInventario(productoId, sucursalId);

        // Validar que hay suficiente cantidad bloqueada
        if (inventario.getCantidadBloqueada().compareTo(cantidad) < 0) {
            throw new BusinessException(
                String.format("Cantidad bloqueada insuficiente. Bloqueado: %s, Solicitado: %s",
                    inventario.getCantidadBloqueada(), cantidad)
            );
        }

        // Reducir de bloqueado
        BigDecimal nuevoBloqueado = inventario.getCantidadBloqueada().subtract(cantidad);
        inventario.setCantidadBloqueada(nuevoBloqueado);
        inventario.setUltimaActualizacionBloqueada(LocalDateTime.now());

        // Reducir de disponible
        BigDecimal nuevaDisponible = inventario.getCantidadActual().subtract(cantidad);
        inventario.setCantidadActual(nuevaDisponible);

        // Guardar cambios
        inventario = inventarioRepository.save(inventario);

        // Registrar movimiento
        ProductoMovimiento movimiento = new ProductoMovimiento();
        movimiento.setProducto(inventario.getProducto());
        movimiento.setSucursal(inventario.getSucursal());
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
        movimiento.setCantidad(cantidad.negate()); // Negativo para salida
        movimiento.setSaldoAnterior(inventario.getCantidadActual().add(cantidad));
        movimiento.setSaldoNuevo(inventario.getCantidadActual());
        movimiento.setObservaciones(motivo != null ? motivo : "Confirmación de inventario bloqueado");
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setUsuario(obtenerUsuarioActual());

        productoMovimientoRepository.save(movimiento);

        log.info("Bloqueo confirmado. Nueva cantidad disponible: {}, Bloqueada: {}",
            nuevaDisponible, nuevoBloqueado);
    }

    /**
     * Obtiene el usuario autenticado del contexto de seguridad
     */
    private Usuario obtenerUsuarioActual() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new BusinessException("No hay usuario autenticado");
        }

        String email = auth.getName(); // El email es el principal en tu sistema

        return usuarioService.buscarPorEmail(email)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + email));
    }

    /**
     * Consumir inventario (alias de reducirInventario para mantener consistencia con ProductoRecetaService)
     */
    @Transactional
    public void consumirInventario(Long productoId, Long sucursalId, BigDecimal cantidad, String motivo) {
        reducirInventario(productoId, sucursalId, cantidad, motivo);
    }

    /**
     * Verificar si se puede producir cantidad de producto
     */
    @Transactional(readOnly = true)
    public boolean puedeProducir(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        // 1. Buscar receta
        ProductoReceta receta = recetaRepository
            .findByProductoIdAndEmpresaId(productoId, empresaId)
            .orElse(null);

        // Si no tiene receta, se puede producir (producto sin receta)
        if (receta == null) {
            return true;
        }

        // 2. Verificar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadNecesaria = ingrediente.getCantidad().multiply(cantidad);

            // Obtener el inventario completo
            ProductoInventario inventario = obtenerInventario(
                ingrediente.getProducto().getId(),
                sucursalId
            );

            // Verificar si existe inventario
            if (inventario == null) {
                log.warn("No existe inventario para ingrediente {}",
                    ingrediente.getProducto().getNombre());
                return false;
            }

            // Obtener cantidad disponible (actual - bloqueada)
            BigDecimal cantidadDisponible = inventario.getCantidadActual()
                .subtract(inventario.getCantidadBloqueada() != null ?
                    inventario.getCantidadBloqueada() : BigDecimal.ZERO);

            if (cantidadDisponible.compareTo(cantidadNecesaria) < 0) {
                log.warn("Ingrediente {} insuficiente. Necesario: {}, Disponible: {}",
                    ingrediente.getProducto().getNombre(),
                    cantidadNecesaria,
                    cantidadDisponible);
                return false;
            }
        }

        return true;
    }

    /**
     * Descontar ingredientes al producir
     */
    @Transactional
    public void descontarIngredientes(Long empresaId, Long productoId, Long sucursalId, BigDecimal cantidad) {
        log.info("Descontando ingredientes para producir {} unidades de producto {}", cantidad, productoId);

        // 1. Obtener receta
        ProductoReceta receta = recetaRepository
            .findByProductoIdAndEmpresaId(productoId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        // 2. Descontar cada ingrediente
        for (RecetaIngrediente ingrediente : receta.getIngredientes()) {
            BigDecimal cantidadADescontar = ingrediente.getCantidad().multiply(cantidad);

            // Obtener inventario actual
            ProductoInventario inventario = obtenerInventario(
                ingrediente.getProducto().getId(),
                sucursalId
            );

            if (inventario == null) {
                throw new BusinessException("No existe inventario para ingrediente " +
                    ingrediente.getProducto().getNombre());
            }

            // Actualizar cantidad
            BigDecimal nuevaCantidad = inventario.getCantidadActual().subtract(cantidadADescontar);

            if (nuevaCantidad.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Stock insuficiente para " +
                    ingrediente.getProducto().getNombre());
            }

            inventario.setCantidadActual(nuevaCantidad);

            // Si el ProductoInventarioService tiene un método para actualizar, úsalo
            // Si no, necesitarás inyectar el repositorio directamente
//            inventarioRepository.actualizarInventario(inventario);

            log.info("Descontado {} de {}, nuevo stock: {}",
                cantidadADescontar,
                ingrediente.getProducto().getNombre(),
                nuevaCantidad);
        }
    }
}