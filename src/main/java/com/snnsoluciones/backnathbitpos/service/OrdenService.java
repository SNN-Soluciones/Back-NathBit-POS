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
import java.util.Collections;
import java.util.Optional;
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
  private final SucursalRepository sucursalRepository;
  private final OrdenPersonaRepository ordenPersonaRepository;


  @Transactional
  public OrdenResponse crearOrden(CrearOrdenRequest request) {
    log.info("Creando nueva orden para mesa ID: {}", request.mesaId());

    // Validar mesa
    Mesa mesa = null;
    if (request.mesaId() != null) {
      mesa = mesaRepository.findById(request.mesaId())
          .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

      if (mesa.tieneOrdenActiva()) {
        throw new BusinessException("La mesa " + mesa.getCodigo()
            + " ya tiene una orden activa. Use el endpoint de agregar items.");
      }

      // Validar estado solo si NO tiene orden activa
      if (mesa.getEstado() != EstadoMesa.DISPONIBLE) {
        throw new BusinessException("La mesa " + mesa.getCodigo() + " no está disponible");
      }
    }

    Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    String numeroOrden = request.ordenNumero() != null ? request.ordenNumero()
        : generarNumeroOrden(request.sucursalId());

    ContextoUsuario contexto = (ContextoUsuario) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    Long usuarioId = contexto.getUserId();

    // Obtener usuario actual
    Usuario mesero = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    // Crear orden
    Orden orden = Orden.builder()
        .numero(numeroOrden)
        .mesa(mesa)  // Puede ser null
        .sucursal(sucursal)
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

    for (CrearOrdenRequest.ItemRequest itemReq : request.items()) {
      Producto producto = productoRepository.findById(itemReq.productoId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Producto no encontrado: " + itemReq.productoId()));

      BigDecimal precioUnitario = itemReq.precioUnitarioOverride() != null
          ? itemReq.precioUnitarioOverride()
          : (producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO);

      OrdenItem item = OrdenItem.builder()
          .orden(orden)
          .producto(producto)
          .cantidad(itemReq.cantidad())
          .precioUnitario(precioUnitario)
          .tarifaImpuesto(obtenerTarifaImpuesto(producto))
          .notas(itemReq.notas())
          .build();

      item.calcularTotales(); // Si existe este método
      orden.getItems().add(item);
    }

    // Recalcular totales de la orden
    orden.calcularTotales();
    orden = ordenRepository.save(orden);

    // Actualizar estado de mesa
    if (mesa != null) {
      mesa.setEstado(EstadoMesa.OCUPADA);
      mesaRepository.save(mesa);
    }

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

    // Usar precio override si existe, sino el precio del producto
    BigDecimal precioUnitario = request.precioUnitarioOverride() != null
        ? request.precioUnitarioOverride()
        : (producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO);

    OrdenItem item = OrdenItem.builder()
        .orden(orden)
        .producto(producto)
        .cantidad(request.cantidad())
        .precioUnitario(precioUnitario)
        .tarifaImpuesto(obtenerTarifaImpuesto(producto))
        .notas(request.notas())
        .build();

    // Calcular totales del item
    item.calcularTotales();

    // Agregar a la orden
    orden.agregarItem(item);
    orden.recalcularTotales();

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

        // ⭐ PRIMERO: Cambiar estado de la orden y guardar
        orden.setEstado(EstadoOrden.PAGADA);
        orden.setFechaCierre(LocalDateTime.now());
        orden = ordenRepository.save(orden);

        // ⭐ DESPUÉS: Liberar la mesa
        Mesa mesa = orden.getMesa();
        if (mesa != null) {
          mesa.actualizarEstadoSegunOrden(); // Verifica órdenes activas y actualiza estado
          mesaRepository.save(mesa);
          log.info("✅ Mesa {} liberada después de pagar orden {}", mesa.getCodigo(),
              orden.getNumero());
        }

        log.info("✅ Orden {} marcada como PAGADA", orden.getNumero());
        return mapToResponse(orden); // ⭐ Return aquí para evitar doble guardado

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

  /**
   * Genera número de orden único por sucursal y día Formato: ORD-DDMMAA-NNN Ejemplo: ORD-131125-001
   * (día 13/11/25, orden #1)
   */
  private String generarNumeroOrden(Long sucursalId) {
    LocalDate hoy = LocalDate.now();

    // Formato: DDMMAA (día-mes-año con 2 dígitos)
    String fechaFormato = String.format("%02d%02d%02d",
        hoy.getDayOfMonth(),
        hoy.getMonthValue(),
        hoy.getYear() % 100 // Últimos 2 dígitos del año (2025 -> 25)
    );

    // Buscar el último número del día en esta sucursal
    String patron = "ORD-" + fechaFormato + "-%";

    Optional<Orden> ultimaOrden = ordenRepository.findUltimaOrdenDelDia(sucursalId, patron);

    int siguiente = 1; // Por defecto, primera orden del día

    if (ultimaOrden.isPresent()) {
      // Extraer el número secuencial del último número
      String ultimoNumero = ultimaOrden.get().getNumero();
      // Ejemplo: "ORD-131125-001" -> extraer "001"
      String secuenciaStr = ultimoNumero.substring(ultimoNumero.lastIndexOf("-") + 1);

      try {
        int ultimaSecuencia = Integer.parseInt(secuenciaStr);
        siguiente = ultimaSecuencia + 1;
      } catch (NumberFormatException e) {
        log.warn("Error parseando secuencia de orden: {}", ultimoNumero, e);
        siguiente = 1;
      }
    }

    // Generar número con formato ORD-DDMMAA-NNN
    String numeroOrden = String.format("ORD-%s-%03d", fechaFormato, siguiente);

    log.info("Número de orden generado: {} para sucursal: {}", numeroOrden, sucursalId);

    return numeroOrden;
  }

  // Métodos de mapeo
  private OrdenResponse mapToResponse(Orden orden) {
    List<OrdenItemResponse> items = orden.getItems().stream()
        .map(this::mapItemToResponse)
        .collect(Collectors.toList());

    List<OrdenPersonaDTO> personasDTO = orden.getPersonas().stream()
        .map(this::mapPersonaToDTO)
        .collect(Collectors.toList());

    boolean tienePersonas = !orden.getPersonas().isEmpty();
    int cantidadPersonas = orden.getPersonas().size();
    long itemsCompartidos = orden.contarItemsCompartidos();

    return new OrdenResponse(
        orden.getId(),
        orden.getNumero(),
        orden.getMesa() != null ? orden.getMesa().getId() : null,
        orden.getMesa() != null ? orden.getMesa().getCodigo() : "VENTANILLA",
        orden.getMesa() != null ? orden.getMesa().getZona().getNombre() : "Para llevar",
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
        orden.getFactura() != null ? orden.getFactura().getConsecutivo() : null, // ⭐ String

        // ===== NUEVOS CAMPOS - PERSONAS =====
        tienePersonas,
        cantidadPersonas,
        personasDTO,
        (int) itemsCompartidos
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
        item.getProducto().getZonaPreparacion(),
        item.getNotas(),
        item.getEnviadoCocina(),
        item.getFechaEnvioCocina(),
        item.getPreparado(),
        item.getFechaPreparado(),
        item.getEntregado(),
        item.getFechaEntregado(),
        opciones,
        item.getEstadoPago() != null ? item.getEstadoPago().name() : "PENDIENTE",
        item.getFacturaInterna() != null ? item.getFacturaInterna().getId() : null,
        item.getFechaPago(),

        // ===== NUEVOS CAMPOS - PERSONA =====
        item.getOrdenPersona() != null ? item.getOrdenPersona().getId() : null,
        item.getOrdenPersona() != null ? item.getOrdenPersona().getNombre() : null,
        item.getOrdenPersona() != null ? item.getOrdenPersona().getColor() : null
    );
  }

  private OrdenListResponse mapToListResponse(Orden orden) {
    LocalDateTime creacion = orden.getFechaCreacion();
    long minutos = (creacion != null)
        ? ChronoUnit.MINUTES.between(creacion, LocalDateTime.now())
        : 0;

    String mesaCodigo = "VENTANILLA";
    if (orden.getMesa() != null && orden.getMesa().getCodigo() != null && !orden.getMesa()
        .getCodigo().isBlank()) {
      mesaCodigo = orden.getMesa().getCodigo();
    }

    String meseroNombre = (orden.getMesero() != null && orden.getMesero().getNombre() != null)
        ? orden.getMesero().getNombre()
        : "Sin asignar";

    // Mapear items -> DTO (y null-safe)
    List<OrdenItemResumenResponse> items =
        (orden.getItems() != null ? orden.getItems() : Collections.<OrdenItem>emptyList())
            .stream()
            .map(it -> new OrdenItemResumenResponse(
                it.getProducto() != null ? it.getProducto().getId() : null,
                it.getProducto() != null ? it.getProducto().getNombre() : "(Sin nombre)",
                it.getCantidad(),
                it.getNotas()
            ))
            .toList();

    BigDecimal total = orden.getTotal() != null ? orden.getTotal() : BigDecimal.ZERO;

    return new OrdenListResponse(
        orden.getId(),
        orden.getNumero(),
        mesaCodigo,
        meseroNombre,
        orden.getEstado(),
        items.size(),
        total,
        creacion,
        (int) minutos,
        items
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

  @Transactional
  public OrdenResponse actualizarNumeroPersonas(Long ordenId,
      ActualizarNumeroPersonasRequest request) {
    log.info("Actualizando número de personas de orden {}: {}", ordenId, request.numeroPersonas());

    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

    // Validar que la orden esté en estado modificable
    if (!orden.puedeModificarse()) {
      throw new BusinessException(
          "No se puede modificar el número de personas en estado: " + orden.getEstado());
    }

    // Actualizar número de personas
    orden.setNumeroPersonas(request.numeroPersonas());
    orden.setFechaActualizacion(LocalDateTime.now());

    orden = ordenRepository.save(orden);

    log.info("Número de personas actualizado exitosamente");
    return mapToResponse(orden);
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
          .orElse(BigDecimal.ZERO);
    }

    // Si no tiene impuestos, devolver ZERO
    return BigDecimal.ZERO;
  }

  // En OrdenService.java

  @Transactional(readOnly = true)
  public OrdenResponse obtenerOrdenPorId(Long ordenId) {
    log.info("Obteniendo orden por ID: {}", ordenId);

    Orden orden = ordenRepository.findById(ordenId)
        .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + ordenId));

    return mapToResponse(orden);
  }

  private OrdenPersonaDTO mapPersonaToDTO(OrdenPersona persona) {
    List<Long> itemIds = persona.getItems().stream()
        .map(OrdenItem::getId)
        .collect(Collectors.toList());

    String estadoPago = determinarEstadoPagoPersona(persona);

    return new OrdenPersonaDTO(
        persona.getId(),
        persona.getNombre(),
        persona.getColor(),
        persona.getOrdenVisualizacion(),
        persona.getCantidadItems(),
        persona.getTotal(),
        estadoPago,
        persona.getCreatedAt(),
        itemIds
    );
  }

  private String determinarEstadoPagoPersona(OrdenPersona persona) {
    if (persona.getItems().isEmpty()) {
      return "PENDIENTE";
    }

    boolean todosPagados = persona.todoPagado();
    boolean algunoPagado = persona.tienePagosParciales();

    if (todosPagados) {
      return "PAGADO";
    } else if (algunoPagado) {
      return "PARCIAL";
    } else {
      return "PENDIENTE";
    }
  }
}