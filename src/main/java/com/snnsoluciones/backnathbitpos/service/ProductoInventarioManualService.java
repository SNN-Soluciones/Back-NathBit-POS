package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.inventario.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoMovimiento;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extensión del servicio de inventario con métodos para ajustes manuales
 * 
 * FLUJO MANUAL:
 * 1. Carga inicial de inventarios (lote)
 * 2. Ajustes individuales (entrada/salida)
 * 3. Consulta de kardex/movimientos
 * 4. Reportes de inventario actual
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoInventarioManualService {

    private final ProductoInventarioRepository inventarioRepository;
    private final ProductoMovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioService usuarioService;

    // ==================== CARGA INICIAL EN LOTE ====================

    /**
     * Carga inicial de inventarios para una sucursal
     * Útil al dar de alta un nuevo cliente o sucursal
     * 
     * @param request Listado de productos con cantidad inicial
     * @return Lista de inventarios creados
     */
    @Transactional
    public List<InventarioActualDTO> cargarInventarioInicial(CargaInicialInventarioDTO request) {
        log.info("Iniciando carga de inventario inicial para sucursal {}", request.getSucursalId());

        // Validar sucursal
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        if (!sucursal.getManejaInventario()) {
            throw new BusinessException(
                "La sucursal " + sucursal.getNombre() + " no maneja inventario"
            );
        }

        Usuario usuario = obtenerUsuarioActual();
        List<InventarioActualDTO> resultados = new ArrayList<>();

        // Procesar cada producto
        for (CargaInicialInventarioDTO.ProductoInventarioInicialDTO item : request.getProductos()) {
            try {
                InventarioActualDTO inventario = cargarInventarioInicialProducto(
                    item, sucursal, usuario, request.getObservaciones()
                );
                resultados.add(inventario);
            } catch (Exception e) {
                log.error("Error cargando inventario para producto {}: {}", 
                    item.getProductoId(), e.getMessage());
                // Continuar con los demás productos
            }
        }

        log.info("Carga inicial completada: {} productos procesados de {} solicitados", 
            resultados.size(), request.getProductos().size());

        return resultados;
    }

    /**
     * Carga inventario inicial de un producto individual
     */
    private InventarioActualDTO cargarInventarioInicialProducto(
            CargaInicialInventarioDTO.ProductoInventarioInicialDTO item,
            Sucursal sucursal,
            Usuario usuario,
            String observacionesGenerales) {

        // Validar producto
        Producto producto = productoRepository.findById(item.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getProductoId()));

        if (!producto.requiereControlInventario()) {
            throw new BusinessException(
                "Producto " + producto.getNombre() + " no maneja inventario"
            );
        }

        // Buscar o crear inventario
        ProductoInventario inventario = inventarioRepository
            .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
            .orElseGet(() -> {
                ProductoInventario nuevo = ProductoInventario.builder()
                    .producto(producto)
                    .sucursal(sucursal)
                    .cantidadActual(BigDecimal.ZERO)
                    .cantidadMinima(item.getCantidadMinima() != null ? 
                        item.getCantidadMinima() : BigDecimal.ZERO)
                    .cantidadBloqueada(BigDecimal.ZERO)
                    .ultimaActualizacion(LocalDateTime.now())
                    .estado(true)
                    .build();
                return inventarioRepository.save(nuevo);
            });

        // Si ya tenía stock, lanzar warning
        if (inventario.getCantidadActual().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Producto {} ya tenía stock {} en sucursal {}. Sumando cantidad inicial.", 
                producto.getNombre(), inventario.getCantidadActual(), sucursal.getNombre());
        }

        // Actualizar inventario
        BigDecimal saldoAnterior = inventario.getCantidadActual();
        BigDecimal saldoNuevo = saldoAnterior.add(item.getCantidadInicial());

        inventario.setCantidadActual(saldoNuevo);
        inventario.setCantidadMinima(item.getCantidadMinima() != null ? 
            item.getCantidadMinima() : inventario.getCantidadMinima());
        inventario.setUltimaActualizacion(LocalDateTime.now());
        inventario = inventarioRepository.save(inventario);

        // Registrar movimiento
        String observaciones = item.getObservaciones() != null ? 
            item.getObservaciones() : observacionesGenerales;
        if (observaciones == null) {
            observaciones = "Carga inicial de inventario";
        }

        registrarMovimiento(
            producto,
            sucursal,
            TipoMovimiento.ENTRADA_INICIAL,
            item.getCantidadInicial(),
            saldoAnterior,
            saldoNuevo,
            observaciones,
            usuario,
            item.getPrecioCompra(),
            "CARGA-INICIAL-" + LocalDateTime.now().toLocalDate()
        );

        log.info("Inventario inicial cargado: {} | {} {} en {}", 
            producto.getNombre(), saldoNuevo, producto.getUnidadMedida(), sucursal.getNombre());

        return mapearAInventarioActualDTO(inventario);
    }

    // ==================== AJUSTE INDIVIDUAL ====================

    /**
     * Ajuste manual de inventario (entrada o salida)
     * Usado para correcciones, mermas, consumos internos, etc.
     * 
     * @param ajuste Datos del ajuste a realizar
     * @return Inventario actualizado
     */
    @Transactional
    public InventarioActualDTO ajustarInventario(AjusteInventarioDTO ajuste) {
        log.info("Ajustando inventario: Producto {} | Tipo {} | Cantidad {}",
            ajuste.getProductoId(), ajuste.getTipoMovimiento(), ajuste.getCantidad());

        // Validaciones
        Producto producto = productoRepository.findById(ajuste.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        Sucursal sucursal = sucursalRepository.findById(ajuste.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        if (!sucursal.getManejaInventario()) {
            throw new BusinessException("La sucursal no maneja inventario");
        }

        if (!producto.requiereControlInventario()) {
            throw new BusinessException("El producto no maneja inventario");
        }

        // Parsear tipo de movimiento
        TipoMovimiento tipoMovimiento;
        try {
            tipoMovimiento = TipoMovimiento.valueOf(ajuste.getTipoMovimiento());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de movimiento inválido: " + ajuste.getTipoMovimiento());
        }

        // Obtener o crear inventario
        ProductoInventario inventario = inventarioRepository
            .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
            .orElseGet(() -> crearInventarioInicial(producto, sucursal));

        // Calcular nueva cantidad según tipo de movimiento
        BigDecimal saldoAnterior = inventario.getCantidadActual();
        BigDecimal cantidadCambio = ajuste.getCantidad();
        BigDecimal saldoNuevo;

        if (tipoMovimiento.esEntrada()) {
            // Entradas: sumar
            saldoNuevo = saldoAnterior.add(cantidadCambio);
        } else if (tipoMovimiento.esSalida()) {
            // Salidas: restar
            saldoNuevo = saldoAnterior.subtract(cantidadCambio);

            // Validar si permite negativos
            if (!sucursal.getPermiteNegativos() && saldoNuevo.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(String.format(
                    "Stock insuficiente. Disponible: %s, Intentando descontar: %s",
                    saldoAnterior, cantidadCambio
                ));
            }
        } else {
            throw new BusinessException("Tipo de movimiento no soportado: " + tipoMovimiento);
        }

        // Actualizar inventario
        inventario.setCantidadActual(saldoNuevo);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        inventario = inventarioRepository.save(inventario);

        // Registrar movimiento
        Usuario usuario = obtenerUsuarioActual();
        BigDecimal cantidadMovimiento = tipoMovimiento.esSalida() ? 
            cantidadCambio.negate() : cantidadCambio;

        registrarMovimiento(
            producto,
            sucursal,
            tipoMovimiento,
            cantidadMovimiento,
            saldoAnterior,
            saldoNuevo,
            ajuste.getObservaciones(),
            usuario,
            ajuste.getPrecioUnitario(),
            ajuste.getDocumentoReferencia()
        );

        log.info("Ajuste completado: {} | {} → {} {}", 
            producto.getNombre(), saldoAnterior, saldoNuevo, producto.getUnidadMedida());

        return mapearAInventarioActualDTO(inventario);
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtener kardex/movimientos de un producto en una sucursal
     * 
     * @param productoId ID del producto
     * @param sucursalId ID de la sucursal
     * @param pageable Paginación
     * @return Movimientos paginados
     */
    @Transactional(readOnly = true)
    public Page<MovimientoInventarioDTO> obtenerKardex(Long productoId, Long sucursalId, Pageable pageable) {
        log.debug("Consultando kardex: Producto {} | Sucursal {}", productoId, sucursalId);

        Page<ProductoMovimiento> movimientos = movimientoRepository
            .findByProductoIdAndSucursalIdOrderByFechaMovimientoDesc(productoId, sucursalId, pageable);

        return movimientos.map(this::mapearAMovimientoDTO);
    }

    /**
     * Obtener todos los movimientos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @param pageable Paginación
     * @return Movimientos paginados
     */
    @Transactional(readOnly = true)
    public Page<MovimientoInventarioDTO> obtenerMovimientosPorSucursal(Long sucursalId, Pageable pageable) {
        log.debug("Consultando movimientos de sucursal {}", sucursalId);

        Page<ProductoMovimiento> movimientos = movimientoRepository
            .findBySucursalIdOrderByFechaMovimientoDesc(sucursalId, pageable);

        return movimientos.map(this::mapearAMovimientoDTO);
    }

    /**
     * Obtener inventario actual de todos los productos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @return Lista de inventarios
     */
    @Transactional(readOnly = true)
    public List<InventarioActualDTO> obtenerInventariosPorSucursal(Long sucursalId) {
        log.debug("Consultando inventarios de sucursal {}", sucursalId);

        List<ProductoInventario> inventarios = inventarioRepository
            .findBySucursalIdAndEstadoTrue(sucursalId);

        return inventarios.stream()
            .map(this::mapearAInventarioActualDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener productos con stock bajo en una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @return Lista de productos bajo mínimo
     */
    @Transactional(readOnly = true)
    public List<InventarioActualDTO> obtenerProductosBajoMinimo(Long sucursalId) {
        log.debug("Consultando productos bajo mínimo en sucursal {}", sucursalId);

        List<ProductoInventario> inventarios = inventarioRepository
            .findBajoMinimosBySucursal(sucursalId);

        return inventarios.stream()
            .map(this::mapearAInventarioActualDTO)
            .collect(Collectors.toList());
    }

    // ==================== HELPERS PRIVADOS ====================

    private ProductoInventario crearInventarioInicial(Producto producto, Sucursal sucursal) {
        log.warn("Creando inventario inicial en 0 para producto {} en sucursal {}", 
            producto.getNombre(), sucursal.getNombre());

        ProductoInventario inventario = ProductoInventario.builder()
            .producto(producto)
            .sucursal(sucursal)
            .cantidadActual(BigDecimal.ZERO)
            .cantidadMinima(BigDecimal.ZERO)
            .cantidadBloqueada(BigDecimal.ZERO)
            .ultimaActualizacion(LocalDateTime.now())
            .estado(true)
            .build();

        return inventarioRepository.save(inventario);
    }

    private void registrarMovimiento(Producto producto, Sucursal sucursal, TipoMovimiento tipo,
                                     BigDecimal cantidad, BigDecimal saldoAnterior, BigDecimal saldoNuevo,
                                     String observaciones, Usuario usuario, BigDecimal precioUnitario,
                                     String documentoReferencia) {

        ProductoMovimiento movimiento = ProductoMovimiento.builder()
            .producto(producto)
            .sucursal(sucursal)
            .tipoMovimiento(tipo)
            .cantidad(cantidad)
            .saldoAnterior(saldoAnterior)
            .saldoNuevo(saldoNuevo)
            .observaciones(observaciones)
            .usuario(usuario)
            .fechaMovimiento(LocalDateTime.now())
            .precioUnitario(precioUnitario)
            .documentoReferencia(documentoReferencia)
            .build();

        if (precioUnitario != null && cantidad != null) {
            movimiento.setCostoTotal(precioUnitario.multiply(cantidad.abs()));
        }

        movimientoRepository.save(movimiento);
    }

    private Usuario obtenerUsuarioActual() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new BusinessException("No hay usuario autenticado");
        }

        String email = auth.getName(); // El email es el principal en tu sistema

        return usuarioService.buscarPorEmail(email)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + email));
    }


        // ==================== MAPPERS ====================

    private InventarioActualDTO mapearAInventarioActualDTO(ProductoInventario inventario) {
        BigDecimal disponible = inventario.getCantidadActual()
            .subtract(inventario.getCantidadBloqueada() != null ? 
                inventario.getCantidadBloqueada() : BigDecimal.ZERO);

        InventarioActualDTO dto = InventarioActualDTO.builder()
            .id(inventario.getId())
            .productoId(inventario.getProducto().getId())
            .productoNombre(inventario.getProducto().getNombre())
            .productoCodigo(inventario.getProducto().getCodigoInterno())
            .unidadMedida(inventario.getProducto().getUnidadMedida() != null ? 
                inventario.getProducto().getUnidadMedida().getDescripcion() : "")
            .tipoInventario(inventario.getProducto().getTipoInventario().name())
            .sucursalId(inventario.getSucursal().getId())
            .sucursalNombre(inventario.getSucursal().getNombre())
            .cantidadActual(inventario.getCantidadActual())
            .cantidadMinima(inventario.getCantidadMinima())
            .cantidadBloqueada(inventario.getCantidadBloqueada())
            .cantidadDisponible(disponible)
            .bajominimo(inventario.getCantidadActual().compareTo(inventario.getCantidadMinima()) < 0)
            .agotado(disponible.compareTo(BigDecimal.ZERO) <= 0)
            .precioCompra(inventario.getProducto().getPrecioCompra())
            .ultimaActualizacion(inventario.getUltimaActualizacion())
            .build();

        // Calcular valor de inventario
        if (inventario.getProducto().getPrecioCompra() != null) {
            dto.setValorInventario(
                inventario.getCantidadActual().multiply(inventario.getProducto().getPrecioCompra())
            );
        }

        // Calcular estado
        dto.setEstadoStock(dto.calcularEstadoStock());

        return dto;
    }

    private MovimientoInventarioDTO mapearAMovimientoDTO(ProductoMovimiento movimiento) {
        return MovimientoInventarioDTO.builder()
            .id(movimiento.getId())
            .productoId(movimiento.getProducto().getId())
            .productoNombre(movimiento.getProducto().getNombre())
            .productoCodigo(movimiento.getProducto().getCodigoInterno())
            .unidadMedida(movimiento.getProducto().getUnidadMedida() != null ? 
                movimiento.getProducto().getUnidadMedida().getDescripcion() : "")
            .sucursalId(movimiento.getSucursal().getId())
            .sucursalNombre(movimiento.getSucursal().getNombre())
            .tipoMovimiento(movimiento.getTipoMovimiento())
            .tipoMovimientoDescripcion(movimiento.getTipoMovimiento().getDescripcion())
            .esEntrada(movimiento.getTipoMovimiento().esEntrada())
            .cantidad(movimiento.getCantidad())
            .saldoAnterior(movimiento.getSaldoAnterior())
            .saldoNuevo(movimiento.getSaldoNuevo())
            .precioUnitario(movimiento.getPrecioUnitario())
            .costoTotal(movimiento.getCostoTotal())
            .documentoReferencia(movimiento.getDocumentoReferencia())
            .observaciones(movimiento.getObservaciones())
            .usuarioId(movimiento.getUsuario() != null ? movimiento.getUsuario().getId() : null)
            .usuarioNombre(movimiento.getUsuario() != null ? movimiento.getUsuario().getNombre() : "")
            .fechaMovimiento(movimiento.getFechaMovimiento())
            .build();
    }
}