// ProductoServiceV2Impl.java (ACTUALIZADO con S3PathService)
package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoServiceV2;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ContentType;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceV2Impl implements ProductoServiceV2 {

  private final ProductoRepository productoRepository;
  private final EmpresaRepository empresaRepository;
  private final SucursalRepository sucursalRepository;
  private final S3PathBuilder s3PathService;
  private final ModelMapper modelMapper;
  private final StorageService storageService;

  // ========== CRUD CON IMÁGENES ==========

  @Override
  @Transactional
  public ProductoDto crear(Long empresaId, ProductoCreateDto dto) {
    return crearProductoInterno(empresaId, dto, null);
  }

  @Override
  @Transactional
  public ProductoDto crearConImagen(Long empresaId, ProductoCreateDto dto, MultipartFile imagen) {
    return crearProductoInterno(empresaId, dto, imagen);
  }

  private ProductoDto crearProductoInterno(Long empresaId, ProductoCreateDto dto,
      MultipartFile imagen) {
    log.info("Creando producto para empresa: {}", empresaId);

    // Validar empresa
    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

    // Validar códigos únicos
    if (dto.getCodigoInterno() != null && existeCodigoInterno(empresaId, dto.getCodigoInterno())) {
      throw new BusinessException("El código interno ya existe");
    }

    if (dto.getCodigoBarras() != null && existeCodigoBarras(dto.getCodigoBarras(), null)) {
      throw new BusinessException("El código de barras ya existe");
    }

    // Crear producto
    Producto producto = new Producto();
    producto.setEmpresa(empresa);
    producto.setNombre(dto.getNombre());
    producto.setDescripcion(dto.getDescripcion());
    producto.setCodigoInterno(dto.getCodigoInterno() != null ?
        dto.getCodigoInterno() : generarCodigoInterno(empresaId));
    producto.setCodigoBarras(dto.getCodigoBarras());
    producto.setPrecioBase(dto.getPrecioVenta());
    producto.setTipo(
        dto.getTipo() != null ? TipoProducto.valueOf(dto.getTipo()) : TipoProducto.VENTA);
    producto.setTipoInventario(dto.getTipoInventario() != null ?
        dto.getTipoInventario() : TipoInventario.SIMPLE);
    producto.setActivo(true);

    // Validar consistencia tipo/inventario
    validarTipoInventario(producto.getTipo(), producto.getTipoInventario());

    // Si hay sucursal específica
    if (dto.getSucursal() != null) {
      Sucursal sucursal = sucursalRepository.findById(dto.getSucursal())
          .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
      producto.setSucursal(sucursal);
    }

    producto = productoRepository.save(producto);

    // Guardar imagen si se proporciona
    if (imagen != null && !imagen.isEmpty()) {
      guardarImagen(producto, imagen);
    }

    return convertirADto(producto);
  }

  @Override
  @Transactional
  public ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto) {
    return actualizarProductoInterno(empresaId, productoId, dto, null);
  }

  @Override
  @Transactional
  public ProductoDto actualizarConImagen(Long empresaId, Long productoId, ProductoUpdateDto dto,
      MultipartFile imagen) {
    return actualizarProductoInterno(empresaId, productoId, dto, imagen);
  }

  private ProductoDto actualizarProductoInterno(Long empresaId, Long productoId,
      ProductoUpdateDto dto, MultipartFile imagen) {
    log.info("Actualizando producto: {} de empresa: {}", productoId, empresaId);

    Producto producto = buscarProductoValidado(empresaId, productoId);

    // Actualizar campos básicos
    if (dto.getNombre() != null) {
      producto.setNombre(dto.getNombre());
    }

    if (dto.getDescripcion() != null) {
      producto.setDescripcion(dto.getDescripcion());
    }

    if (dto.getPrecioVenta() != null) {
      producto.setPrecioBase(dto.getPrecioVenta());
    }

    // Validar códigos si cambian
    if (dto.getCodigoInterno() != null && !dto.getCodigoInterno()
        .equals(producto.getCodigoInterno())) {
      if (existeCodigoInterno(empresaId, dto.getCodigoInterno())) {
        throw new BusinessException("El código interno ya existe");
      }
      producto.setCodigoInterno(dto.getCodigoInterno());
    }

    if (dto.getCodigoBarras() != null && !dto.getCodigoBarras()
        .equals(producto.getCodigoBarras())) {
      if (existeCodigoBarras(dto.getCodigoBarras(), productoId)) {
        throw new BusinessException("El código de barras ya existe");
      }
      producto.setCodigoBarras(dto.getCodigoBarras());
    }

    producto = productoRepository.save(producto);

    // Actualizar imagen si se proporciona
    if (imagen != null && !imagen.isEmpty()) {
      guardarImagen(producto, imagen);
    }

    return convertirADto(producto);
  }

  @Override
  @Transactional
  public void actualizarImagen(Long empresaId, Long productoId, MultipartFile imagen) {
    log.info("Actualizando imagen de producto: {}", productoId);

    Producto producto = buscarProductoValidado(empresaId, productoId);
    guardarImagen(producto, imagen);
  }

  @Override
  @Transactional
  public void eliminarImagen(Long empresaId, Long productoId) {
    log.info("Eliminando imagen de producto: {}", productoId);

    Producto producto = buscarProductoValidado(empresaId, productoId);

    if (producto.getImagenUrl() != null) {
      try {
//                s3PathService.eliminarArchivo(producto.getImagenUrl());
        producto.setImagenUrl(null);
        productoRepository.save(producto);
      } catch (Exception e) {
        log.error("Error al eliminar imagen: ", e);
        throw new BusinessException("Error al eliminar la imagen");
      }
    }
  }

  // ========== MÉTODOS CRUD ANTERIORES (sin cambios) ==========

  @Override
  @Transactional
  public void desactivar(Long empresaId, Long productoId) {
    log.info("Desactivando producto: {} de empresa: {}", productoId, empresaId);

    Producto producto = buscarProductoValidado(empresaId, productoId);
    producto.setActivo(false);
    productoRepository.save(producto);
  }

  @Override
  @Transactional
  public void activar(Long empresaId, Long productoId) {
    log.info("Activando producto: {} de empresa: {}", productoId, empresaId);

    Producto producto = buscarProductoValidado(empresaId, productoId);
    producto.setActivo(true);
    productoRepository.save(producto);
  }

  @Override
  @Transactional
  public void eliminar(Long empresaId, Long productoId) {
    log.info("Eliminando producto: {} de empresa: {}", productoId, empresaId);

    Producto producto = buscarProductoValidado(empresaId, productoId);

    // Eliminar imagen si existe
    if (producto.getImagenUrl() != null) {
      try {
//                s3PathService.eliminarArchivo(producto.getImagenUrl());
      } catch (Exception e) {
        log.warn("No se pudo eliminar la imagen: {}", e.getMessage());
      }
    }

    productoRepository.delete(producto);
  }

  // ========== BÚSQUEDAS (sin cambios) ==========

  @Override
  @Transactional(readOnly = true)
  public ProductoDto buscarPorId(Long empresaId, Long productoId) {
    Producto producto = buscarProductoValidado(empresaId, productoId);
    return convertirADto(producto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductoDto> buscarPorEmpresa(Long empresaId, Pageable pageable) {
    return productoRepository.findByEmpresaId(empresaId, pageable)
        .map((element) -> modelMapper.map(element, ProductoDto.class));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductoDto> buscarPorSucursal(Long sucursalId, Pageable pageable) {
    return productoRepository.findBySucursalId(sucursalId, pageable)
        .map((element) -> modelMapper.map(element, ProductoDto.class));
  }

  @Override
  @Transactional(readOnly = true)
  public ProductoDto buscarPorCodigo(Long empresaId, String codigoInterno) {
    Producto producto = productoRepository
        .findByCodigoInternoAndEmpresaId(codigoInterno, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Producto no encontrado con código: " + codigoInterno));

    return convertirADto(producto);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductoDto buscarPorCodigoBarras(String codigoBarras, Long empresaId) {
    Producto producto = productoRepository
        .findByCodigoBarrasAndEmpresaId(codigoBarras, empresaId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Producto no encontrado con código de barras: " + codigoBarras));

    return convertirADto(producto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductoDto> buscarActivos(Long empresaId, Pageable pageable) {
    return productoRepository.findByEmpresaIdAndActivoTrue(empresaId,
        pageable).map((element) -> modelMapper.map(element, ProductoDto.class));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductoDto> buscarPaginado(Long empresaId, String termino, Pageable pageable) {
    Page<Producto> productos;

    if (termino != null && !termino.trim().isEmpty()) {
      productos = productoRepository.buscarPorEmpresa(empresaId, termino.trim(), pageable);
    } else {
      productos = productoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);
    }

    return productos.map(this::convertirADto);
  }

  // ========== UTILIDADES ==========

  @Override
  public String generarCodigoInterno(Long empresaId) {
    long count = productoRepository.countByEmpresaIdAndSucursalIdIsNullAndActivoTrue(empresaId);
    String codigo;
    int intentos = 0;

    do {
      codigo = String.format("PROD%05d", count + 1 + intentos);
      intentos++;
    } while (existeCodigoInterno(empresaId, codigo) && intentos < 100);

    if (intentos >= 100) {
      throw new BusinessException("No se pudo generar un código único");
    }

    return codigo;
  }

  @Override
  public boolean existeCodigoInterno(Long empresaId, String codigo) {
    return productoRepository.existsByCodigoInternoAndEmpresaId(codigo, empresaId);
  }

  @Override
  public boolean existeCodigoBarras(String codigoBarras, Long empresaId) {
      return productoRepository.existsByCodigoBarrasAndEmpresaId(codigoBarras, empresaId);
  }

  // ========== MÉTODOS PRIVADOS ==========

  private Producto buscarProductoValidado(Long empresaId, Long productoId) {
    Producto producto = productoRepository.findById(productoId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

    if (!producto.getEmpresa().getId().equals(empresaId)) {
      throw new BusinessException("El producto no pertenece a la empresa");
    }

    return producto;
  }

  private void validarTipoInventario(TipoProducto tipo, TipoInventario tipoInventario) {
    if (tipo == TipoProducto.MATERIA_PRIMA && tipoInventario != TipoInventario.SIMPLE) {
      throw new BusinessException("MATERIA_PRIMA solo permite inventario SIMPLE");
    }

    if (tipo == TipoProducto.MIXTO && tipoInventario != TipoInventario.SIMPLE) {
      throw new BusinessException("MIXTO solo permite inventario SIMPLE");
    }

    if (tipo == TipoProducto.COMPUESTO && tipoInventario != TipoInventario.NINGUNO) {
      throw new BusinessException("COMPUESTO debe tener inventario NINGUNO");
    }
  }

  private void guardarImagen(Producto producto, MultipartFile imagen) {
    try {
      // Validar archivo
      if (imagen.isEmpty()) {
        throw new BusinessException("El archivo está vacío");
      }

      // Validar tipo de archivo
      String contentType = imagen.getContentType();
      if (contentType == null || !contentType.startsWith("image/")) {
        throw new BusinessException("El archivo debe ser una imagen");
      }

      // Si ya tiene imagen, eliminar la anterior
      if (producto.getImagenUrl() != null) {
        try {
//          s3PathService.eliminarArchivo(producto.getImagenUrl());
        } catch (Exception e) {
          log.warn("No se pudo eliminar imagen anterior: {}", e.getMessage());
        }
      }

      String imagenPath = s3PathService.buildArchivoPath(producto.getEmpresa().getNombreComercial(), producto.getNombre());

      // Subir nueva imagen
      String imagenUrl = storageService.subirArchivo(
          imagen,
          imagenPath,
          ContentType.IMAGE_PNG.toString(),
          true
      );

      producto.setImagenUrl(imagenUrl);
      productoRepository.save(producto);

    } catch (Exception e) {
      log.error("Error al guardar imagen: ", e);
      throw new BusinessException("Error al guardar la imagen: " + e.getMessage());
    }
  }

  private ProductoDto convertirADto(Producto producto) {
    ProductoDto dto = modelMapper.map(producto, ProductoDto.class);

    dto.setEmpresaId(producto.getEmpresa().getId());

    if (producto.getSucursal() != null) {
      dto.setSucursalId(producto.getSucursal().getId());
      dto.setSucursalNombre(producto.getSucursal().getNombre());
    }

    // URL de imagen desde S3
    if (producto.getImagenUrl() != null) {
      dto.setImagenUrl(producto.getImagenUrl());
    }

    return dto;
  }
}