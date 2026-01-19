package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.compuesto.ActualizarConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CalcularPrecioCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CrearConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.dto.compuesto.SlotConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoSlotDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoOpcionDto;
import com.snnsoluciones.backnathbitpos.dto.producto.SlotSeleccionDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.ConfiguracionFlujoDTO;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.OpcionPreguntaInicialDTO;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.SlotPreguntaInicialDTO;
import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCompuestoServiceImpl implements ProductoCompuestoService {

  private final ProductoRepository productoRepository;
  private final ProductoCompuestoRepository compuestoRepository;
  private final ProductoCompuestoSlotRepository slotRepository;
  private final ProductoCompuestoOpcionRepository opcionRepository;
  private final ProductoInventarioService inventarioService;
  private final ModelMapper modelMapper;
  private final SucursalRepository sucursalRepository;
  private final ProductoInventarioService productoInventarioService;
  private final FamiliaProductoRepository familiaProductoRepository;
  private final ProductoCompuestoConfiguracionRepository configuracionRepository;
  private final ProductoCompuestoSlotConfiguracionRepository slotConfigRepository;
  private final ProductoCompuestoOpcionRepository productoCompuestoOpcionRepository;
  private final SlotSeleccionValidator slotSeleccionValidator;

  @Override
  @Transactional
  public ProductoCompuestoDto crear(Long empresaId, Long productoId,
      ProductoCompuestoRequest request) {
    log.info("Creando configuración de compuesto para producto: {}", productoId);

    // Validar producto
    Producto producto = validarProducto(empresaId, productoId);

    // Validar que sea tipo COMPUESTO
    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // Validar que no exista configuración previa
    if (compuestoRepository.existsByProductoId(productoId)) {
      throw new BusinessException("El producto ya tiene configuración de compuesto");
    }

    // Crear compuesto
    ProductoCompuesto compuesto = ProductoCompuesto.builder()
        .producto(producto)
        .instruccionesPersonalizacion(request.getInstruccionesPersonalizacion())
        .tiempoPreparacionExtra(request.getTiempoPreparacionExtra())
        .build();

    compuesto = compuestoRepository.save(compuesto);

    // Crear slots con soporte de familias
    if (request.getSlots() != null && !request.getSlots().isEmpty()) {
      int ordenSlot = 0;
      for (var slotRequest : request.getSlots()) {
        crearSlotConFamilia(compuesto, slotRequest, empresaId, ordenSlot++);
      }
    }

    log.info("Configuración de compuesto creada exitosamente para producto: {}", productoId);
    return convertirADto(compuesto);
  }

  @Override
  @Transactional
  public ProductoCompuestoDto actualizar(Long empresaId, Long productoId,
      ProductoCompuestoRequest request) {
    log.info("Actualizando configuración de compuesto para producto: {}", productoId);

    validarProducto(empresaId, productoId);

    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

    // Actualizar datos básicos
    compuesto.setInstruccionesPersonalizacion(request.getInstruccionesPersonalizacion());
    compuesto.setTiempoPreparacionExtra(request.getTiempoPreparacionExtra());

    // Por simplicidad, eliminamos slots existentes y recreamos
    slotRepository.deleteByCompuestoId(compuesto.getId());

    // Recrear slots con soporte de familias
    if (request.getSlots() != null) {
      int ordenSlot = 0;
      for (var slotRequest : request.getSlots()) {
        crearSlotConFamilia(compuesto, slotRequest, empresaId, ordenSlot++);
      }
    }

    compuesto = compuestoRepository.save(compuesto);
    log.info("Configuración de compuesto actualizada exitosamente para producto: {}", productoId);

    return convertirADto(compuesto);
  }

  @Override
  @Transactional
  public void eliminar(Long empresaId, Long productoId) {
    log.info("Eliminando configuración de compuesto para producto: {}", productoId);

    validarProducto(empresaId, productoId);

    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

    compuestoRepository.delete(compuesto); // CASCADE eliminará slots y opciones
  }

  @Override
  @Transactional(readOnly = true)
  public ProductoCompuestoDto buscarPorProductoId(Long empresaId, Long productoId) {
    validarProducto(empresaId, productoId);

    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

    return convertirADto(compuesto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductoCompuestoDto> listarPorEmpresa(Long empresaId) {
    return productoRepository
        .findByEmpresaIdAndTipoAndActivoTrue(empresaId, TipoProducto.COMPUESTO)
        .stream()
        .map(producto -> {
          try {
            return buscarPorProductoId(empresaId, producto.getId());
          } catch (ResourceNotFoundException e) {
            log.warn("Producto compuesto sin configuración: {}", producto.getId());
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  // ========== MÉTODOS PRIVADOS ==========

  private Producto validarProducto(Long empresaId, Long productoId) {
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa");
    }

    return producto;
  }

  private ProductoCompuestoOpcion validarOpcion(Long slotId, Long opcionId) {
    ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
        .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));

    if (!opcion.getSlot().getId().equals(slotId)) {
      throw new BusinessException("La opción no pertenece al slot especificado");
    }

    return opcion;
  }

  private void agregarOpcionASlot(
      ProductoCompuestoSlot slot,
      ProductoCompuestoRequest.OpcionRequest opcionRequest,
      Long empresaId,
      int orden
  ) {
    ProductoCompuestoOpcion opcion = new ProductoCompuestoOpcion();
    opcion.setSlot(slot);
    opcion.setOrden(orden);
    opcion.setPrecioAdicional(
        opcionRequest.getPrecioAdicional() != null
            ? opcionRequest.getPrecioAdicional()
            : BigDecimal.ZERO
    );
    opcion.setEsDefault(
        opcionRequest.getEsDefault() != null
            ? opcionRequest.getEsDefault()
            : false
    );
    opcion.setDisponible(
        opcionRequest.getDisponible() != null
            ? opcionRequest.getDisponible()
            : true
    );

    // ⭐ MANEJO DE PRODUCTO VS NOMBRE
    if (opcionRequest.getProductoId() != null) {
      // Caso normal: tiene producto asociado
      Producto producto = productoRepository.findById(opcionRequest.getProductoId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Producto no encontrado con ID: " + opcionRequest.getProductoId()
          ));
      opcion.setProducto(producto);
      // Nombre opcional si viene en request
      opcion.setNombre(opcionRequest.getNombre());
    } else {
      // Caso slot maestro: solo nombre, sin producto
      if (opcionRequest.getNombre() == null || opcionRequest.getNombre().isBlank()) {
        throw new BusinessException(
            "Las opciones sin producto deben tener un nombre (slot maestro)"
        );
      }
      opcion.setProducto(null);
      opcion.setNombre(opcionRequest.getNombre());
    }

    opcionRepository.save(opcion);
  }

  private ProductoCompuestoDto convertirADto(ProductoCompuesto compuesto) {
    ProductoCompuestoDto dto = modelMapper.map(compuesto, ProductoCompuestoDto.class);
    dto.setProductoId(compuesto.getProducto().getId());
    dto.setProductoNombre(compuesto.getProducto().getNombre());
    dto.setProductoCodigoInterno(compuesto.getProducto().getCodigoInterno());

    if (compuesto.getSlotPreguntaInicial() != null) {
      dto.setSlotPreguntaInicialId(compuesto.getSlotPreguntaInicial().getId());
    }

    // Cargar slots usando el nuevo método que soporta familias
    List<ProductoCompuestoSlot> slots = slotRepository.findByCompuestoIdOrderByOrden(
        compuesto.getId());

    List<ProductoCompuestoSlotDto> slotDtos = slots.stream()
        .map(this::convertirSlotADto) // ✅ USA EL NUEVO MÉTODO
        .collect(Collectors.toList());

    dto.setSlots(slotDtos);

    return dto;
  }

  // En ProductoCompuestoServiceImpl.java, agregar estos métodos:

  @Override
  @Transactional(readOnly = true)
  public CalculoPrecioResponse calcularPrecio(CalcularPrecioCompuestoRequest request) {
    log.info("Calculando precio para producto {} con {} selecciones de slots",
        request.getProductoId(), request.getSelecciones().size());

    // 1. Obtener producto y validar
    Producto producto = productoRepository.findById(request.getProductoId())
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto no es de tipo COMPUESTO");
    }

    // 2. Validar sucursal
    sucursalRepository.findById(request.getSucursalId())
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    // 3. Precio base
    BigDecimal precioBase = producto.getPrecioBase() != null ?
        producto.getPrecioBase() : producto.getPrecioVenta();

    BigDecimal totalAdicionales = BigDecimal.ZERO;
    List<CalculoPrecioResponse.DetalleOpcion> detalles = new ArrayList<>();

    // 4. Procesar cada slot seleccionado
    for (SlotSeleccionDTO seleccion : request.getSelecciones()) {

      // Obtener slot
      ProductoCompuestoSlot slot = slotRepository.findById(seleccion.getSlotId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Slot no encontrado: " + seleccion.getSlotId()));

      // Validar selección (cantidad total, min/max, etc)
      slotSeleccionValidator.validarSeleccion(slot, seleccion);

      // Procesar cada opción del slot
      for (SlotSeleccionDTO.OpcionSeleccionada opcionSel : seleccion.getOpciones()) {

        ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionSel.getOpcionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Opción no encontrada: " + opcionSel.getOpcionId()));

        // Verificar disponibilidad en sucursal
        boolean disponibleEnSucursal = verificarDisponibilidadEnSucursal(
            opcion, request.getSucursalId());

        // Precio unitario de la opción
        BigDecimal precioUnitario = opcion.getPrecioAdicional() != null ?
            opcion.getPrecioAdicional() : BigDecimal.ZERO;

        // Cantidad (si el slot permite cantidad por opción, usar la cantidad; si no, siempre es 1)
        Integer cantidad = Boolean.TRUE.equals(slot.getPermiteCantidadPorOpcion()) ?
            opcionSel.getCantidad() : 1;

        // Subtotal = precio unitario × cantidad
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        totalAdicionales = totalAdicionales.add(subtotal);

        // Agregar detalle
        detalles.add(CalculoPrecioResponse.DetalleOpcion.builder()
            .opcionId(opcionSel.getOpcionId())
            .productoNombre(opcion.getNombreEfectivo())
            .slotNombre(slot.getNombre())
            .cantidad(cantidad)
            .precioUnitario(precioUnitario)
            .precioAdicional(subtotal) // Subtotal de esta opción
            .disponibleEnSucursal(disponibleEnSucursal)
            .build());
      }
    }

    BigDecimal precioFinal = precioBase.add(totalAdicionales);

    log.info("Precio calculado - Base: {}, Adicionales: {}, Final: {}",
        precioBase, totalAdicionales, precioFinal);

    return CalculoPrecioResponse.builder()
        .precioBase(precioBase)
        .totalAdicionales(totalAdicionales)
        .precioFinal(precioFinal)
        .detalleOpciones(detalles)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public ValidacionSeleccionResponse validarSeleccion(Long productoId, Long sucursalId,
      List<Long> opcionesSeleccionadas) {
    log.info("Validando selección para producto {} en sucursal {}", productoId, sucursalId);

    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

    List<ValidacionSeleccionResponse.ErrorValidacion> errores = new ArrayList<>();
    List<ValidacionSeleccionResponse.SlotValidacion> validacionesSlot = new ArrayList<>();
    boolean todasDisponibles = true;

    // Agrupar opciones seleccionadas por slot
    Map<Long, List<ProductoCompuestoOpcion>> opcionesPorSlot = new HashMap<>();
    for (Long opcionId : opcionesSeleccionadas) {
      ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId).orElse(null);
      if (opcion != null) {
        opcionesPorSlot.computeIfAbsent(opcion.getSlot().getId(), k -> new ArrayList<>())
            .add(opcion);
      }
    }

    // Validar cada slot
    List<ProductoCompuestoSlot> slots = slotRepository.findByCompuestoIdOrderByOrden(
        compuesto.getId());
    for (ProductoCompuestoSlot slot : slots) {
      List<ProductoCompuestoOpcion> opcionesEnSlot = opcionesPorSlot.getOrDefault(slot.getId(),
          new ArrayList<>());
      int cantidadSeleccionada = opcionesEnSlot.size();
      boolean cumpleRequisitos = true;

      // Validar cantidad mínima
      if (slot.getEsRequerido() && cantidadSeleccionada < slot.getCantidadMinima()) {
        errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
            .campo("slot_" + slot.getId())
            .mensaje(String.format("%s requiere mínimo %d opción(es)", slot.getNombre(),
                slot.getCantidadMinima()))
            .tipoError("FALTA_REQUERIDO")
            .build());
        cumpleRequisitos = false;
      }

      // Validar cantidad máxima
      if (cantidadSeleccionada > slot.getCantidadMaxima()) {
        errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
            .campo("slot_" + slot.getId())
            .mensaje(String.format("%s permite máximo %d opción(es)", slot.getNombre(),
                slot.getCantidadMaxima()))
            .tipoError("EXCEDE_MAXIMO")
            .build());
        cumpleRequisitos = false;
      }

      // Validar stock de cada opción
      List<ValidacionSeleccionResponse.OpcionValidada> opcionesValidadas = new ArrayList<>();
      for (ProductoCompuestoOpcion opcion : opcionesEnSlot) {
        boolean tieneStock = verificarStockOpcion(opcion, sucursalId);
        if (!tieneStock) {
          todasDisponibles = false;
          errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
              .campo("opcion_" + opcion.getId())
              .mensaje(
                  String.format("%s no tiene stock suficiente", opcion.getProducto().getNombre()))
              .tipoError("SIN_STOCK")
              .build());
        }

        opcionesValidadas.add(ValidacionSeleccionResponse.OpcionValidada.builder()
            .opcionId(opcion.getId())
            .productoNombre(opcion.getProducto().getNombre())
            .tieneStockSuficiente(tieneStock)
            .mensajeStock(tieneStock ? "Disponible" : "Sin stock")
            .build());
      }

      validacionesSlot.add(ValidacionSeleccionResponse.SlotValidacion.builder()
          .slotId(slot.getId())
          .slotNombre(slot.getNombre())
          .esRequerido(slot.getEsRequerido())
          .cantidadMinima(slot.getCantidadMinima())
          .cantidadMaxima(slot.getCantidadMaxima())
          .cantidadSeleccionada(cantidadSeleccionada)
          .cumpleRequisitos(cumpleRequisitos)
          .opcionesSeleccionadas(opcionesValidadas)
          .build());
    }

    return ValidacionSeleccionResponse.builder()
        .esValida(errores.isEmpty())
        .todasDisponiblesEnSucursal(todasDisponibles)
        .errores(errores)
        .validacionPorSlot(validacionesSlot)
        .build();
  }

  // Agregar estos métodos privados en ProductoCompuestoServiceImpl:

  private boolean verificarDisponibilidadEnSucursal(ProductoCompuestoOpcion opcion,
      Long sucursalId) {
    // Primero verificar si la opción está marcada como disponible globalmente
    if (!opcion.getDisponible()) {
      return false;
    }

    Producto producto = opcion.getProducto();

    // Si el producto no maneja inventario, siempre está disponible
    if (producto.getTipoInventario() == TipoInventario.NINGUNO) {
      return true;
    }

    // Verificar si la sucursal maneja inventario
    Sucursal sucursal = sucursalRepository.findById(sucursalId)
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    if (!sucursal.getManejaInventario()) {
      // Si la sucursal no maneja inventario, todo está disponible
      return true;
    }

    // Para productos MATERIA_PRIMA o MIXTO, verificar existencia en inventario
    if (producto.getTipo() == TipoProducto.MATERIA_PRIMA ||
        producto.getTipo() == TipoProducto.MIXTO ||
        producto.getTipo() == TipoProducto.VENTA) {

      try {
        ProductoInventario inventario = productoInventarioService
            .obtenerInventario(producto.getId(), sucursalId);

        // Si no hay registro de inventario, no está disponible
        if (inventario == null) {
          return false;
        }

        // Verificar que haya al menos algo de stock
        BigDecimal disponible = inventario.getCantidadActual()
            .subtract(inventario.getCantidadBloqueada());

        return disponible.compareTo(BigDecimal.ZERO) > 0;

      } catch (Exception e) {
        log.warn("Error verificando disponibilidad para producto {} en sucursal {}: {}",
            producto.getId(), sucursalId, e.getMessage());
        return false;
      }
    }

    return true;
  }

  private boolean verificarStockOpcion(ProductoCompuestoOpcion opcion, Long sucursalId) {
    Producto producto = opcion.getProducto();

    // Si no maneja inventario, siempre tiene "stock"
    if (producto.getTipoInventario() == TipoInventario.NINGUNO) {
      return true;
    }

    // Verificar configuración de sucursal
    Sucursal sucursal = sucursalRepository.findById(sucursalId)
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    // Si la sucursal no maneja inventario o permite negativos
    if (!sucursal.getManejaInventario() || sucursal.getPermiteNegativos()) {
      return true;
    }

    try {
      // Para productos con inventario SIMPLE
      if (producto.getTipoInventario() == TipoInventario.SIMPLE) {
        BigDecimal stockDisponible = obtenerCantidadDisponible(producto.getId(), sucursalId);

        // Asumimos que necesitamos al menos 1 unidad
        // Aquí podrías ajustar según la cantidad típica usada en el compuesto
        return stockDisponible.compareTo(BigDecimal.ONE) >= 0;
      }

      // Para productos con RECETA, verificar si se puede producir
      if (producto.getTipoInventario() == TipoInventario.RECETA) {
        return inventarioService.puedeProducir(
            producto.getEmpresa().getId(),
            producto.getId(),
            sucursalId,
            BigDecimal.ONE
        );
      }

    } catch (Exception e) {
      log.warn("Error verificando stock para opción {}: {}", opcion.getId(), e.getMessage());
      return false;
    }

    return true;
  }

  // Método helper adicional que podrías necesitar en ProductoInventarioService
  public BigDecimal obtenerCantidadDisponible(Long productoId, Long sucursalId) {
    ProductoInventario inventario = productoInventarioService
        .obtenerInventario(productoId, sucursalId);

    if (inventario == null) {
      return BigDecimal.ZERO;
    }

    return inventario.getCantidadActual()
        .subtract(inventario.getCantidadBloqueada());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductoCompuestoDto> filtrarPorDisponibilidadSucursal(
      List<ProductoCompuestoDto> compuestos, Long sucursalId) {
    log.info("Filtrando {} compuestos por disponibilidad en sucursal {}", compuestos.size(),
        sucursalId);

    List<ProductoCompuestoDto> compuestosFiltrados = new ArrayList<>();

    for (ProductoCompuestoDto compuesto : compuestos) {
      ProductoCompuestoDto compuestoFiltrado = new ProductoCompuestoDto();
      // Copiar datos básicos
      BeanUtils.copyProperties(compuesto, compuestoFiltrado);
      compuestoFiltrado.setSlots(new ArrayList<>());

      boolean tieneOpcionesDisponibles = false;

      // Filtrar slots y opciones
      for (ProductoCompuestoSlotDto slot : compuesto.getSlots()) {
        ProductoCompuestoSlotDto slotFiltrado = new ProductoCompuestoSlotDto();
        BeanUtils.copyProperties(slot, slotFiltrado);
        slotFiltrado.setOpciones(new ArrayList<>());

        // Filtrar solo opciones disponibles en la sucursal
        for (ProductoCompuestoOpcionDto opcion : slot.getOpciones()) {
          if (opcion.getDisponible()) {
            ProductoCompuestoOpcion opcionEntity = opcionRepository.findById(opcion.getId())
                .orElse(null);

            if (opcionEntity != null && verificarDisponibilidadEnSucursal(opcionEntity,
                sucursalId)) {
              slotFiltrado.getOpciones().add(opcion);
              tieneOpcionesDisponibles = true;
            }
          }
        }

        // Solo agregar el slot si tiene opciones disponibles o es opcional
        if (!slotFiltrado.getOpciones().isEmpty() || !slot.getEsRequerido()) {
          compuestoFiltrado.getSlots().add(slotFiltrado);
        }
      }

      // Solo incluir el compuesto si tiene opciones disponibles
      if (tieneOpcionesDisponibles) {
        compuestosFiltrados.add(compuestoFiltrado);
      }
    }

    log.info("Filtrados {} compuestos con disponibilidad", compuestosFiltrados.size());
    return compuestosFiltrados;
  }

  /**
   * Valida la configuración de un slot con familia
   *
   * Reglas:
   * 1. Si usaFamilia = true → familia NO puede ser null
   * 2. Si usaFamilia = false → debe tener opciones manuales
   * 3. No puede tener ambos al mismo tiempo
   * 4. cantidadMaxima >= cantidadMinima
   * 5. Si es requerido → cantidadMinima >= 1
   */
  private void validarSlotConFamilia(ProductoCompuestoRequest.SlotRequest slotRequest, Long empresaId) {
    Boolean usaFamilia = slotRequest.getUsaFamilia();

    // Si no especifica, asumimos opciones manuales (false)
    if (usaFamilia == null) {
      usaFamilia = false;
    }

    // Validación 1: Si usa familia, entonces familiaId NO puede ser null
    if (Boolean.TRUE.equals(usaFamilia)) {
      if (slotRequest.getFamiliaId() == null) {
        throw new BusinessException(
            "Si el slot '" + slotRequest.getNombre() + "' usa familia, debe especificar familiaId"
        );
      }

      // Validar que la familia existe y pertenece a la empresa
      FamiliaProducto familia = familiaProductoRepository.findById(slotRequest.getFamiliaId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Familia no encontrada: " + slotRequest.getFamiliaId()
          ));

      if (!familia.getEmpresa().getId().equals(empresaId)) {
        throw new BusinessException(
            "La familia no pertenece a la empresa"
        );
      }

      if (!familia.getActiva()) {
        throw new BusinessException(
            "No se puede usar una familia inactiva en el slot '" + slotRequest.getNombre() + "'"
        );
      }

      // Si usa familia, NO debe tener opciones manuales
      if (slotRequest.getOpciones() != null && !slotRequest.getOpciones().isEmpty()) {
        throw new BusinessException(
            "El slot '" + slotRequest.getNombre() +
                "' no puede tener opciones manuales y familia al mismo tiempo"
        );
      }
    } else {
      // Validación 2: Si NO usa familia, debe tener opciones manuales
      if (slotRequest.getOpciones() == null || slotRequest.getOpciones().isEmpty()) {
        throw new BusinessException(
            "El slot '" + slotRequest.getNombre() +
                "' debe tener opciones manuales si no usa familia"
        );
      }

      // Si no usa familia, NO debe tener familiaId
      if (slotRequest.getFamiliaId() != null) {
        throw new BusinessException(
            "El slot '" + slotRequest.getNombre() +
                "' no puede tener familiaId si no usa familia"
        );
      }
    }

    // Validación 3: cantidadMaxima >= cantidadMinima
    if (slotRequest.getCantidadMaxima() < slotRequest.getCantidadMinima()) {
      throw new BusinessException(
          "La cantidad máxima no puede ser menor que la cantidad mínima en el slot '" +
              slotRequest.getNombre() + "'"
      );
    }

    // Validación 4: Si es requerido, cantidadMinima debe ser >= 1
    if (Boolean.TRUE.equals(slotRequest.getEsRequerido()) && slotRequest.getCantidadMinima() < 1) {
      throw new BusinessException(
          "Si el slot '" + slotRequest.getNombre() +
              "' es requerido, la cantidad mínima debe ser al menos 1"
      );
    }
  }

  /**
   * Convierte ProductoCompuestoSlot a DTO (incluyendo datos de familia)
   */
  private ProductoCompuestoSlotDto convertirSlotADto(ProductoCompuestoSlot slot) {
    ProductoCompuestoSlotDto dto = ProductoCompuestoSlotDto.builder()
        .id(slot.getId())
        .nombre(slot.getNombre())
        .descripcion(slot.getDescripcion())
        .cantidadMinima(slot.getCantidadMinima())
        .cantidadMaxima(slot.getCantidadMaxima())
        .esRequerido(slot.getEsRequerido())
        .orden(slot.getOrden())
        .usaFamilia(slot.getUsaFamilia())
        .build();

    // Si usa familia, incluir datos de la familia
    if (Boolean.TRUE.equals(slot.getUsaFamilia()) && slot.getFamilia() != null) {
      dto.setFamiliaId(slot.getFamilia().getId());
      dto.setFamiliaNombre(slot.getFamilia().getNombre());
      dto.setFamiliaCodigo(slot.getFamilia().getCodigo());
      dto.setPrecioAdicionalPorOpcion(slot.getPrecioAdicionalPorOpcion());

      // NO cargar opciones aquí - se cargarán dinámicamente en otro endpoint
      dto.setOpciones(List.of());
    } else {
      // Si usa opciones manuales, convertir opciones
      List<ProductoCompuestoOpcionDto> opcionesDto = slot.getOpciones().stream()
          .map(this::convertirOpcionADto)
          .toList();

      dto.setOpciones(opcionesDto);
    }

    return dto;
  }

  /**
   * Convierte ProductoCompuestoOpcion a DTO
   */
  private ProductoCompuestoOpcionDto convertirOpcionADto(ProductoCompuestoOpcion opcion) {
    ProductoCompuestoOpcionDto dto = ProductoCompuestoOpcionDto.builder()
        .id(opcion.getId())
        .productoId(opcion.getProducto().getId())
        .productoNombre(opcion.getProducto().getNombre())
        .productoCodigo(opcion.getProducto().getCodigoInterno())
        .precioAdicional(opcion.getPrecioAdicional() != null ? opcion.getPrecioAdicional() : BigDecimal.ZERO)
        .esDefault(opcion.getEsDefault())
        .disponible(opcion.getDisponible())
        .orden(opcion.getOrden())
        .build();

    return dto;
  }

  /**
   * Crea un slot con soporte de familias dinámicas
   */
  private ProductoCompuestoSlot crearSlotConFamilia(
      ProductoCompuesto compuesto,
      ProductoCompuestoRequest.SlotRequest slotRequest,
      Long empresaId,
      int ordenSlot
  ) {
    // Validar configuración del slot
    validarSlotConFamilia(slotRequest, empresaId);

    // Crear entidad slot
    ProductoCompuestoSlot slot = ProductoCompuestoSlot.builder()
        .compuesto(compuesto)
        .nombre(slotRequest.getNombre())
        .descripcion(slotRequest.getDescripcion())
        .cantidadMinima(slotRequest.getCantidadMinima())
        .cantidadMaxima(slotRequest.getCantidadMaxima())
        .esRequerido(slotRequest.getEsRequerido())
        .orden(slotRequest.getOrden() != null ? slotRequest.getOrden() : ordenSlot)
        .usaFamilia(slotRequest.getUsaFamilia() != null ? slotRequest.getUsaFamilia() : false)
        .build();

    // Si usa familia, configurar familia y precio adicional
    if (Boolean.TRUE.equals(slot.getUsaFamilia())) {
      FamiliaProducto familia = familiaProductoRepository.findById(slotRequest.getFamiliaId())
          .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));

      slot.setFamilia(familia);
      slot.setPrecioAdicionalPorOpcion(slotRequest.getPrecioAdicionalPorOpcion());

      log.debug("Slot '{}' configurado con familia: {} (precio adicional: {})",
          slot.getNombre(),
          familia.getNombre(),
          slotRequest.getPrecioAdicionalPorOpcion());
    }

    // Guardar slot
    slot = slotRepository.save(slot);

    // Si usa opciones manuales, crearlas
    if (Boolean.FALSE.equals(slot.getUsaFamilia()) && slotRequest.getOpciones() != null) {
      int ordenOpcion = 0;
      for (ProductoCompuestoRequest.OpcionRequest opcionRequest : slotRequest.getOpciones()) {
        agregarOpcionASlot(slot, opcionRequest, empresaId, ordenOpcion++);
      }
      log.debug("Slot '{}' creado con {} opciones manuales",
          slot.getNombre(),
          slotRequest.getOpciones().size());
    }

    return slot;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OpcionSlotDTO> obtenerOpcionesSlot(Long slotId, Long sucursalId) {
    log.info("Obteniendo opciones para slot {} en sucursal {}", slotId, sucursalId);

    // 1. Obtener el slot
    ProductoCompuestoSlot slot = slotRepository.findById(slotId)
        .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

    // 2. Validar sucursal
    Sucursal sucursal = sucursalRepository.findById(sucursalId)
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    List<OpcionSlotDTO> opciones;

    // 3. Decidir origen de opciones
    if (Boolean.TRUE.equals(slot.getUsaFamilia()) && slot.getFamilia() != null) {
      // ========== CARGAR DESDE FAMILIA ==========
      log.info("Cargando opciones desde familia: {}", slot.getFamilia().getNombre());
      opciones = cargarOpcionesDesdeFamilia(slot, sucursal);
    } else {
      // ========== CARGAR OPCIONES MANUALES ==========
      log.info("Cargando opciones manuales del slot");
      opciones = cargarOpcionesManuales(slot, sucursal);
    }

    // 4. Ordenar por disponibilidad y luego por orden
    opciones.sort((a, b) -> {
      // Primero las disponibles
      if (!a.getDisponible().equals(b.getDisponible())) {
        return a.getDisponible() ? -1 : 1;
      }
      // Luego por orden
      return Integer.compare(a.getOrden(), b.getOrden());
    });

    log.info("Se encontraron {} opciones para el slot", opciones.size());
    return opciones;
  }

  /**
   * Carga opciones desde una familia de productos
   */
  private List<OpcionSlotDTO> cargarOpcionesDesdeFamilia(
      ProductoCompuestoSlot slot,
      Sucursal sucursal
  ) {
    List<OpcionSlotDTO> opciones = new ArrayList<>();

    // Obtener todos los productos activos de la familia
    List<Producto> productos = productoRepository.findByFamiliaIdAndActivoTrue(
        slot.getFamilia().getId()
    );

    log.info("Familia {} tiene {} productos activos",
        slot.getFamilia().getNombre(), productos.size());

    int orden = 0;
    for (Producto producto : productos) {
      // Verificar stock en sucursal
      boolean tieneStock = verificarStockProducto(producto.getId(), sucursal.getId());
      Integer stockDisponible = obtenerStockDisponible(producto.getId(), sucursal.getId());

      // Calcular precio adicional
      BigDecimal precioAdicional = slot.getPrecioAdicionalPorOpcion() != null
          ? slot.getPrecioAdicionalPorOpcion()
          : BigDecimal.ZERO;

      boolean esGratuita = precioAdicional.compareTo(BigDecimal.ZERO) == 0;

      // Crear DTO
      OpcionSlotDTO opcion = OpcionSlotDTO.builder()
          .opcionId(null)  // No hay opción manual
          .productoId(producto.getId())
          .nombre(producto.getNombre())
          .codigoInterno(producto.getCodigoInterno())
          .imagen(producto.getImagenUrl())
          .precioBase(producto.getPrecioBase() != null ? producto.getPrecioBase() : BigDecimal.ZERO)
          .precioAdicional(precioAdicional)
          .esGratuita(esGratuita)
          .disponible(tieneStock)
          .esDefault(false)  // Las opciones de familia no tienen default
          .stockDisponible(stockDisponible)
          .orden(orden++)
          .origen("FAMILIA")
          .build();

      opciones.add(opcion);

      log.debug("Producto {} - Stock: {}, Disponible: {}",
          producto.getNombre(), stockDisponible, tieneStock);
    }

    return opciones;
  }

  /**
   * Carga opciones manuales del slot
   */
  private List<OpcionSlotDTO> cargarOpcionesManuales(
      ProductoCompuestoSlot slot,
      Sucursal sucursal
  ) {
    List<OpcionSlotDTO> opciones = new ArrayList<>();

    // ✅ Trae opciones + producto inicializados (sin lazy)
    List<ProductoCompuestoOpcion> opcionesManuales =
        opcionRepository.findBySlotIdOrderByOrdenWithProducto(slot.getId());

    log.info("Slot tiene {} opciones manuales", opcionesManuales.size());

    for (ProductoCompuestoOpcion opcionManual : opcionesManuales) {
      Producto producto = opcionManual.getProducto(); // ya inicializado

      // Verificar stock en sucursal
      boolean tieneStock = verificarStockProducto(producto.getId(), sucursal.getId());
      Integer stockDisponible = obtenerStockDisponible(producto.getId(), sucursal.getId());

      boolean disponibleFinal = Boolean.TRUE.equals(opcionManual.getDisponible()) && tieneStock;
      BigDecimal precioAdicional = opcionManual.getPrecioAdicional() != null
          ? opcionManual.getPrecioAdicional()
          : BigDecimal.ZERO;

      boolean esGratuita = precioAdicional.compareTo(BigDecimal.ZERO) == 0;

      OpcionSlotDTO opcion = OpcionSlotDTO.builder()
          .opcionId(opcionManual.getId())
          .productoId(producto.getId())
          .nombre(producto.getNombre())
          .codigoInterno(producto.getCodigoInterno())
          .imagen(producto.getImagenUrl())
          .precioBase(producto.getPrecioBase() != null ? producto.getPrecioBase() : BigDecimal.ZERO)
          .precioAdicional(precioAdicional)
          .esGratuita(esGratuita)
          .disponible(disponibleFinal)
          .esDefault(Boolean.TRUE.equals(opcionManual.getEsDefault()))
          .stockDisponible(stockDisponible)
          .orden(opcionManual.getOrden())
          .origen("MANUAL")
          .build();

      opciones.add(opcion);

      log.debug("Opción manual {} - Stock: {}, Disponible config: {}, Final: {}",
          producto.getNombre(), stockDisponible, opcionManual.getDisponible(), disponibleFinal);
    }

    return opciones;
  }

  /**
   * Verifica si un producto tiene stock disponible en sucursal
   */
  private boolean verificarStockProducto(Long productoId, Long sucursalId) {
    try {
      Integer stock = obtenerStockDisponible(productoId, sucursalId);
      return stock != null && stock > 0;
    } catch (Exception e) {
      log.warn("Error verificando stock para producto {}: {}", productoId, e.getMessage());
      return true;  // En caso de error, marcar como no disponible
    }
  }
  /**
   * Obtiene la cantidad de stock disponible para un producto en una sucursal
   * Stock disponible = cantidad actual - cantidad bloqueada
   */
  private Integer obtenerStockDisponible(Long productoId, Long sucursalId) {
    try {
      // Llamar al servicio de inventario para obtener el inventario
      ProductoInventario inventario = productoInventarioService.obtenerInventario(productoId, sucursalId);

      if (inventario == null) {
        log.debug("No existe inventario para producto {} en sucursal {}", productoId, sucursalId);
        return 0;
      }

      // Calcular stock disponible (actual - bloqueado)
      BigDecimal cantidadActual = inventario.getCantidadActual() != null
          ? inventario.getCantidadActual()
          : BigDecimal.ZERO;

      BigDecimal cantidadBloqueada = inventario.getCantidadBloqueada() != null
          ? inventario.getCantidadBloqueada()
          : BigDecimal.ZERO;

      BigDecimal stockDisponible = cantidadActual.subtract(cantidadBloqueada);

      // Convertir a Integer (redondeando hacia abajo)
      return stockDisponible.intValue();

    } catch (ResourceNotFoundException e) {
      log.debug("No existe inventario para producto {} en sucursal {}", productoId, sucursalId);
      return 0;
    } catch (Exception e) {
      log.warn("Error obteniendo stock disponible para producto {}: {}", productoId, e.getMessage());
      return 0;
    }
  }

  // Agregar a ProductoCompuestoServiceImpl.java

  @Override
  @Transactional(readOnly = true)
  public ProductoCompuestoConfiguracionDTO obtenerConfiguracionPorOpcion(Long productoId, Long opcionId) {
    log.info("Obteniendo configuración para producto {} con opción {}", productoId, opcionId);

    // 1. Validar que el producto existe
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    // 2. Validar que es tipo COMPUESTO
    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // 3. Obtener el compuesto
    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

    // 4. Buscar configuración por opcionTriggerId
    ProductoCompuestoConfiguracion configuracion = configuracionRepository.findByOpcionTriggerId(opcionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No existe configuración para la opción seleccionada. " +
                "Debe configurar qué slots mostrar cuando se elige esta opción."
        ));

    // 5. Validar que la configuración pertenece a este compuesto
    if (!configuracion.getCompuesto().getId().equals(compuesto.getId())) {
      throw new BusinessException("La configuración no pertenece a este producto compuesto");
    }

    // 6. Validar que la configuración está activa
    if (!configuracion.getActiva()) {
      throw new BusinessException("La configuración está inactiva");
    }

    log.info("Configuración encontrada: {} con {} slots",
        configuracion.getNombre(),
        configuracion.getSlots().size());

    // 7. Convertir a DTO
    return convertirConfiguracionADto(configuracion, producto.getSucursal().getId());
  }

  @Override
  public ConfiguracionFlujoDTO obtenerFlujoConfiguracion(Long productoId, Long sucursalId) {
    log.info("Obteniendo flujo de configuración para producto {} en sucursal {}",
        productoId, sucursalId);

    // 1. Validar que el producto existe y es COMPUESTO
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // 2. Obtener el ProductoCompuesto
    ProductoCompuesto compuesto = compuestoRepository
        .findByProductoId(productoId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Configuración de producto compuesto no encontrada"
        ));

    // 3. Construir DTO base
    ConfiguracionFlujoDTO flujo = ConfiguracionFlujoDTO.builder()
        .productoId(producto.getId())
        .productoNombre(producto.getNombre())
        .precioBase(producto.getPrecioBase())
        .build();

    // 4. DECIDIR EL FLUJO
    if (compuesto.getSlotPreguntaInicial() != null) {
      log.info("Producto tiene pregunta inicial");
      flujo.setTienePreguntaInicial(true);
      flujo.setSlotPreguntaInicial(
          construirSlotPreguntaInicial(compuesto.getSlotPreguntaInicial(), sucursalId)
      );
      flujo.setConfiguracionDefault(null);

    } else {
      log.info("Producto NO tiene pregunta inicial, buscando configuración default");
      flujo.setTienePreguntaInicial(false);
      flujo.setSlotPreguntaInicial(null);

      ProductoCompuestoConfiguracion configDefault = configuracionRepository
          .findByCompuestoIdAndEsDefaultTrueWithSlots(compuesto.getId())  // ⭐ Usar este
          .orElseThrow(() -> new BusinessException(
              "El producto no tiene pregunta inicial ni configuración default configurada"
          ));

      // ⭐ CONVERTIR CON MANEJO DE ERRORES
      ProductoCompuestoConfiguracionDTO configDto =
          convertirConfiguracionADtoSafe(configDefault, sucursalId);
      flujo.setConfiguracionDefault(configDto);
    }

    log.info("Flujo de configuración generado: tienePreguntaInicial={}",
        flujo.getTienePreguntaInicial());

    return flujo;
  }

  /**
   * Versión SAFE de convertir configuración que maneja errores internamente
   */
  private ProductoCompuestoConfiguracionDTO convertirConfiguracionADtoSafe(
      ProductoCompuestoConfiguracion configuracion,
      Long sucursalId) {

    try {
      return convertirConfiguracionADto(configuracion, sucursalId);
    } catch (Exception e) {
      log.error("Error convirtiendo configuración a DTO: {}", e.getMessage(), e);

      // Crear DTO básico sin slots si falla

      return ProductoCompuestoConfiguracionDTO.builder()
          .id(configuracion.getId())
          .compuestoId(configuracion.getCompuesto().getId())
          .nombre(configuracion.getNombre())
          .descripcion(configuracion.getDescripcion())
          .orden(configuracion.getOrden())
          .activa(configuracion.getActiva())
          .opcionTriggerId(null)
          .slots(new ArrayList<>())
          .build();
    }
  }

  @Override
  public ProductoCompuestoConfiguracionDTO obtenerConfiguracionPorOpcion(
      Long productoId,
      Long opcionId,
      Long sucursalId
  ) {
    log.info("Obteniendo configuración por opción {} del producto {} en sucursal {}",
        opcionId, productoId, sucursalId);

    // 1. Validar que el producto existe y es COMPUESTO
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // 2. Validar que la opción existe
    ProductoCompuestoOpcion opcion = productoCompuestoOpcionRepository.findById(opcionId)
        .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));

    // 3. Buscar la configuración que se activa con esta opción
    ProductoCompuestoConfiguracion configuracion = configuracionRepository
        .findByOpcionTriggerIdWithSlots(opcionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No existe configuración para la opción seleccionada"
        ));

    // 4. Validar que la configuración pertenece al producto correcto
    if (!configuracion.getCompuesto().getProducto().getId().equals(productoId)) {
      throw new BusinessException(
          "La configuración no pertenece al producto especificado"
      );
    }

    // 5. Validar que la configuración está activa
    if (!configuracion.getActiva()) {
      throw new BusinessException(
          "La configuración '" + configuracion.getNombre() + "' está inactiva"
      );
    }

    log.info("Configuración encontrada: {} con {} slots",
        configuracion.getNombre(),
        configuracion.getSlots().size());

    // 6. Convertir a DTO con todas las opciones cargadas
    return convertirConfiguracionADto(configuracion, sucursalId);
  }

  /**
   * Construye el DTO del slot de pregunta inicial con sus opciones
   */
  private SlotPreguntaInicialDTO construirSlotPreguntaInicial(
      ProductoCompuestoSlot slot,
      Long sucursalId
  ) {
    log.debug("Construyendo slot pregunta inicial: {}", slot.getNombre());

    // Obtener opciones del slot ordenadas
    List<OpcionPreguntaInicialDTO> opciones = slot.getOpciones().stream()
        .sorted(Comparator.comparingInt(ProductoCompuestoOpcion::getOrden))
        .map(opcion -> {
          // Buscar la configuración que activa esta opción
          Long configuracionId = configuracionRepository
              .findByOpcionTriggerId(opcion.getId())
              .map(ProductoCompuestoConfiguracion::getId)
              .orElse(null);

          if (configuracionId == null) {
            log.warn("Opción {} no tiene configuración asociada", opcion.getId());
          }

          return OpcionPreguntaInicialDTO.builder()
              .id(opcion.getId())
              .nombre(opcion.getNombreEfectivo())
              .descripcion(slot.getDescripcion())
              .precioAdicional(opcion.getPrecioAdicional())
              .configuracionId(configuracionId)
              .esDefault(opcion.getEsDefault())
              .orden(opcion.getOrden())
              .build();
        })
        .collect(Collectors.toList());

    return SlotPreguntaInicialDTO.builder()
        .slotId(slot.getId())
        .nombre(slot.getNombre())
        .pregunta(slot.getDescripcion() != null ?
            slot.getDescripcion() : "¿Cómo deseas tu " + slot.getNombre() + "?")
        .descripcion(slot.getDescripcion())
        .opciones(opciones)
        .build();
  }

  /**
   * Convierte ProductoCompuestoConfiguracion a DTO
   * Carga las opciones dinámicamente para cada slot
   */
  private ProductoCompuestoConfiguracionDTO convertirConfiguracionADto(
      ProductoCompuestoConfiguracion configuracion,
      Long sucursalId) {

    ProductoCompuestoOpcion opcionTrigger = configuracion.getOpcionTrigger();

    ProductoCompuestoConfiguracionDTO dto = ProductoCompuestoConfiguracionDTO.builder()
        .id(configuracion.getId())
        .compuestoId(configuracion.getCompuesto().getId())
        .nombre(configuracion.getNombre())
        .descripcion(configuracion.getDescripcion())
        .orden(configuracion.getOrden())
        .activa(configuracion.getActiva())
        .opcionTriggerId(opcionTrigger != null ? opcionTrigger.getId() : null)
        .opcionTriggerProductoId(opcionTrigger != null && opcionTrigger.getProducto() != null ? opcionTrigger.getProducto().getId() : null)
        .opcionTriggerProductoNombre(opcionTrigger != null && opcionTrigger.getProducto() != null ? opcionTrigger.getProducto().getNombre() : null)
        .createdAt(configuracion.getCreatedAt())
        .updatedAt(configuracion.getUpdatedAt())
        .build();

    // Cargar slots con sus opciones
    List<SlotConfiguracionDTO> slotsDto = configuracion.getSlots().stream()
        .sorted(Comparator.comparingInt(ProductoCompuestoSlotConfiguracion::getOrden))
        .map(slotConfig -> convertirSlotConfigADto(slotConfig, sucursalId))
        .collect(Collectors.toList());

    dto.setSlots(slotsDto);

    log.debug("Configuración DTO creado con {} slots", slotsDto.size());

    return dto;
  }

  /**
   * Convierte ProductoCompuestoSlotConfiguracion a DTO
   * Incluye valores originales y overrides
   */
  private SlotConfiguracionDTO convertirSlotConfigADto(
      ProductoCompuestoSlotConfiguracion slotConfig,
      Long sucursalId) {

    ProductoCompuestoSlot slot = slotConfig.getSlot();

    // ========== CALCULAR VALORES EFECTIVOS (originales + overrides) ==========

    Integer cantidadMinimaEfectiva = slotConfig.getCantidadMinimaOverride() != null
        ? slotConfig.getCantidadMinimaOverride()
        : slot.getCantidadMinima();

    Integer cantidadMaximaEfectiva = slotConfig.getCantidadMaximaOverride() != null
        ? slotConfig.getCantidadMaximaOverride()
        : slot.getCantidadMaxima();

    Boolean esRequeridoEfectivo = slotConfig.getEsRequeridoOverride() != null
        ? slotConfig.getEsRequeridoOverride()
        : slot.getEsRequerido();

    BigDecimal precioAdicionalEfectivo = slotConfig.getPrecioAdicionalOverride() != null
        ? slotConfig.getPrecioAdicionalOverride()
        : (slot.getUsaFamilia() ? slot.getPrecioAdicionalPorOpcion() : null);

    // ========== CONSTRUIR DTO BASE ==========

    SlotConfiguracionDTO dto = SlotConfiguracionDTO.builder()
        .id(slotConfig.getId())
        .slotId(slot.getId())
        .slotNombre(slot.getNombre())
        .slotDescripcion(slot.getDescripcion())
        .orden(slotConfig.getOrden())

        // Valores originales del slot
        .cantidadMinimaOriginal(slot.getCantidadMinima())
        .cantidadMaximaOriginal(slot.getCantidadMaxima())
        .esRequeridoOriginal(slot.getEsRequerido())
        .precioAdicionalOriginal(
            slot.getUsaFamilia() ? slot.getPrecioAdicionalPorOpcion() : null
        )

        .permiteCantidadPorOpcion(slot.getPermiteCantidadPorOpcion())
        .maxOpcionesDiferentes(slot.getMaxOpcionesDiferentes())
        // Overrides de esta configuración
        .cantidadMinimaOverride(slotConfig.getCantidadMinimaOverride())
        .cantidadMaximaOverride(slotConfig.getCantidadMaximaOverride())
        .esRequeridoOverride(slotConfig.getEsRequeridoOverride())
        .precioAdicionalOverride(slotConfig.getPrecioAdicionalOverride())

        // ⭐ VALORES EFECTIVOS (los que se deben usar)
        .cantidadMinima(cantidadMinimaEfectiva)
        .cantidadMaxima(cantidadMaximaEfectiva)
        .esRequerido(esRequeridoEfectivo)
        .precioAdicional(precioAdicionalEfectivo)

        // Info de familia
        .usaFamilia(slot.getUsaFamilia())
        .familiaId(slot.getFamilia() != null ? slot.getFamilia().getId() : null)
        .familiaNombre(slot.getFamilia() != null ? slot.getFamilia().getNombre() : null)
        .build();

    // ⭐ CARGAR OPCIONES CON MANEJO DE ERRORES
    try {
      Sucursal sucursal = sucursalRepository.findById(sucursalId)
          .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

      List<OpcionSlotDTO> opciones;

      if (Boolean.TRUE.equals(slot.getUsaFamilia()) && slot.getFamilia() != null) {
        opciones = cargarOpcionesDesdeFamiliaConPrecio(
            slot.getFamilia(),
            sucursal,
            precioAdicionalEfectivo != null ? precioAdicionalEfectivo : BigDecimal.ZERO
        );
      } else {
        opciones = cargarOpcionesManualesSafe(slot, sucursal); // ⭐ Usar versión SAFE
      }

      dto.setOpciones(opciones);
      log.debug("Slot '{}' cargado con {} opciones", slot.getNombre(), opciones.size());

    } catch (Exception e) {
      log.error("Error cargando opciones para slot {}: {}", slot.getNombre(), e.getMessage());
      dto.setOpciones(new ArrayList<>()); // Lista vacía si falla
    }

    return dto;
  }

  /**
   * Versión SAFE que no lanza excepciones
   */
  private List<OpcionSlotDTO> cargarOpcionesManualesSafe(
      ProductoCompuestoSlot slot,
      Sucursal sucursal) {

    try {
      return cargarOpcionesManuales(slot, sucursal);
    } catch (Exception e) {
      log.error("Error en cargarOpcionesManuales: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * Carga opciones desde familia con precio específico
   */
  private List<OpcionSlotDTO> cargarOpcionesDesdeFamiliaConPrecio(
      FamiliaProducto familia,
      Sucursal sucursal,
      BigDecimal precioAdicional
  ) {
    List<OpcionSlotDTO> opciones = new ArrayList<>();

    // Obtener todos los productos activos de la familia
    List<Producto> productos = productoRepository.findByFamiliaIdAndActivoTrue(familia.getId());

    log.debug("Familia {} tiene {} productos activos", familia.getNombre(), productos.size());

    int orden = 0;
    for (Producto producto : productos) {
      // Verificar stock en sucursal
      boolean tieneStock = verificarStockProducto(producto.getId(), sucursal.getId());
      Integer stockDisponible = obtenerStockDisponible(producto.getId(), sucursal.getId());

      boolean esGratuita = precioAdicional.compareTo(BigDecimal.ZERO) == 0;

      // Crear DTO
      OpcionSlotDTO opcion = OpcionSlotDTO.builder()
          .opcionId(null)  // No hay opción manual en familia
          .productoId(producto.getId())
          .nombre(producto.getNombre())
          .codigoInterno(producto.getCodigoInterno())
          .imagen(producto.getImagenUrl())
          .precioBase(producto.getPrecioBase() != null ? producto.getPrecioBase() : BigDecimal.ZERO)
          .precioAdicional(precioAdicional)
          .esGratuita(esGratuita)
          .disponible(tieneStock)
          .esDefault(false)
          .stockDisponible(stockDisponible)
          .orden(orden++)
          .origen("FAMILIA")
          .build();

      opciones.add(opcion);
    }

    // Ordenar: disponibles primero, luego por nombre
    opciones.sort((a, b) -> {
      if (!a.getDisponible().equals(b.getDisponible())) {
        return a.getDisponible() ? -1 : 1;
      }
      return a.getNombre().compareTo(b.getNombre());
    });

    return opciones;
  }


  @Override
  @Transactional
  public ProductoCompuestoConfiguracionDTO crearConfiguracion(
      Long productoId,
      CrearConfiguracionRequest request) {

    log.info("Creando configuración '{}' para producto {}", request.getNombre(), productoId);

    // 1. Validar que el producto existe y es COMPUESTO
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // 2. Obtener el compuesto
    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "El producto no tiene configuración de compuesto. Debe crear la configuración base primero."
        ));

    // ⭐ 3. DECIDIR SI ES CONFIGURACIÓN DEFAULT O CONDICIONAL
    ProductoCompuestoOpcion opcionTrigger = null;
    boolean esDefault = false;

    if (request.getOpcionTriggerId() != null) {
      // ===== CONFIGURACIÓN CONDICIONAL (con trigger) =====

      opcionTrigger = opcionRepository.findById(request.getOpcionTriggerId())
          .orElseThrow(() -> new ResourceNotFoundException("La opción trigger no existe"));

      // Validar que la opción pertenece a un slot de este compuesto
      boolean opcionPerteneceAlCompuesto = compuesto.getSlots().stream()
          .anyMatch(slot -> slot.getOpciones().stream()
              .anyMatch(opcion -> opcion.getId().equals(request.getOpcionTriggerId()))
          );

      if (!opcionPerteneceAlCompuesto) {
        throw new BusinessException(
            "La opción trigger no pertenece a ningún slot de este producto compuesto"
        );
      }

      // Validar que opcionTrigger NO esté usada por otra configuración
      if (configuracionRepository.existsByOpcionTriggerId(request.getOpcionTriggerId())) {
        throw new BusinessException(
            "Ya existe una configuración para esta opción. " +
                "Una opción solo puede activar una configuración."
        );
      }

      log.info("Creando configuración CONDICIONAL con trigger: opcionId={}", request.getOpcionTriggerId());

    } else {
      // ===== CONFIGURACIÓN DEFAULT (sin trigger) =====

      // Validar que NO exista ya una configuración default
      Optional<ProductoCompuestoConfiguracion> configDefaultExistente =
          configuracionRepository.findByCompuestoIdAndEsDefaultTrue(compuesto.getId());

      if (configDefaultExistente.isPresent()) {
        throw new BusinessException(
            "Ya existe una configuración default para este producto. " +
                "Solo puede haber una configuración default por producto."
        );
      }

      esDefault = true;
      log.info("Creando configuración DEFAULT (sin trigger)");
    }

    // 4. Validar que tenga al menos 1 slot
    if (request.getSlots() == null || request.getSlots().isEmpty()) {
      throw new BusinessException("Debe incluir al menos un slot en la configuración");
    }

    // 5. Validar que todos los slots pertenecen al compuesto
    for (CrearConfiguracionRequest.SlotConfigRequest slotReq : request.getSlots()) {
      ProductoCompuestoSlot slot = slotRepository.findById(slotReq.getSlotId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Slot con ID " + slotReq.getSlotId() + " no encontrado"
          ));

      if (!slot.getCompuesto().getId().equals(compuesto.getId())) {
        throw new BusinessException(
            "El slot '" + slot.getNombre() + "' no pertenece a este producto compuesto"
        );
      }

      // Validar overrides si existen
      validarOverrides(slotReq, slot);
    }

    // 6. Crear la configuración
    ProductoCompuestoConfiguracion configuracion = ProductoCompuestoConfiguracion.builder()
        .compuesto(compuesto)
        .nombre(request.getNombre())
        .descripcion(request.getDescripcion())
        .opcionTrigger(opcionTrigger)  // ⭐ NULL si es default
        .orden(request.getOrden() != null ? request.getOrden() : 0)
        .activa(request.getActiva() != null ? request.getActiva() : true)
        .esDefault(esDefault)  // ⭐ TRUE si es default, FALSE si es condicional
        .build();

    configuracion = configuracionRepository.save(configuracion);

    log.info("Configuración creada con ID: {} (esDefault: {})", configuracion.getId(), esDefault);

    // 7. Crear los slots de la configuración
    int orden = 0;
    for (CrearConfiguracionRequest.SlotConfigRequest slotReq : request.getSlots()) {
      ProductoCompuestoSlot slot = slotRepository.findById(slotReq.getSlotId()).get();

      ProductoCompuestoSlotConfiguracion slotConfig = ProductoCompuestoSlotConfiguracion.builder()
          .configuracion(configuracion)
          .slot(slot)
          .orden(slotReq.getOrden() != null ? slotReq.getOrden() : orden++)
          .cantidadMinimaOverride(slotReq.getCantidadMinimaOverride())
          .cantidadMaximaOverride(slotReq.getCantidadMaximaOverride())
          .esRequeridoOverride(slotReq.getEsRequeridoOverride())
          .precioAdicionalOverride(slotReq.getPrecioAdicionalOverride())
          .build();

      slotConfigRepository.save(slotConfig);

      log.debug("Slot '{}' agregado a configuración (orden: {})", slot.getNombre(), slotConfig.getOrden());
    }

    log.info("Configuración '{}' creada exitosamente con {} slots",
        configuracion.getNombre(),
        request.getSlots().size());

    // 8. Convertir a DTO y retornar
    Long sucursalId = producto.getSucursal() != null ? producto.getSucursal().getId() : 0L;
    return convertirConfiguracionADto(configuracion, sucursalId);
  }

  /**
   * Valida que los overrides sean válidos
   */
  private void validarOverrides(
      CrearConfiguracionRequest.SlotConfigRequest slotReq,
      ProductoCompuestoSlot slot) {

    // Validar cantidades si hay overrides
    if (slotReq.getCantidadMinimaOverride() != null || slotReq.getCantidadMaximaOverride() != null) {

      Integer minima = slotReq.getCantidadMinimaOverride() != null
          ? slotReq.getCantidadMinimaOverride()
          : slot.getCantidadMinima();

      Integer maxima = slotReq.getCantidadMaximaOverride() != null
          ? slotReq.getCantidadMaximaOverride()
          : slot.getCantidadMaxima();

      if (maxima < minima) {
        throw new BusinessException(
            "En slot '" + slot.getNombre() + "': " +
                "cantidadMaxima (" + maxima + ") no puede ser menor que " +
                "cantidadMinima (" + minima + ")"
        );
      }
    }

    // Validar que si es requerido, cantidadMinima sea >= 1
    Boolean esRequerido = slotReq.getEsRequeridoOverride() != null
        ? slotReq.getEsRequeridoOverride()
        : slot.getEsRequerido();

    Integer cantidadMinima = slotReq.getCantidadMinimaOverride() != null
        ? slotReq.getCantidadMinimaOverride()
        : slot.getCantidadMinima();

    if (Boolean.TRUE.equals(esRequerido) && cantidadMinima < 1) {
      throw new BusinessException(
          "En slot '" + slot.getNombre() + "': " +
              "Si es requerido, la cantidad mínima debe ser al menos 1"
      );
    }

    // Validar precio adicional no negativo
    if (slotReq.getPrecioAdicionalOverride() != null &&
        slotReq.getPrecioAdicionalOverride().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(
          "En slot '" + slot.getNombre() + "': " +
              "El precio adicional no puede ser negativo"
      );
    }
  }

  // Agregar a ProductoCompuestoServiceImpl.java

  @Override
  @Transactional
  public ProductoCompuestoConfiguracionDTO actualizarConfiguracion(
      Long configId,
      ActualizarConfiguracionRequest request) {

    log.info("Actualizando configuración {}", configId);

    // 1. Buscar configuración existente
    ProductoCompuestoConfiguracion configuracion = configuracionRepository.findById(configId)
        .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada"));

    ProductoCompuesto compuesto = configuracion.getCompuesto();

    // 2. Actualizar campos básicos si vienen en el request
    if (request.getNombre() != null) {
      configuracion.setNombre(request.getNombre());
    }

    if (request.getDescripcion() != null) {
      configuracion.setDescripcion(request.getDescripcion());
    }

    if (request.getOrden() != null) {
      configuracion.setOrden(request.getOrden());
    }

    if (request.getActiva() != null) {
      configuracion.setActiva(request.getActiva());
    }

    // 3. Actualizar opcionTrigger si viene en el request
    if (request.getOpcionTriggerId() != null) {
      // Validar que la nueva opción existe
      ProductoCompuestoOpcion nuevaOpcion = opcionRepository.findById(request.getOpcionTriggerId())
          .orElseThrow(() -> new ResourceNotFoundException("La opción trigger no existe"));

      // Validar que la opción pertenece a un slot de este compuesto
      boolean opcionPerteneceAlCompuesto = compuesto.getSlots().stream()
          .anyMatch(slot -> slot.getOpciones().stream()
              .anyMatch(opcion -> opcion.getId().equals(request.getOpcionTriggerId()))
          );

      if (!opcionPerteneceAlCompuesto) {
        throw new BusinessException(
            "La opción trigger no pertenece a ningún slot de este producto compuesto"
        );
      }

      // Validar que la nueva opción NO esté usada por OTRA configuración
      configuracionRepository.findByOpcionTriggerId(request.getOpcionTriggerId())
          .ifPresent(configExistente -> {
            // Si existe y NO es la configuración actual, es un error
            if (!configExistente.getId().equals(configId)) {
              throw new BusinessException(
                  "Ya existe otra configuración para esta opción. " +
                      "Una opción solo puede activar una configuración."
              );
            }
          });

      configuracion.setOpcionTrigger(nuevaOpcion);
      log.info("OpcionTrigger actualizada a: {}", nuevaOpcion.getProducto().getNombre());
    }

    // 4. Actualizar slots si vienen en el request
    if (request.getSlots() != null) {

      // Validar que tenga al menos 1 slot
      if (request.getSlots().isEmpty()) {
        throw new BusinessException("Debe incluir al menos un slot en la configuración");
      }

      // Validar que todos los slots pertenecen al compuesto
      for (ActualizarConfiguracionRequest.SlotConfigRequest slotReq : request.getSlots()) {
        ProductoCompuestoSlot slot = slotRepository.findById(slotReq.getSlotId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Slot con ID " + slotReq.getSlotId() + " no encontrado"
            ));

        if (!slot.getCompuesto().getId().equals(compuesto.getId())) {
          throw new BusinessException(
              "El slot '" + slot.getNombre() + "' no pertenece a este producto compuesto"
          );
        }

        // Validar overrides si existen
        validarOverridesActualizacion(slotReq, slot);
      }

      // Eliminar slots existentes (enfoque simple)
      log.info("Eliminando {} slots antiguos", configuracion.getSlots().size());
      slotConfigRepository.deleteByConfiguracionId(configId);
      configuracion.getSlots().clear();

      // Crear nuevos slots
      int orden = 0;
      for (ActualizarConfiguracionRequest.SlotConfigRequest slotReq : request.getSlots()) {
        ProductoCompuestoSlot slot = slotRepository.findById(slotReq.getSlotId()).get();

        ProductoCompuestoSlotConfiguracion slotConfig = ProductoCompuestoSlotConfiguracion.builder()
            .configuracion(configuracion)
            .slot(slot)
            .orden(slotReq.getOrden() != null ? slotReq.getOrden() : orden++)
            .cantidadMinimaOverride(slotReq.getCantidadMinimaOverride())
            .cantidadMaximaOverride(slotReq.getCantidadMaximaOverride())
            .esRequeridoOverride(slotReq.getEsRequeridoOverride())
            .precioAdicionalOverride(slotReq.getPrecioAdicionalOverride())
            .build();

        configuracion.agregarSlot(slotConfig);
        slotConfigRepository.save(slotConfig);

        log.debug("Slot '{}' agregado a configuración", slot.getNombre());
      }

      log.info("Configuración actualizada con {} slots nuevos", configuracion.getSlots().size());
    }

    // 5. Guardar cambios
    configuracion = configuracionRepository.save(configuracion);

    log.info("Configuración '{}' actualizada exitosamente", configuracion.getNombre());

    // 6. Convertir a DTO y retornar
    return convertirConfiguracionADto(
        configuracion,
        compuesto.getProducto().getSucursal().getId()
    );
  }

  /**
   * Valida overrides en actualización (similar a crear pero sin throw en algunos casos)
   */
  private void validarOverridesActualizacion(
      ActualizarConfiguracionRequest.SlotConfigRequest slotReq,
      ProductoCompuestoSlot slot) {

    // Validar cantidades si hay overrides
    if (slotReq.getCantidadMinimaOverride() != null || slotReq.getCantidadMaximaOverride() != null) {

      Integer minima = slotReq.getCantidadMinimaOverride() != null
          ? slotReq.getCantidadMinimaOverride()
          : slot.getCantidadMinima();

      Integer maxima = slotReq.getCantidadMaximaOverride() != null
          ? slotReq.getCantidadMaximaOverride()
          : slot.getCantidadMaxima();

      if (maxima < minima) {
        throw new BusinessException(
            "En slot '" + slot.getNombre() + "': " +
                "cantidadMaxima (" + maxima + ") no puede ser menor que " +
                "cantidadMinima (" + minima + ")"
        );
      }
    }

    // Validar que si es requerido, cantidadMinima sea >= 1
    Boolean esRequerido = slotReq.getEsRequeridoOverride() != null
        ? slotReq.getEsRequeridoOverride()
        : slot.getEsRequerido();

    Integer cantidadMinima = slotReq.getCantidadMinimaOverride() != null
        ? slotReq.getCantidadMinimaOverride()
        : slot.getCantidadMinima();

    if (Boolean.TRUE.equals(esRequerido) && cantidadMinima < 1) {
      throw new BusinessException(
          "En slot '" + slot.getNombre() + "': " +
              "Si es requerido, la cantidad mínima debe ser al menos 1"
      );
    }

    // Validar precio adicional no negativo
    if (slotReq.getPrecioAdicionalOverride() != null &&
        slotReq.getPrecioAdicionalOverride().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(
          "En slot '" + slot.getNombre() + "': " +
              "El precio adicional no puede ser negativo"
      );
    }
  }


  @Override
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public List<ProductoCompuestoConfiguracionDTO> obtenerConfiguraciones(Long productoId) {
    log.info("Listando configuraciones para producto {}", productoId);

    // 1. Validar que el producto existe y es COMPUESTO
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto debe ser tipo COMPUESTO");
    }

    // 2. Obtener el compuesto
    ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "El producto no tiene configuración de compuesto"
        ));

    // 3. Buscar todas las configuraciones del compuesto ordenadas
    List<ProductoCompuestoConfiguracion> configuraciones =
        configuracionRepository.findByCompuestoIdOrderByOrden(compuesto.getId());

    log.info("Se encontraron {} configuraciones", configuraciones.size());

    // 4. Convertir a DTOs CON MANEJO DE ERRORES (NO usar stream)
    Long sucursalId = producto.getSucursal() != null ? producto.getSucursal().getId() : 0L;

    List<ProductoCompuestoConfiguracionDTO> dtos = new ArrayList<>();

    for (ProductoCompuestoConfiguracion config : configuraciones) {
      try {
        ProductoCompuestoConfiguracionDTO dto = convertirConfiguracionADto(config, sucursalId);
        dtos.add(dto);
        log.debug("Configuración {} convertida exitosamente", config.getId());
      } catch (Exception e) {
        log.error("Error convirtiendo configuración {} a DTO: {}",
            config.getId(), e.getMessage(), e);
        // NO lanzar excepción, solo logear y continuar
      }
    }

    log.info("Configuraciones convertidas: {}/{}", dtos.size(), configuraciones.size());

    return dtos;
  }

  // Agregar a ProductoCompuestoServiceImpl.java

  @Override
  @Transactional
  public void eliminarConfiguracion(Long configId) {
    log.info("Eliminando configuración {}", configId);

    // 1. Buscar configuración
    ProductoCompuestoConfiguracion configuracion = configuracionRepository.findById(configId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Configuración no encontrada con ID: " + configId
        ));

    String nombreConfig = configuracion.getNombre();
    int cantidadSlots = configuracion.getSlots().size();

    // 2. Eliminar (cascade automático eliminará los slots asociados)
    configuracionRepository.delete(configuracion);

    log.info("Configuración '{}' eliminada exitosamente (tenía {} slots)",
        nombreConfig,
        cantidadSlots);
  }
}