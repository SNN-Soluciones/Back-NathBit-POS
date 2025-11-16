package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.CategoriaProductoService;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import com.snnsoluciones.backnathbitpos.service.ProductoImagenService;
import com.snnsoluciones.backnathbitpos.service.ProductoValidacionService;
import com.snnsoluciones.backnathbitpos.service.ProductoCategoriaService;
import com.snnsoluciones.backnathbitpos.service.ProductoImpuestoService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCrudServiceImpl implements ProductoCrudService {

  private final ProductoRepository productoRepository;
  private final EmpresaRepository empresaRepository;
  private final EmpresaCABySRepository empresaCABySRepository;
  private final ProductoValidacionService validacionService;
  private final CategoriaProductoService categoriaService;
  private final ProductoImpuestoService impuestoService;
  private final ProductoImagenService productoImagenService;
  private final StorageService storageService;
  private final ProductoCategoriaService productoCategoriaService;
  private final ModelMapper modelMapper;
  private final SucursalRepository sucursalRepository;
  private final ModularHelperService modularHelper;

  @Override
  @Transactional
  public ProductoDto crear(Long empresaId, ProductoCreateDto dto, MultipartFile imagen) {
    log.debug("Creando producto: {} para empresa: {}", dto.getNombre(), empresaId);

    // Validar empresa
    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

    // Validar duplicados
    if (validacionService.existeCodigoInterno(dto.getCodigoInterno(), empresaId, null)) {
      throw new BusinessException("Ya existe un producto con el código: " + dto.getCodigoInterno());
    }

    if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isEmpty() &&
        validacionService.existeCodigoBarras(dto.getCodigoBarras(), empresaId, null)) {
      throw new BusinessException(
          "Ya existe un producto con el código de barras: " + dto.getCodigoBarras());
    }

    if (validacionService.existeNombre(dto.getNombre(), empresaId, null)) {
      throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
    }

    Long sucursalId = dto.getSucursalId() != null ? dto.getSucursalId() : null;

    Sucursal sucursal = modularHelper.determinarSucursalParaEntidad(empresaId, sucursalId, "producto");

    // Crear producto
    Producto producto = Producto.builder()
        .empresa(empresa)
        .sucursal(sucursal)
        .codigoInterno(dto.getCodigoInterno() != null ?
            dto.getCodigoInterno() : generarCodigoInterno(empresaId))
        .codigoBarras(dto.getCodigoBarras())
        .nombre(dto.getNombre())
        .descripcion(dto.getDescripcion())
        .unidadMedida(dto.getUnidadMedida())
        .moneda(dto.getMoneda())
        .precioVenta(dto.getPrecioVenta())
        .tipo(TipoProducto.valueOf(dto.getTipo()))
        .requiereInventario(dto.getRequiereInventario() != null ? dto.getRequiereInventario() : true)
        .requiereReceta(dto.getRequiereReceta() != null ? dto.getRequiereReceta() : false)
        .esServicio(dto.getEsServicio() != null ? dto.getEsServicio() : false)
        .incluyeIVA(dto.getIncluyeIVA() != null ? dto.getIncluyeIVA() : true)
        .activo(dto.getActivo() != null ? dto.getActivo() : true)
        .build();

    // Asignar CABYS
    if (dto.getEmpresaCabysId() != null) {
      EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
          .orElse(null);
      producto.setEmpresaCabys(cabys);
    }

    if (imagen != null && !imagen.isEmpty()) {
      try {
        String urlImagen = productoImagenService.subirImagen(
            empresaId,
            empresa.getNombreComercial() != null ? empresa.getNombreComercial()
                : empresa.getNombreRazonSocial(),
            producto.getCodigoInterno(),
            imagen
        );

        // Construir la key para S3
        String nombreComercialLimpio = limpiarNombreParaRuta(
            empresa.getNombreComercial() != null ? empresa.getNombreComercial()
                : empresa.getNombreRazonSocial()
        );
        String extension = obtenerExtension(imagen.getOriginalFilename());
        String imagenKey = String.format("NathBit-POS/%s/productos/%s%s",
            nombreComercialLimpio, producto.getCodigoInterno(), extension);

        producto.setImagenUrl(urlImagen);
        producto.setImagenKey(imagenKey);

      } catch (Exception e) {
        log.error("Error al subir imagen, continuando sin imagen: {}", e.getMessage());
      }
    }

    // Guardar producto
    producto = productoRepository.save(producto);
    log.info("Producto creado con ID: {}", producto.getId());

    // Asignar categorías si vienen
    if (dto.getCategoriaIds() != null && !dto.getCategoriaIds().isEmpty()) {
      for (Long categoriaId : dto.getCategoriaIds()) {
        CategoriaProducto categoria = categoriaService.buscarPorId(categoriaId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));

        // Validar que pertenezca a la misma empresa
        if (!categoria.getEmpresa().getId().equals(empresa.getId())) {
          throw new BusinessException(
              "La categoría " + categoriaId + " no pertenece a la misma empresa");
        }

        if (!categoria.getActivo()) {
          throw new BusinessException("La categoría " + categoria.getNombre() + " está inactiva");
        }

        productoCategoriaService.agregarCategoria(producto.getId(), categoria.getId());
      }
    }

    // Crear impuestos si vienen
    if (dto.getImpuestos() != null && !dto.getImpuestos().isEmpty()) {
      impuestoService.actualizarImpuestos(producto.getId(),
          dto.getImpuestos().stream()
              .map(imp -> modelMapper.map(imp, ProductoImpuestoDto.class))
              .collect(Collectors.toList())
      );
    }

    return convertirADto(producto);
  }

  @Override
  @Transactional
  public ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto,
      MultipartFile imagen) {
    log.debug("Actualizando producto: {} de empresa: {}", productoId, empresaId);

    // Validar que existe y pertenece a la empresa
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa especificada");
    }

    // Validar duplicados
    if (!producto.getCodigoInterno().equals(dto.getCodigoInterno()) &&
        validacionService.existeCodigoInterno(dto.getCodigoInterno(), empresaId, productoId)) {
      throw new BusinessException("Ya existe un producto con el código: " + dto.getCodigoInterno());
    }

    if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().equals(producto.getCodigoBarras())
        &&
        validacionService.existeCodigoBarras(dto.getCodigoBarras(), empresaId, productoId)) {
      throw new BusinessException(
          "Ya existe un producto con el código de barras: " + dto.getCodigoBarras());
    }

    if (!producto.getNombre().equals(dto.getNombre()) &&
        validacionService.existeNombre(dto.getNombre(), empresaId, productoId)) {
      throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
    }

    // Actualizar campos
    producto.setCodigoInterno(dto.getCodigoInterno());
    producto.setCodigoBarras(dto.getCodigoBarras());
    producto.setNombre(dto.getNombre());
    producto.setDescripcion(dto.getDescripcion());
    producto.setUnidadMedida(dto.getUnidadMedida());
    producto.setMoneda(dto.getMoneda());
    producto.setPrecioVenta(dto.getPrecioVenta());
    producto.setEsServicio(dto.getEsServicio());
    producto.setActivo(dto.getActivo());

    // Actualizar CABYS si cambió
    if (dto.getEmpresaCabysId() != null &&
        (producto.getEmpresaCabys() == null || !producto.getEmpresaCabys().getId()
            .equals(dto.getEmpresaCabysId()))) {
      EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Código CABYS no encontrado: " + dto.getEmpresaCabysId()));
      producto.setEmpresaCabys(cabys);
    }

    if (imagen != null && !imagen.isEmpty()) {
      try {
        // Si había imagen anterior, eliminarla
        if (producto.getImagenKey() != null) {
          try {
            storageService.eliminarArchivo(producto.getImagenKey());
          } catch (Exception e) {
            log.warn("No se pudo eliminar imagen anterior: {}", e.getMessage());
          }
        }

        // Subir nueva imagen
        String urlImagen = productoImagenService.subirImagen(
            empresaId,
            producto.getEmpresa().getNombreComercial() != null ?
                producto.getEmpresa().getNombreComercial()
                : producto.getEmpresa().getNombreRazonSocial(),
            producto.getCodigoInterno(),
            imagen
        );

        // Construir la key
        String nombreComercialLimpio = limpiarNombreParaRuta(
            producto.getEmpresa().getNombreComercial() != null ?
                producto.getEmpresa().getNombreComercial()
                : producto.getEmpresa().getNombreRazonSocial()
        );
        String extension = obtenerExtension(imagen.getOriginalFilename());
        String imagenKey = String.format("NathBit-POS/%s/productos/%s%s",
            nombreComercialLimpio, producto.getCodigoInterno(), extension);

        producto.setImagenUrl(urlImagen);
        producto.setImagenKey(imagenKey);

      } catch (Exception e) {
        log.error("Error al actualizar imagen: {}", e.getMessage());
        throw new RuntimeException("Error al actualizar imagen: " + e.getMessage());
      }
    }

    producto = productoRepository.save(producto);
    log.info("Producto actualizado ID: {}", producto.getId());

    // Actualizar categorías si vienen
    if (dto.getEmpresaCabysId() != null) {
      EmpresaCAByS cabys = empresaCABySRepository.findById(dto.getEmpresaCabysId())
          .orElse(null);
      producto.setEmpresaCabys(cabys);
    }

    return convertirADto(producto);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductoDto obtenerPorId(Long empresaId, Long productoId) {
    Producto producto = productoRepository.findByIdConRelaciones(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa especificada");
    }

    return convertirADto(producto);
  }

  @Override
  @Transactional(readOnly = true)
  public Producto obtenerEntidadPorId(Long productoId) {
    return productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
  }

  @Override
  @Transactional
  public void eliminar(Long empresaId, Long productoId) {
    log.debug("Eliminando producto: {} de empresa: {}", productoId, empresaId);

    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa especificada");
    }

    // TODO: Verificar si está en uso en ventas antes de eliminar

    productoRepository.deleteById(productoId);
    log.info("Producto eliminado ID: {}", productoId);
  }

  @Override
  @Transactional
  public void activarDesactivar(Long empresaId, Long productoId, boolean activo) {
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa especificada");
    }

    producto.setActivo(activo);
    productoRepository.save(producto);

    log.info("Producto {} {}", productoId, activo ? "activado" : "desactivado");
  }

  @Override
  public String generarCodigoInterno(Long empresaId) {
    long count = productoRepository.countByEmpresaIdAndActivoTrue(empresaId);
    String codigo;
    int intentos = 0;

    do {
      codigo = String.format("PROD%05d", count + 1 + intentos);
      intentos++;
    } while (validacionService.existeCodigoInterno(codigo, empresaId, null) && intentos < 100);

    if (intentos >= 100) {
      throw new BusinessException("No se pudo generar un código único");
    }

    return codigo;
  }

  // Método auxiliar para convertir a DTO
  private ProductoDto convertirADto(Producto producto) {
    ProductoDto dto = modelMapper.map(producto, ProductoDto.class);

    // Mapeos adicionales que ModelMapper no puede hacer automáticamente
    dto.setEmpresaId(producto.getEmpresa().getId());

    if (producto.getSucursal() != null) {
      dto.setSucursalId(producto.getSucursal().getId());
      dto.setSucursalNombre(producto.getSucursal().getNombre());
    }

    // Cargar categorías
    Set<CategoriaProductoDto> categorias = producto.getCategorias().stream()
        .map(cat -> modelMapper.map(cat, CategoriaProductoDto.class))
        .collect(Collectors.toSet());
    dto.setCategorias(categorias);

    // Cargar impuestos
    dto.setImpuestos(
        impuestoService.obtenerImpuestos(producto.getId()).stream()
            .map(imp -> modelMapper.map(imp, ProductoImpuestoDto.class))
            .collect(Collectors.toList())
    );

    return dto;
  }

  @Override
  @Transactional
  public ProductoDto actualizarPrecio(Long empresaId, Long productoId, ActualizarPrecioDto dto) {
    log.debug("Actualizando precio del producto: {} de empresa: {}", productoId, empresaId);

    // Buscar el producto
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

    // Validar que pertenece a la empresa
    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa especificada");
    }

    // Actualizar precio
    producto.setPrecioVenta(dto.getPrecioVenta());

    // Actualizar aplica servicio si viene en el DTO
    if (dto.getEsServicio() != null) {
      producto.setEsServicio(dto.getEsServicio());
    }

    // Actualizar impuesto IVA si viene en el DTO
    if (dto.getCodigoTarifaIVA() != null) {
      // Buscar el impuesto IVA actual
      ProductoImpuesto impuestoIVA = producto.getImpuestos().stream()
          .filter(imp -> imp.getTipoImpuesto() == TipoImpuesto.IVA)
          .findFirst()
          .orElse(null);

      if (impuestoIVA != null) {
        // Actualizar tarifa IVA existente
        CodigoTarifaIVA nuevaTarifa = CodigoTarifaIVA.fromCodigo(dto.getCodigoTarifaIVA());
        if (nuevaTarifa == null) {
          throw new BusinessException("Código de tarifa IVA inválido: " + dto.getCodigoTarifaIVA());
        }
        impuestoIVA.setCodigoTarifaIVA(nuevaTarifa);
        impuestoIVA.setPorcentaje(nuevaTarifa.getPorcentaje());
      } else {
        // Crear nuevo impuesto IVA si no existe
        CodigoTarifaIVA nuevaTarifa = CodigoTarifaIVA.fromCodigo(dto.getCodigoTarifaIVA());
        if (nuevaTarifa == null) {
          throw new BusinessException("Código de tarifa IVA inválido: " + dto.getCodigoTarifaIVA());
        }

        ProductoImpuesto nuevoImpuesto = new ProductoImpuesto();
        nuevoImpuesto.setProducto(producto);
        nuevoImpuesto.setTipoImpuesto(TipoImpuesto.IVA);
        nuevoImpuesto.setCodigoTarifaIVA(nuevaTarifa);
        nuevoImpuesto.setPorcentaje(nuevaTarifa.getPorcentaje());
        nuevoImpuesto.setActivo(true);

        producto.getImpuestos().add(nuevoImpuesto);
      }
    }

    // Guardar cambios
    producto = productoRepository.save(producto);

    log.info("Precio del producto {} actualizado exitosamente a {}",
        productoId, dto.getPrecioVenta());

    return convertirADto(producto);
  }

  @Override
  public void save(Producto producto) {
    productoRepository.save(producto);
  }


  private String limpiarNombreParaRuta(String nombre) {
    if (nombre == null) {
      return "sin_nombre";
    }
    return nombre.trim()
        .replaceAll("\\s+", "_")
        .replaceAll("[^a-zA-Z0-9_-]", "")
        .toLowerCase();
  }

  private String obtenerExtension(String nombreArchivo) {
    if (nombreArchivo == null || !nombreArchivo.contains(".")) {
      return "";
    }
    return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
  }

  @Override
  @Transactional
  public ProductoDto crearProductoSimplificado(Long empresaId, Long sucursalId,
      ProductoCreateDto dto, MultipartFile imagen) {
    log.info("Creando producto simplificado para empresa {} y sucursal {}", empresaId, sucursalId);

    try {
      // Verificar que la sucursal pertenezca a la empresa
      Sucursal sucursal = sucursalRepository.findById(sucursalId)
          .orElseThrow(
              () -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));

      if (!sucursal.getEmpresa().getId().equals(empresaId)) {
        throw new BusinessException("La sucursal no pertenece a la empresa especificada");
      }

      // Verificar el modo de facturación
      boolean requiereFacturaElectronica =
          sucursal.getModoFacturacion() == ModoFacturacion.ELECTRONICO;
      RegimenTributario regimen = sucursal.getEmpresa().getRegimenTributario();

      // Si es régimen simplificado sin factura electrónica, forzar tarifa exenta
      if (regimen == RegimenTributario.REGIMEN_SIMPLIFICADO && !requiereFacturaElectronica) {
        log.info("Régimen simplificado sin factura electrónica detectado. Forzando tarifa exenta.");

        // Limpiar impuestos y forzar tarifa exenta
        if (dto.getImpuestos() != null) {
          dto.getImpuestos().clear();
        } else {
          dto.setImpuestos(new ArrayList<>());
        }

      // Si viene con código de tarifa TARIFA_EXENTA, crear impuesto exento
      if ("TARIFA_EXENTA".equals(dto.getImpuestos().get(0).getTipoImpuesto().name())) {
        ProductoImpuestoCreateDto impuestoExento = new ProductoImpuestoCreateDto();
        impuestoExento.setTipoImpuesto(TipoImpuesto.IVA); // IVA
        impuestoExento.setCodigoTarifaIVA(CodigoTarifaIVA.TARIFA_0_EXENTO); // Código MH para exento
        impuestoExento.setPorcentaje(BigDecimal.ZERO);

        List<ProductoImpuestoCreateDto> impuestos = new ArrayList<>();
        impuestos.add(impuestoExento);
        dto.setImpuestos(impuestos);
      }
    }

      // Delegar la creación al método normal
      return crear(empresaId, dto, imagen);

    } catch (Exception e) {
      log.error("Error creando producto simplificado", e);
      throw new BusinessException("Error al crear el producto: " + e.getMessage());
    }
  }

  @Override
  public Map<String, Object> obtenerModoFacturacion(Long sucursalId) {
    try {
      Sucursal sucursal = sucursalRepository.findById(sucursalId)
          .orElseThrow(
              () -> new ResourceNotFoundException("Sucursal no encontrada: " + sucursalId));
      Empresa empresa = sucursal.getEmpresa();

      Map<String, Object> info = new HashMap<>();
      info.put("sucursalId", sucursalId);
      info.put("empresaId", empresa.getId());
      info.put("requiereFacturaElectronica",
          sucursal.getModoFacturacion() == ModoFacturacion.ELECTRONICO);
      info.put("modoFacturacion", sucursal.getModoFacturacion().toString());
      info.put("regimenTributario", empresa.getRegimenTributario().toString());

      return info;
    } catch (Exception e) {
      log.error("Error obteniendo modo de facturación", e);
      throw new BusinessException("Error al obtener información de facturación");
    }
  }
}