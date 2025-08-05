// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/service/operacion/impl/OrdenServiceImpl.java

package com.snnsoluciones.backnathbitpos.service.operacion.impl;

import com.snnsoluciones.backnathbitpos.dto.request.OrdenRequest;
import com.snnsoluciones.backnathbitpos.dto.response.OrdenResponse;
import com.snnsoluciones.backnathbitpos.entity.catalogo.Cliente;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.operacion.Mesa;
import com.snnsoluciones.backnathbitpos.entity.operacion.Orden;
import com.snnsoluciones.backnathbitpos.entity.operacion.OrdenDetalle;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.OrdenMapper;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import com.snnsoluciones.backnathbitpos.service.operacion.OrdenCalculoService;
import com.snnsoluciones.backnathbitpos.service.operacion.OrdenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository ordenRepository;
    private final MesaRepository mesaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioGlobalRepository usuarioRepository;
    private final CajaRepository cajaRepository;
    private final SucursalRepository sucursalRepository;
    private final OrdenCalculoService calculoService;
    private final OrdenMapper ordenMapper;

    @Override
    public OrdenResponse crear(OrdenRequest request) {
        log.info("Creando nueva orden tipo: {}", request.getTipo());

        Orden orden = new Orden();
        orden.setTipo(request.getTipo());
        orden.setFechaOrden(LocalDateTime.now());
        orden.setEstado(EstadoOrden.PENDIENTE);

        // Generar número de orden
        orden.setNumeroOrden(generarNumeroOrden(request.getTipo()));

        // Asignar mesa si aplica
        if (request.getMesaId() != null) {
            Mesa mesa = mesaRepository.findById(request.getMesaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

            if (!mesa.estaLibre()) {
                throw new BusinessException("La mesa no está disponible");
            }

            orden.setMesa(mesa);
            mesa.ocupar(usuarioRepository.findById(request.getMeseroId()).orElse(null), request.getCantidadPersonas(), request.getNombreCliente());
            mesaRepository.save(mesa);
        }

        // Asignar cliente si existe
        if (request.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            orden.setCliente(cliente);
        }

        // Asignar mesero
        if (request.getMeseroId() != null) {
            UsuarioGlobal mesero = usuarioRepository.findById(request.getMeseroId())
                .orElseThrow(() -> new ResourceNotFoundException("Mesero no encontrado"));
            orden.setMesero(mesero);
        }

        // Información adicional
        orden.setCantidadPersonas(request.getCantidadPersonas());
        orden.setObservaciones(request.getObservaciones());

        // Para delivery/takeaway
        if (TipoOrden.DELIVERY.equals(request.getTipo()) ||
            TipoOrden.PARA_LLEVAR.equals(request.getTipo())) {
            orden.setNombreClienteDelivery(request.getNombreClienteDelivery());
            orden.setTelefonoDelivery(request.getTelefonoDelivery());
            orden.setDireccionDelivery(request.getDireccionDelivery());
            orden.setHoraEntregaEstimada(request.getHoraEntregaEstimada());
        }

        // Inicializar totales
        orden.setSubtotal(BigDecimal.ZERO);
        orden.setTotalDescuentos(BigDecimal.ZERO);
        orden.setTotalImpuestos(BigDecimal.ZERO);
        orden.setTotal(BigDecimal.ZERO);

        orden = ordenRepository.save(orden);
        log.info("Orden creada exitosamente: {}", orden.getNumeroOrden());

        return ordenMapper.toResponse(orden);
    }

    @Override
    public OrdenResponse actualizar(UUID id, OrdenRequest request) {
        log.info("Actualizando orden: {}", id);

        Orden orden = ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // Validar que se pueda actualizar
        if (EstadoOrden.PAGADA.equals(orden.getEstado()) ||
            EstadoOrden.CANCELADA.equals(orden.getEstado())) {
            throw new BusinessException("No se puede actualizar una orden pagada o cancelada");
        }

        // Actualizar campos permitidos
        if (request.getObservaciones() != null) {
            orden.setObservaciones(request.getObservaciones());
        }

        if (request.getCantidadPersonas() != null) {
            orden.setCantidadPersonas(request.getCantidadPersonas());
        }

        orden = ordenRepository.save(orden);
        return ordenMapper.toResponse(orden);
    }

    @Override
    public OrdenResponse obtenerPorId(UUID id) {
        Orden orden = ordenRepository.findByIdWithDetalles(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        return ordenMapper.toResponse(orden);
    }

    @Override
    public void eliminar(UUID id) {
        log.warn("Intentando eliminar orden: {}", id);

        Orden orden = ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // Solo permitir eliminar órdenes pendientes sin detalles
        if (!EstadoOrden.PENDIENTE.equals(orden.getEstado())) {
            throw new BusinessException("Solo se pueden eliminar órdenes pendientes");
        }

        if (!orden.getDetalles().isEmpty()) {
            throw new BusinessException("No se puede eliminar una orden con productos");
        }

        // Liberar mesa si aplica
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();
            mesaRepository.save(orden.getMesa());
        }

        ordenRepository.delete(orden);
        log.info("Orden eliminada: {}", orden.getNumeroOrden());
    }

    @Override
    public Page<OrdenResponse> listar(Pageable pageable) {
        return ordenRepository.findAll(pageable)
            .map(ordenMapper::toResponse);
    }

    @Override
    public OrdenResponse obtenerPorNumero(String numeroOrden) {
        Orden orden = ordenRepository.findByNumeroOrden(numeroOrden)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        return ordenMapper.toResponse(orden);
    }

    @Override
    public List<OrdenResponse> obtenerPorMesa(UUID mesaId) {
        Mesa mesa = mesaRepository.findById(mesaId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

        return ordenRepository.findByMesa(mesa).stream()
            .map(ordenMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrdenResponse> obtenerPorEstado(EstadoOrden estado) {
        return ordenRepository.findByEstado(estado).stream()
            .map(ordenMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrdenResponse> obtenerPorMesero(UUID meseroId) {
        UsuarioGlobal mesero = usuarioRepository.findById(meseroId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesero no encontrado"));

        return ordenRepository.findByMesero(mesero).stream()
            .map(ordenMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public OrdenDetalle agregarDetalle(UUID ordenId, OrdenDetalle detalle) {
        log.info("Agregando detalle a orden: {}", ordenId);

        Orden orden = ordenRepository.findByIdWithDetalles(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // Validar estado
        if (!puedeModificarDetalles(orden.getEstado())) {
            throw new BusinessException("No se pueden agregar productos en el estado actual");
        }

        // Validar detalle
        if (!calculoService.validarDetalle(detalle)) {
            throw new BusinessException("Detalle inválido");
        }

        // Configurar detalle
        detalle.setOrden(orden);
        detalle.setNumeroLinea(orden.getDetalles().size() + 1);
        detalle.setFechaPedido(LocalDateTime.now());

        // Calcular totales
        calculoService.calcularTotalesDetalle(detalle);

        // Agregar a la orden
        orden.getDetalles().add(detalle);

        // Recalcular totales
        calculoService.calcularTotalesOrden(orden);

        ordenRepository.save(orden);
        log.info("Detalle agregado exitosamente");

        return detalle;
    }

    @Override
    public OrdenDetalle actualizarDetalle(UUID ordenId, Integer numeroLinea, OrdenDetalle detalleActualizado) {
        log.info("Actualizando detalle {} de orden: {}", numeroLinea, ordenId);

        Orden orden = ordenRepository.findByIdWithDetalles(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!puedeModificarDetalles(orden.getEstado())) {
            throw new BusinessException("No se pueden modificar productos en el estado actual");
        }

        OrdenDetalle detalle = orden.getDetalles().stream()
            .filter(d -> d.getNumeroLinea().equals(numeroLinea))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado"));

        // Actualizar campos permitidos
        if (detalleActualizado.getCantidad() != null) {
            detalle.setCantidad(detalleActualizado.getCantidad());
        }

        if (detalleActualizado.getPorcentajeDescuento() != null) {
            detalle.setPorcentajeDescuento(detalleActualizado.getPorcentajeDescuento());
            detalle.setMotivoDescuento(detalleActualizado.getMotivoDescuento());
        }

        if (detalleActualizado.getObservaciones() != null) {
            detalle.setObservaciones(detalleActualizado.getObservaciones());
        }

        // Recalcular
        calculoService.calcularTotalesDetalle(detalle);
        calculoService.calcularTotalesOrden(orden);

        ordenRepository.save(orden);
        return detalle;
    }

    @Override
    public void eliminarDetalle(UUID ordenId, Integer numeroLinea) {
        log.info("Eliminando detalle {} de orden: {}", numeroLinea, ordenId);

        Orden orden = ordenRepository.findByIdWithDetalles(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!puedeModificarDetalles(orden.getEstado())) {
            throw new BusinessException("No se pueden eliminar productos en el estado actual");
        }

        OrdenDetalle detalle = orden.getDetalles().stream()
            .filter(d -> d.getNumeroLinea().equals(numeroLinea))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado"));

        if (!detalle.esCancelable()) {
            throw new BusinessException("Este producto no se puede eliminar");
        }

        orden.getDetalles().remove(detalle);

        // Reordenar números de línea
        int numero = 1;
        for (OrdenDetalle d : orden.getDetalles()) {
            d.setNumeroLinea(numero++);
        }

        // Recalcular totales
        calculoService.calcularTotalesOrden(orden);

        ordenRepository.save(orden);
    }

    @Override
    public void cambiarEstado(UUID ordenId, EstadoOrden nuevoEstado) {
        log.info("Cambiando estado de orden {} a {}", ordenId, nuevoEstado);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // Validar transición de estado
        if (!esTransicionValida(orden.getEstado(), nuevoEstado)) {
            throw new BusinessException(
                String.format("No se puede cambiar de %s a %s",
                    orden.getEstado(), nuevoEstado));
        }

        orden.setEstado(nuevoEstado);

        // Acciones según el nuevo estado
        if (EstadoOrden.SERVIDA.equals(nuevoEstado) && orden.getMesa() != null) {
            orden.getMesa().setEstado(EstadoMesa.CUENTA_PEDIDA);
            mesaRepository.save(orden.getMesa());
        }

        ordenRepository.save(orden);
    }

    @Override
    public void asignarMesero(UUID ordenId, UUID meseroId) {
        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        UsuarioGlobal mesero = usuarioRepository.findById(meseroId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesero no encontrado"));

        orden.setMesero(mesero);
        ordenRepository.save(orden);
    }

    @Override
    public void cambiarMesa(UUID ordenId, UUID nuevaMesaId) {
        log.info("Cambiando mesa de orden {}", ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!TipoOrden.MESA.equals(orden.getTipo())) {
            throw new BusinessException("Solo se pueden cambiar mesas en órdenes de tipo MESA");
        }

        Mesa nuevaMesa = mesaRepository.findById(nuevaMesaId)
            .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

        if (!nuevaMesa.estaLibre()) {
            throw new BusinessException("La mesa destino no está disponible");
        }

        // Liberar mesa actual
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();
            mesaRepository.save(orden.getMesa());
        }

        // Asignar nueva mesa
        orden.setMesa(nuevaMesa);
        nuevaMesa.ocupar(orden.getMesero(), orden.getCantidadPersonas(), null);

        mesaRepository.save(nuevaMesa);
        ordenRepository.save(orden);
    }

    @Override
    public void aplicarDescuentoGlobal(UUID ordenId, BigDecimal porcentajeDescuento) {
        Orden orden = ordenRepository.findByIdWithDetalles(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!puedeModificarDetalles(orden.getEstado())) {
            throw new BusinessException("No se pueden aplicar descuentos en el estado actual");
        }

        calculoService.aplicarDescuentoGlobal(orden, porcentajeDescuento);
        ordenRepository.save(orden);
    }

    @Override
    public void marcarEnPreparacion(UUID ordenId) {
        cambiarEstado(ordenId, EstadoOrden.EN_COCINA);
    }

    @Override
    public void marcarLista(UUID ordenId) {
        cambiarEstado(ordenId, EstadoOrden.LISTA);
    }

    @Override
    public void marcarServida(UUID ordenId) {
        cambiarEstado(ordenId, EstadoOrden.SERVIDA);
    }

    @Override
    public void marcarPagada(UUID ordenId, UUID cajaId) {
        log.info("Marcando orden {} como pagada", ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        Caja caja = cajaRepository.findById(cajaId)
            .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));

        if (!caja.estaAbierta()) {
            throw new BusinessException("La caja debe estar abierta");
        }

        orden.setCaja(caja);
        orden.setEstado(EstadoOrden.PAGADA);

        // Liberar mesa si aplica
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();
            mesaRepository.save(orden.getMesa());
        }

        ordenRepository.save(orden);
    }

    @Override
    public void cancelarOrden(UUID ordenId, String motivo) {
        log.info("Cancelando orden {}", ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (EstadoOrden.PAGADA.equals(orden.getEstado())) {
            throw new BusinessException("No se puede cancelar una orden pagada");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setObservaciones(motivo);

        // Liberar mesa si aplica
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();
            mesaRepository.save(orden.getMesa());
        }

        // Marcar todos los detalles como cancelados
        for (OrdenDetalle detalle : orden.getDetalles()) {
            detalle.marcarCancelado(motivo);
        }

        ordenRepository.save(orden);
    }

    @Override
    public Page<OrdenResponse> buscar(String numeroOrden,
        EstadoOrden estado,
        TipoOrden tipo,
        UUID meseroId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Pageable pageable) {
        return ordenRepository.buscarOrdenes(numeroOrden, estado, tipo, meseroId,
                fechaInicio, fechaFin, pageable)
            .map(ordenMapper::toResponse);
    }

    // Métodos privados de utilidad

    private String generarNumeroOrden(TipoOrden tipo) {
        String prefijo = switch (tipo) {
            case MESA -> "M";
            case PARA_LLEVAR -> "L";
            case DELIVERY -> "D";
            case EXPRESS -> "E";
        };

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long secuencia = ordenRepository.count() + 1;

        return String.format("%s-%s-%05d", prefijo, fecha, secuencia);
    }

    private boolean puedeModificarDetalles(EstadoOrden estado) {
        return EstadoOrden.PENDIENTE.equals(estado) ||
            EstadoOrden.EN_COCINA.equals(estado);
    }

    private boolean esTransicionValida(EstadoOrden actual, EstadoOrden nuevo) {
        return switch (actual) {
            case PENDIENTE -> true; // Puede cambiar a cualquier estado
            case EN_COCINA -> !EstadoOrden.PENDIENTE.equals(nuevo);
            case LISTA -> EstadoOrden.SERVIDA.equals(nuevo) ||
                EstadoOrden.CANCELADA.equals(nuevo);
            case SERVIDA -> EstadoOrden.PAGADA.equals(nuevo) ||
                EstadoOrden.CANCELADA.equals(nuevo);
            case PAGADA, CANCELADA, DEVUELTA -> false; // Estados finales
        };
    }
}