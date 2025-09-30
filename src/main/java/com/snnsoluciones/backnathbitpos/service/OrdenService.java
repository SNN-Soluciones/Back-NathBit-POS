package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.orden.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenService {

  private final OrdenRepository ordenRepository;
  private final OrdenItemRepository ordenItemRepository;
  private final MesaRepository mesaRepository;
  private final ProductoRepository productoRepository;
  private final UsuarioRepository usuarioRepository;
  private final ClienteRepository clienteRepository;
  private final ProductoCompuestoSlotRepository slotRepository;

  @Transactional
  public OrdenResponse crearOrden(CrearOrdenRequest request) {
    log.info("Creando nueva orden para mesa ID: {}", request.mesaId());

    // Validar mesa
    Mesa mesa = mesaRepository.findById(request.mesaId())
        .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

    // Verificar si la mesa ya tiene orden activa
    if (mesa.tieneOrdenActiva()) {
      throw new BusinessException("La mesa ya tiene una orden activa");
    }

    // Verificar estado de la mesa
    if (mesa.getEstado() != EstadoMesa.DISPONIBLE) {
      throw new BusinessException("La mesa no está disponible");
    }

    ContextoUsuario contexto = (ContextoUsuario) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    Long usuarioId = contexto.getUserId();

    // Obtener usuario actual
    Usuario mesero = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    // Crear orden
    Orden orden = Orden.builder()
        .numero(generarNumeroOrden(mesa.getSucursal().getId()))
        .mesa(mesa)
        .sucursal(mesa.getSucursal())
        .mesero(mesero)
        .estado(EstadoOrden.ABIERTA)
        .numeroPersonas(request.numeroPersonas() != null ? request.numeroPersonas() : 1)
        .porcentajeServicio(request.porcentajeServicio() != null ?
            request.porcentajeServicio() : new BigDecimal("10"))
        .observaciones(request.observaciones())
        .build();

    // Cliente opcional
    if (request.clienteId() != null) {
      Cliente cliente = clienteRepository.findById(request.clienteId())
          .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
      orden.setCliente(cliente);
    } else if (request.nombreCliente() != null) {
      orden.setNombreCliente(request.nombreCliente());
    }

    orden.setFechaCreacion(LocalDateTime.now());
    orden.setFechaActualizacion(LocalDateTime.now());

    orden = ordenRepository.save(orden);

    // Actualizar estado de mesa
    mesa.setEstado(EstadoMesa.OCUPADA);
    mesaRepository.save(mesa);

    log.info("Orden creada: {}", orden.getNumero());
    return mapToResponse(orden);
  }

  @Transactional
  public OrdenResponse agregarItem(Long ordenId, AgregarItemRequest request) {
    log.info("Agregando item a orden ID: {}", ordenId);

    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    if (!orden.puedeModificarse()) {
      throw new BusinessException("La orden no puede modificarse en estado: " + orden.getEstado());
    }

    Producto producto = productoRepository.findById(request.productoId())
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    // Crear item
    OrdenItem item = OrdenItem.builder()
        .orden(orden)
        .producto(producto)
        .cantidad(request.cantidad())
        .precioUnitario(producto.getPrecioVenta())
        .tarifaImpuesto(obtenerTarifaImpuesto(producto))
        .notas(request.notas())
        .build();

    // Si es producto compuesto, agregar opciones
    if (producto.getTipo() == TipoProducto.COMPUESTO && request.opciones() != null) {
      for (OpcionCompuestaRequest opcionReq : request.opciones()) {
        ProductoCompuestoSlot slot = slotRepository.findById(opcionReq.slotId())
            .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

        Producto productoOpcion = productoRepository.findById(opcionReq.productoOpcionId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto opción no encontrado"));

        OrdenItemOpcion opcion = OrdenItemOpcion.builder()
            .ordenItemPadre(item)
            .slot(slot)
            .productoOpcion(productoOpcion)
            .cantidad(opcionReq.cantidad() != null ? opcionReq.cantidad() : 1)
            .nombreSlot(slot.getNombre())
            .nombreOpcion(productoOpcion.getNombre())
            .build();

        // Calcular precio adicional si aplica
        slot.getOpciones().stream()
            .filter(o -> o.getProducto().getId().equals(productoOpcion.getId()))
            .findFirst()
            .ifPresent(slotOpcion -> {
              if (slotOpcion.getPrecioAdicional() != null) {
                opcion.setPrecioAdicional(slotOpcion.getPrecioAdicional());
                opcion.setEsGratuita(false);
              } else {
                opcion.setPrecioAdicional(BigDecimal.ZERO);
                opcion.setEsGratuita(true);
              }
            });
      }
    }

    // Calcular totales del item
    item.calcularTotales();

    // Agregar a la orden
    orden.agregarItem(item);

    orden = ordenRepository.save(orden);

    log.info("Item agregado a orden: {}", orden.getNumero());
    return mapToResponse(orden);
  }

  @Transactional
  public void marcarComoPagada(Long ordenId, Long facturaId) {
    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    orden.setEstado(EstadoOrden.PAGADA);
    orden.setFechaCierre(LocalDateTime.now());
    // TODO: Guardar referencia a factura si es necesario

    ordenRepository.save(orden);

    // Liberar mesa
    Mesa mesa = orden.getMesa();
    mesa.actualizarEstadoSegunOrden();
    mesaRepository.save(mesa);
  }

  @Transactional
  public OrdenResponse actualizarItem(Long ordenId, Long itemId, ActualizarItemRequest request) {
    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    if (!orden.puedeModificarse()) {
      throw new BusinessException("La orden no puede modificarse");
    }

    OrdenItem item = orden.getItems().stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en la orden"));

    if (item.getEnviadoCocina()) {
      throw new BusinessException("El item ya fue enviado a cocina y no puede modificarse");
    }

    item.setCantidad(request.cantidad());
    item.setNotas(request.notas());
    item.calcularTotales();

    orden.recalcularTotales();
    orden = ordenRepository.save(orden);

    return mapToResponse(orden);
  }

  @Transactional
  public OrdenResponse eliminarItem(Long ordenId, Long itemId) {
    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    if (!orden.puedeModificarse()) {
      throw new BusinessException("La orden no puede modificarse");
    }

    OrdenItem item = orden.getItems().stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

    if (item.getEnviadoCocina()) {
      throw new BusinessException("El item ya fue enviado a cocina y no puede eliminarse");
    }

    orden.removerItem(item);
    orden = ordenRepository.save(orden);

    return mapToResponse(orden);
  }

  @Transactional
  public OrdenResponse enviarCocina(Long ordenId) {
    log.info("Enviando orden {} a cocina", ordenId);

    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    if (!orden.getEstado().puedeEnviarCocina()) {
      throw new BusinessException(
          "La orden no puede enviarse a cocina en estado: " + orden.getEstado());
    }

    // Marcar items no enviados
    boolean hayItemsNuevos = false;
    for (OrdenItem item : orden.getItems()) {
      if (!item.getEnviadoCocina()) {
        item.marcarEnviadoCocina();
        hayItemsNuevos = true;
      }
    }

    if (!hayItemsNuevos) {
      throw new BusinessException("No hay items nuevos para enviar a cocina");
    }

    // Actualizar estado si es necesario
    if (orden.getEstado() == EstadoOrden.ABIERTA) {
      orden.setEstado(EstadoOrden.EN_PREPARACION);
    }

    orden = ordenRepository.save(orden);

    log.info("Orden {} enviada a cocina", orden.getNumero());
    return mapToResponse(orden);
  }

  @Transactional
  public OrdenResponse cambiarEstado(Long ordenId, ActualizarEstadoOrdenRequest request) {
    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    EstadoOrden estadoAnterior = orden.getEstado();
    EstadoOrden nuevoEstado = request.nuevoEstado();

    // Validaciones según el nuevo estado
    switch (nuevoEstado) {
      case PAGADA:
        if (!estadoAnterior.puedePagarse()) {
          throw new BusinessException("No se puede pagar una orden en estado: " + estadoAnterior);
        }
        orden.setFechaCierre(LocalDateTime.now());

        // Liberar mesa
        Mesa mesa = orden.getMesa();
        if (!mesa.tieneOrdenActiva()) {
          mesa.setEstado(EstadoMesa.DISPONIBLE);
          mesaRepository.save(mesa);
        }
        break;

      case ANULADA:
        if (estadoAnterior.esFinal()) {
          throw new BusinessException("No se puede anular una orden finalizada");
        }
        break;
    }

    orden.setEstado(nuevoEstado);
    orden = ordenRepository.save(orden);

    log.info("Orden {} cambió de estado: {} -> {}", orden.getNumero(), estadoAnterior, nuevoEstado);
    return mapToResponse(orden);
  }

  @Transactional(readOnly = true)
  public OrdenResponse obtenerOrden(Long ordenId) {
    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
    return mapToResponse(orden);
  }

  @Transactional(readOnly = true)
  public OrdenResponse obtenerOrdenActivaPorMesa(Long mesaId) {
    Orden orden = ordenRepository.findOrdenActivaPrincipalByMesaId(mesaId)
        .orElseThrow(() -> new ResourceNotFoundException("No hay orden activa para esta mesa"));
    return mapToResponse(orden);
  }

  @Transactional(readOnly = true)
  public List<OrdenListResponse> listarOrdenesPorSucursal(Long sucursalId, EstadoOrden estado) {
    List<Orden> ordenes;

    if (estado != null) {
      ordenes = ordenRepository.findBySucursalIdAndEstadoOrderByFechaCreacionDesc(sucursalId,
          estado);
    } else {
      ordenes = ordenRepository.findOrdenesAbiertasBySucursalId(sucursalId);
    }

    return ordenes.stream()
        .map(this::mapToListResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<OrdenCocinaResponse> obtenerOrdenesParaCocina(Long sucursalId) {
    List<Orden> ordenes = ordenRepository.findOrdenesEnCocina(sucursalId);

    return ordenes.stream()
        .map(this::mapToCocinaResponse)
        .collect(Collectors.toList());
  }

  // Método auxiliar para generar número de orden
  private String generarNumeroOrden(Long sucursalId) {
    String prefijo = "ORD-" + LocalDate.now().getYear() + "-";

    Integer ultimoNumero = ordenRepository
        .findMaxNumeroOrden(sucursalId, prefijo)
        .orElse(0);

    int nuevoNumero = ultimoNumero + 1;
    return String.format("%s%05d", prefijo, nuevoNumero);
  }

  // Métodos de mapeo
  private OrdenResponse mapToResponse(Orden orden) {
    List<OrdenItemResponse> items = orden.getItems().stream()
        .map(this::mapItemToResponse)
        .collect(Collectors.toList());

    return new OrdenResponse(
        orden.getId(),
        orden.getNumero(),
        orden.getMesa().getId(),
        orden.getMesa().getCodigo(),
        orden.getMesa().getZona().getNombre(),
        orden.getMesero().getId(),
        orden.getMesero().getNombre(),
        orden.getEstado(),
        orden.getEstado().getDescripcion(),
        orden.getCliente() != null ? orden.getCliente().getId() : null,
        orden.getCliente() != null ? orden.getCliente().getRazonSocial() : orden.getNombreCliente(),
        orden.getNumeroPersonas(),
        orden.getPorcentajeServicio(),
        orden.getObservaciones(),
        items,
        orden.getSubtotal(),
        orden.getTotalDescuento(),
        orden.getTotalImpuesto(),
        orden.getTotalServicio(),
        orden.getTotal(),
        orden.getEsSplit(),
        orden.getOrdenPadre() != null ? orden.getOrdenPadre().getId() : null,
        orden.getFechaCreacion(),
        orden.getFechaActualizacion(),
        orden.getFechaCierre(),
        orden.getFactura() != null ? orden.getFactura().getId() : null,
        orden.getFactura() != null ? orden.getFactura().getConsecutivo() : null
    );
  }

  private OrdenItemResponse mapItemToResponse(OrdenItem item) {
    List<OrdenItemOpcionResponse> opciones = item.getOpciones().stream()
        .map(opcion -> new OrdenItemOpcionResponse(
            opcion.getId(),
            opcion.getSlot().getId(),
            opcion.getNombreSlot(),
            opcion.getProductoOpcion().getId(),
            opcion.getNombreOpcion(),
            opcion.getCantidad(),
            opcion.getPrecioAdicional(),
            opcion.getEsGratuita()
        ))
        .collect(Collectors.toList());

    return new OrdenItemResponse(
        item.getId(),
        item.getProducto().getId(),
        item.getProducto().getNombre(),
        item.getProducto().getCodigoInterno(),
        item.getCantidad(),
        item.getPrecioUnitario(),
        item.getPorcentajeDescuento(),
        item.getTarifaImpuesto(),
        item.getSubtotal(),
        item.getTotalDescuento(),
        item.getTotalImpuesto(),
        item.getTotal(),
        item.getNotas(),
        item.getEnviadoCocina(),
        item.getFechaEnvioCocina(),
        item.getPreparado(),
        item.getFechaPreparado(),
        item.getEntregado(),
        item.getFechaEntregado(),
        opciones
    );
  }

  private OrdenListResponse mapToListResponse(Orden orden) {
    long minutosTranscurridos = ChronoUnit.MINUTES.between(orden.getFechaCreacion(),
        LocalDateTime.now());

    return new OrdenListResponse(
        orden.getId(),
        orden.getNumero(),
        orden.getMesa().getCodigo(),
        orden.getMesero().getNombre(),
        orden.getEstado(),
        orden.getItems().size(),
        orden.getTotal(),
        orden.getFechaCreacion(),
        (int) minutosTranscurridos
    );
  }

  private OrdenCocinaResponse mapToCocinaResponse(Orden orden) {
    List<ItemCocinaResponse> items = orden.getItems().stream()
        .filter(item -> item.getEnviadoCocina() && !item.getPreparado())
        .map(item -> {
          long minutosEspera = ChronoUnit.MINUTES.between(item.getFechaEnvioCocina(),
              LocalDateTime.now());

          List<String> opciones = item.getOpciones().stream()
              .map(o -> o.getNombreSlot() + ": " + o.getNombreOpcion())
              .collect(Collectors.toList());

          return new ItemCocinaResponse(
              item.getId(),
              item.getProducto().getNombre(),
              item.getCantidad(),
              item.getNotas(),
              opciones,
              item.getFechaEnvioCocina(),
              (int) minutosEspera,
              minutosEspera > 15 // Urgente si lleva más de 15 minutos
          );
        })
        .collect(Collectors.toList());

    return new OrdenCocinaResponse(
        orden.getId(),
        orden.getNumero(),
        orden.getMesa().getCodigo(),
        items
    );
  }

  // En OrdenService.java, agregar este método helper:
  private BigDecimal obtenerTarifaImpuesto(Producto producto) {
    // Si el producto tiene impuestos configurados
    if (producto.getImpuestos() != null && !producto.getImpuestos().isEmpty()) {
      // Buscar el IVA (código 01) o tomar el primer impuesto
      return producto.getImpuestos().stream()
          .filter(imp -> "01".equals(imp.getTipoImpuesto().name())) // IVA
          .map(imp -> imp.getPorcentaje() != null ? imp.getPorcentaje() : BigDecimal.ONE)
          .findFirst()
          .orElse(BigDecimal.ONE);
    }

    // Si no tiene impuestos, devolver ZERO
    return BigDecimal.ZERO;
  }
}