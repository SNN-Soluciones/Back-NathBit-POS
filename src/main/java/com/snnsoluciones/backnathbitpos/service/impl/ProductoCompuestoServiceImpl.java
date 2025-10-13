package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoSlotDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoOpcionDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
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

  private void agregarOpcionASlot(ProductoCompuestoSlot slot,
      ProductoCompuestoRequest.OpcionRequest opcionRequest, Long empresaId, int orden) {
    // Validar y obtener el producto que será la opción
    Producto productoOpcion = productoRepository.findById(opcionRequest.getProductoId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Producto opción no encontrado: " + opcionRequest.getProductoId()));

    // Validar que el producto pertenezca a la misma empresa
    if (!productoOpcion.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto opción no pertenece a la empresa");
    }

    // Validar tipo de producto - solo MIXTO, MATERIA_PRIMA o VENTA pueden ser opciones
    if (productoOpcion.getTipo() == TipoProducto.COMPUESTO
        || productoOpcion.getTipo() == TipoProducto.COMBO) {
      throw new BusinessException(
          "Un producto compuesto o combo fijo no puede ser opción de otro compuesto");
    }

    // Crear la opción
    ProductoCompuestoOpcion opcion = new ProductoCompuestoOpcion();
    opcion.setSlot(slot);
    opcion.setProducto(productoOpcion); // ⚠️ AQUÍ ESTABA EL ERROR - No se asignaba el producto
    opcion.setPrecioAdicional(opcionRequest.getPrecioAdicional());
    opcion.setEsDefault(
        opcionRequest.getEsDefault() != null ? opcionRequest.getEsDefault() : false);
    opcion.setDisponible(
        opcionRequest.getDisponible() != null ? opcionRequest.getDisponible() : true);
    opcion.setOrden(opcionRequest.getOrden() != null ? opcionRequest.getOrden() : orden);

    opcionRepository.save(opcion);
  }

  private ProductoCompuestoDto convertirADto(ProductoCompuesto compuesto) {
    ProductoCompuestoDto dto = modelMapper.map(compuesto, ProductoCompuestoDto.class);
    dto.setProductoId(compuesto.getProducto().getId());
    dto.setProductoNombre(compuesto.getProducto().getNombre());
    dto.setProductoCodigoInterno(compuesto.getProducto().getCodigoInterno());

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
  public CalculoPrecioResponse calcularPrecio(Long productoId, Long sucursalId,
      List<Long> opcionesSeleccionadas) {
    log.info("Calculando precio para producto {} con {} opciones", productoId,
        opcionesSeleccionadas.size());

    // Obtener producto y validar
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (producto.getTipo() != TipoProducto.COMPUESTO) {
      throw new BusinessException("El producto no es de tipo COMPUESTO");
    }

    BigDecimal precioBase =
        producto.getPrecioBase() != null ? producto.getPrecioBase() : producto.getPrecioVenta();
    BigDecimal totalAdicionales = BigDecimal.ZERO;
    List<CalculoPrecioResponse.DetalleOpcion> detalles = new ArrayList<>();

    // Procesar cada opción seleccionada
    for (Long opcionId : opcionesSeleccionadas) {
      ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
          .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada: " + opcionId));

      // Verificar disponibilidad en sucursal
      boolean disponibleEnSucursal = verificarDisponibilidadEnSucursal(opcion, sucursalId);

      totalAdicionales = totalAdicionales.add(opcion.getPrecioAdicional());

      detalles.add(CalculoPrecioResponse.DetalleOpcion.builder()
          .opcionId(opcionId)
          .productoNombre(opcion.getProducto().getNombre())
          .slotNombre(opcion.getSlot().getNombre())
          .precioAdicional(opcion.getPrecioAdicional())
          .disponibleEnSucursal(disponibleEnSucursal)
          .build());
    }

    BigDecimal precioFinal = precioBase.add(totalAdicionales);

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

    // Obtener opciones manuales ordenadas
    List<ProductoCompuestoOpcion> opcionesManuales = slot.getOpciones()
        .stream()
        .sorted(Comparator.comparingInt(ProductoCompuestoOpcion::getOrden))
        .toList();

    log.info("Slot tiene {} opciones manuales", opcionesManuales.size());

    for (ProductoCompuestoOpcion opcionManual : opcionesManuales) {
      Producto producto = opcionManual.getProducto();

      // Verificar stock en sucursal
      boolean tieneStock = verificarStockProducto(producto.getId(), sucursal.getId());
      Integer stockDisponible = obtenerStockDisponible(producto.getId(), sucursal.getId());

      // La disponibilidad final es: disponible en config Y tiene stock
      boolean disponibleFinal = opcionManual.getDisponible() && tieneStock;

      BigDecimal precioAdicional = opcionManual.getPrecioAdicional() != null
          ? opcionManual.getPrecioAdicional()
          : BigDecimal.ZERO;

      boolean esGratuita = precioAdicional.compareTo(BigDecimal.ZERO) == 0;

      // Crear DTO
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
          .esDefault(opcionManual.getEsDefault())
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
      return false;  // En caso de error, marcar como no disponible
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
}