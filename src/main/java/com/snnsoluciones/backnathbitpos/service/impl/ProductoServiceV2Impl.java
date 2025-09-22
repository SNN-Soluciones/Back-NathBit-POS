// ProductoServiceV2Impl.java (ACTUALIZADO con S3PathService)
package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoImagenService;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import com.snnsoluciones.backnathbitpos.service.ProductoServiceV2;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
  private final EmpresaCABySRepository empresaCAbySRepository;
  private final CodigoCABySRepository codigoCAbySRepository;
  private final ProductoImagenService productoImagenService;
  private final ProductoInventarioRepository inventarioRepository;
  private final ProductoRecetaRepository recetaRepository;
  private final FacturaDetalleRepository facturaDetalleRepository;

  // ========== CRUD CON IMÁGENES ==========

  @Override
  @Transactional
  public ProductoDto crear(Long empresaId, ProductoCreateDto dto, MultipartFile imagen) {
    log.info("Creando producto tipo: {} para empresa: {} - sucursal: {}",
        dto.getTipo(), empresaId, dto.getSucursalId());

    Sucursal sucursal = null;
    try {
      // 1. VALIDACIONES DE ENTRADA
      if (empresaId == null) {
        throw new BusinessException("EmpresaId es requerido");
      }

      // 2. OBTENER EMPRESA
      Empresa empresa = empresaRepository.findById(empresaId)
          .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + empresaId));

      // 3. OBTENER SUCURSAL SI VIENE
      if (dto.getSucursalId() != null) {
        sucursal = sucursalRepository.findById(dto.getSucursalId())
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + dto.getSucursalId()));

        // Validar que la sucursal pertenezca a la empresa
        if (!sucursal.getEmpresa().getId().equals(empresaId)) {
          throw new BusinessException("La sucursal no pertenece a la empresa especificada");
        }
        log.info("Producto será local para sucursal: {}", sucursal.getNombre());
      } else {
        log.info("Producto será global para toda la empresa");
      }

      // 4. DETERMINAR MODO DE FACTURACIÓN
      boolean esRegimenSimplificado = empresa.getRegimenTributario() == RegimenTributario.REGIMEN_SIMPLIFICADO;
      boolean esFacturacionElectronica = empresa.getRequiereHacienda();

      // Para sucursal específica, usar su configuración
      if (sucursal != null && sucursal.getModoFacturacion() == ModoFacturacion.SOLO_INTERNO) {
        esFacturacionElectronica = false;
      }

      // 5. VALIDAR CÓDIGOS ÚNICOS
      validarCodigosUnicos(empresaId, dto.getSucursalId(), dto.getCodigoInterno(), dto.getCodigoBarras());

      // 6. CREAR PRODUCTO
      Producto producto = new Producto();
      producto.setEmpresa(empresa);
      producto.setSucursal(sucursal); // Puede ser null para productos globales
      producto.setCodigoInterno(dto.getCodigoInterno() != null ?
          dto.getCodigoInterno() : generarCodigoInterno(empresaId, dto.getSucursalId()));
      producto.setCodigoBarras(dto.getCodigoBarras());
      producto.setNombre(dto.getNombre());
      producto.setDescripcion(dto.getDescripcion());
      producto.setTipo(TipoProducto.valueOf(dto.getTipo()));
      producto.setActivo(true);
      producto.setCreatedAt(LocalDateTime.now());
      producto.setUpdatedAt(LocalDateTime.now());

      // 7. CONFIGURAR SEGÚN RÉGIMEN TRIBUTARIO
      if (esRegimenSimplificado || !esFacturacionElectronica) {
        log.info("Aplicando configuración para régimen simplificado o sin factura electrónica");

        // Buscar o crear CABYS genérico
        Sucursal finalSucursal = sucursal;
        EmpresaCAByS cabysSinMesa = empresaCAbySRepository
            .findByEmpresaIdAndCodigoCabysCodigoAndActivoTrue(empresaId, "9960199999999")
            .orElseGet(() -> {
              CodigoCAByS codigoGenerico = codigoCAbySRepository.findByCodigo("9960199999999")
                  .orElseThrow(() -> new BusinessException("CABYS genérico no configurado en el sistema"));

              EmpresaCAByS nuevo = EmpresaCAByS.builder()
                  .empresa(empresa)
                  .sucursal(finalSucursal)
                  .codigoCabys(codigoGenerico)
                  .activo(true)
                  .createdAt(LocalDateTime.now())
                  .build();
              return empresaCAbySRepository.save(nuevo);
            });

        producto.setEmpresaCabys(cabysSinMesa);

        // Crear IVA 0% exonerado
        ProductoImpuesto impuestoExento = new ProductoImpuesto();
        impuestoExento.setProducto(producto);
        impuestoExento.setTipoImpuesto(TipoImpuesto.IVA);
        impuestoExento.setCodigoTarifaIVA(CodigoTarifaIVA.TARIFA_EXENTA);
        impuestoExento.setPorcentaje(BigDecimal.ZERO);
        impuestoExento.setActivo(true);
        producto.setImpuestos(Set.of(impuestoExento));

        // Unidad por defecto
        producto.setUnidadMedida(UnidadMedida.SERVICIOS_PROFESIONALES);

      } else {
        log.info("Aplicando configuración para régimen tradicional con factura electrónica");

        // VALIDAR CAMPOS REQUERIDOS
        if (dto.getEmpresaCabysId() == null) {
          throw new BusinessException("Régimen tradicional requiere código CABYS");
        }

        if (dto.getImpuestos() == null || dto.getImpuestos().isEmpty()) {
          throw new BusinessException("Régimen tradicional requiere configuración de impuestos");
        }

        // Asignar CABYS
        EmpresaCAByS empresaCabys = empresaCAbySRepository.findById(dto.getEmpresaCabysId())
            .orElseThrow(() -> new BusinessException("CABYS no autorizado para esta empresa"));
        producto.setEmpresaCabys(empresaCabys);

        // Configurar impuestos
        Set<ProductoImpuesto> impuestos = new HashSet<>();
        for (ProductoImpuestoCreateDto impDto : dto.getImpuestos()) {
          ProductoImpuesto impuesto = new ProductoImpuesto();
          impuesto.setTipoImpuesto(impDto.getTipoImpuesto());
          impuesto.setCodigoTarifaIVA(impDto.getCodigoTarifaIVA());
          impuesto.setPorcentaje(impDto.getCodigoTarifaIVA().getPorcentaje());
          impuesto.setProducto(producto);
          impuesto.setActivo(true);
          impuestos.add(impuesto);
        }
        producto.setImpuestos(impuestos);

        // Unidad de medida del DTO
        producto.setUnidadMedida(dto.getUnidadMedida());
      }

      // 8. CONFIGURAR PRECIOS
      producto.setPrecioVenta(dto.getPrecioVenta());
      producto.setPrecioCompra(dto.getPrecioCompra() != null ? dto.getPrecioCompra() : BigDecimal.ZERO);
      producto.setMoneda(dto.getMoneda() != null ? dto.getMoneda() : Moneda.CRC);

      // 9. CONFIGURAR INVENTARIO SEGÚN EMPRESA/SUCURSAL
      boolean manejaInventario = false;
      if (sucursal != null && sucursal.getManejaInventario() != null) {
        manejaInventario = sucursal.getManejaInventario();
      }

      producto.setRequiereInventario(manejaInventario);

      if (sucursal != null && sucursal.getManejaInventario() != null) {
        manejaInventario = sucursal.getManejaInventario();
      }

      producto.setRequiereInventario(manejaInventario);

      // 10. CONFIGURAR SEGÚN TIPO DE PRODUCTO
      configurarSegunTipo(producto, dto);

      // Validaciones específicas por tipo
      if (producto.getTipo() == TipoProducto.MATERIA_PRIMA) {
        producto.setPrecioVenta(BigDecimal.ZERO); // Forzar precio 0
        producto.setRequiereInventario(true); // Siempre requiere inventario
      }

      // 11. GUARDAR PRODUCTO
      producto = productoRepository.save(producto);
      log.info("Producto creado con ID: {} - Alcance: {}",
          producto.getId(),
          sucursal != null ? "Sucursal " + sucursal.getNombre() : "Global empresa");

      // 12. MANEJAR IMAGEN SI EXISTE
      if (imagen != null && !imagen.isEmpty()) {
        try {
          validarImagen(imagen);

          String urlImagen = productoImagenService.subirImagen(
              empresaId,
              empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreRazonSocial(),
              producto.getCodigoInterno(),
              imagen
          );

          // Construir key para S3
          String carpeta = sucursal != null ?
              limpiarNombreParaRuta(sucursal.getNombre()) :
              limpiarNombreParaRuta(empresa.getNombreComercial());
          String extension = obtenerExtension(imagen.getOriginalFilename());
          String imagenKey = String.format("NathBit-POS/%s/productos/%s%s",
              carpeta, producto.getCodigoInterno(), extension);

          producto.setImagenUrl(urlImagen);
          producto.setImagenKey(imagenKey);
          producto = productoRepository.save(producto);

        } catch (Exception e) {
          log.error("Error al subir imagen, continuando sin imagen: {}", e.getMessage());
          // No fallar la creación por la imagen
        }
      }

      // 13. CREAR INVENTARIO INICIAL SI ES MATERIA PRIMA Y ES LOCAL
      if (producto.getTipo() == TipoProducto.MATERIA_PRIMA &&
          producto.getRequiereInventario() &&
          sucursal != null) {

        ProductoInventario inventario = ProductoInventario.builder()
            .producto(producto)
            .sucursal(sucursal)
            .cantidadActual(BigDecimal.ZERO)
            .cantidadMinima(BigDecimal.ZERO)
            // cantidadMaxima NO existe en ProductoInventario
            .estado(true)
            .build();

        inventarioRepository.save(inventario);
        log.info("Inventario inicial creado para materia prima en sucursal: {}", sucursal.getNombre());
      }

      // 14. CONVERTIR Y RETORNAR
      return convertirADto(producto);

    } catch (BusinessException | ResourceNotFoundException e) {
      log.error("Error de negocio creando producto: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Error inesperado creando producto", e);
      throw new BusinessException("Error al crear producto: " + e.getMessage());
    }
  }

// MÉTODOS AUXILIARES

  private void validarCodigosUnicos(Long empresaId, Long sucursalId, String codigoInterno, String codigoBarras) {
    if (codigoInterno != null) {
      boolean existeCodigo;

      if (sucursalId != null) {
        // Validar unicidad en la sucursal
        existeCodigo = productoRepository.existsByCodigoInternoAndSucursalId(codigoInterno, sucursalId);
      } else {
        // Validar unicidad en productos globales de la empresa
        existeCodigo = productoRepository
            .existsByCodigoInternoAndEmpresaIdAndSucursalIdIsNull(codigoInterno, empresaId);
      }

      if (existeCodigo) {
        throw new BusinessException("El código interno ya existe en este alcance");
      }
    }

    if (codigoBarras != null && !codigoBarras.isEmpty()) {
      if (productoRepository.existsByCodigoBarrasAndEmpresaId(codigoBarras, empresaId)) {
        throw new BusinessException("El código de barras ya existe");
      }
    }
  }

  private void validarImagen(MultipartFile imagen) {
    if (!Objects.requireNonNull(imagen.getContentType()).startsWith("image/")) {
      throw new BusinessException("El archivo debe ser una imagen");
    }
    if (imagen.getSize() > 5 * 1024 * 1024) {
      throw new BusinessException("Imagen muy grande, máximo 5MB");
    }
  }

  private String generarCodigoInterno(Long empresaId, Long sucursalId) {
    long count;
    String prefix;

    if (sucursalId != null) {
      // Código para producto de sucursal
      count = productoRepository.countBySucursalIdAndActivoTrue(sucursalId);
      Sucursal sucursal = sucursalRepository.findById(sucursalId).orElse(null);
      prefix = sucursal != null && sucursal.getNumeroSucursal() != null ?
          sucursal.getNumeroSucursal() + "-" : "SUC" + sucursalId + "-";
    } else {
      // Código para producto global
      count = productoRepository.countByEmpresaIdAndSucursalIdIsNullAndActivoTrue(empresaId);
      prefix = "GLOB-";
    }

    String codigo;
    int intentos = 0;

    do {
      codigo = String.format("%sPROD%05d", prefix, count + 1 + intentos);
      intentos++;
    } while ((sucursalId != null ?
        productoRepository.existsByCodigoInternoAndSucursalId(codigo, sucursalId) :
        productoRepository.existsByCodigoInternoAndEmpresaIdAndSucursalIdIsNull(codigo, empresaId))
        && intentos < 100);

    if (intentos >= 100) {
      throw new BusinessException("No se pudo generar un código único");
    }

    return codigo;
  }

  private ProductoDto convertirADto(Producto producto) {
    ProductoDto dto = modelMapper.map(producto, ProductoDto.class);

    // Información adicional
    dto.setEmpresaId(producto.getEmpresa().getId());

    if (producto.getSucursal() != null) {
      dto.setSucursalId(producto.getSucursal().getId());
      dto.setSucursalNombre(producto.getSucursal().getNombre());
    }

    // URL de imagen
    if (producto.getImagenUrl() != null) {
      dto.setImagenUrl(producto.getImagenUrl());
    }

    return dto;
  }

  @Override
  @Transactional
  public ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto,
      MultipartFile imagen) {
    log.info("Actualizando producto ID: {} para empresa: {}", productoId, empresaId);

    try {
      // 1. Buscar y validar producto existe y pertenece a la empresa
      Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
          .orElseThrow(() -> new ResourceNotFoundException(
              "Producto no encontrado o no pertenece a la empresa"));

      // 2. Validar si se puede cambiar el tipo
      if (dto.getTipo() != null) {
        // Validar que no tenga relaciones que impidan el cambio
        if (productoTieneRelaciones(producto)) {
          throw new BusinessException(
              "No se puede cambiar el tipo de un producto con inventario, recetas o ventas asociadas");
        }
        producto.setTipo(TipoProducto.valueOf(String.valueOf(dto.getTipo())));
      }

      // 3. Determinar si cambió el régimen o modo facturación
      Sucursal sucursal = producto.getSucursal();
      boolean esSimplificadoSinFactura;

      if (sucursal != null && sucursal.getModoFacturacion() != null) {
        esSimplificadoSinFactura = sucursal.getModoFacturacion()
            .equals(ModoFacturacion.SOLO_INTERNO);
      } else {
        Empresa empresa = producto.getEmpresa();
        esSimplificadoSinFactura =
            empresa.getRegimenTributario() == RegimenTributario.REGIMEN_SIMPLIFICADO
                && !empresa.getRequiereHacienda();
      }

      // 4. Actualizar campos básicos
      if (dto.getNombre() != null) {
        producto.setNombre(dto.getNombre());
      }
      if (dto.getDescripcion() != null) {
        producto.setDescripcion(dto.getDescripcion());
      }
      if (dto.getActivo() != null) {
        producto.setActivo(dto.getActivo());
      }

      // 5. Actualizar precios
      if (dto.getPrecioVenta() != null) {
        // Validar precio para materia prima
        if (producto.getTipo() == TipoProducto.MATERIA_PRIMA &&
            dto.getPrecioVenta().compareTo(BigDecimal.ZERO) > 0) {
          log.warn("Actualizando materia prima {} con precio de venta", producto.getNombre());
        }
        producto.setPrecioVenta(dto.getPrecioVenta());
      }
      if (dto.getPrecioCompra() != null) {
        producto.setPrecioCompra(dto.getPrecioCompra());
      }

      // 6. Actualizar configuración tributaria solo si cambió
      if (dto.getActualizarConfigTributaria() != null && dto.getActualizarConfigTributaria()) {

        if (esSimplificadoSinFactura) {
          log.info("Manteniendo configuración simplificada para producto {}", productoId);
          // En simplificado no se puede cambiar CABYS ni impuestos

        } else {
          log.info("Actualizando configuración tributaria tradicional");

          // Actualizar CABYS si viene
          if (dto.getEmpresaCabysId() != null) {
            EmpresaCAByS nuevoCabys = empresaCAbySRepository
                .findById(dto.getEmpresaCabysId())
                .orElseThrow(() -> new BusinessException(
                    "CABYS no autorizado para esta empresa"));
            producto.setEmpresaCabys(nuevoCabys);
          }

          // Actualizar impuestos si vienen
          if (dto.getImpuestos() != null && !dto.getImpuestos().isEmpty()) {
            // Eliminar impuestos antiguos
            producto.getImpuestos().clear();

            // Crear nuevos impuestos
            Set<ProductoImpuesto> nuevosImpuestos = new HashSet<>();
            for (ProductoImpuestoCreateDto impDto : dto.getImpuestos()) {
              ProductoImpuesto impuesto = new ProductoImpuesto();
              impuesto.setTipoImpuesto(impDto.getTipoImpuesto());
              impuesto.setCodigoTarifaIVA(impDto.getCodigoTarifaIVA());
              impuesto.setPorcentaje(impDto.getCodigoTarifaIVA().getPorcentaje());
              impuesto.setProducto(producto);
              impuesto.setActivo(true);
              nuevosImpuestos.add(impuesto);
            }
            producto.setImpuestos(nuevosImpuestos);
          }

          // Actualizar unidad medida
          if (dto.getUnidadMedida() != null) {
            producto.setUnidadMedida(dto.getUnidadMedida());
          }
        }
      }

      // 7. Actualizar configuración según tipo (si cambió el tipo)
      if (dto.getTipo() != null) {
        configurarSegunTipoUpdate(producto, dto);
      }

      // 8. Actualizar timestamp
      producto.setUpdatedAt(LocalDateTime.now());

      // 9. Guardar cambios
      producto = productoRepository.save(producto);

      // 10. Actualizar imagen si viene nueva
      if (imagen != null && !imagen.isEmpty()) {
        // Validar imagen
        if (!imagen.getContentType().startsWith("image/")) {
          throw new BusinessException("El archivo debe ser una imagen");
        }
        if (imagen.getSize() > 5 * 1024 * 1024) {
          throw new BusinessException("Imagen muy grande, máximo 5MB");
        }

        try {
          // Eliminar imagen anterior de S3 si existe
          if (producto.getImagenKey() != null) {
            productoImagenService.eliminarImagen(empresaId, productoId);
          }

          // Subir nueva imagen
          String urlImagen = productoImagenService.subirImagen(
              empresaId,
              producto.getEmpresa().getNombreComercial() != null ?
                  producto.getEmpresa().getNombreComercial() :
                  producto.getEmpresa().getNombreRazonSocial(),
              producto.getCodigoInterno(),
              imagen
          );

          // Construir nueva key
          String nombreComercialLimpio;
          if (producto.getSucursal() != null) {
            nombreComercialLimpio = producto.getSucursal().getNombre();
          } else {
            nombreComercialLimpio = producto.getEmpresa().getNombreComercial() != null ?
                producto.getEmpresa().getNombreComercial() :
                producto.getEmpresa().getNombreRazonSocial();
          }
          String extension = obtenerExtension(imagen.getOriginalFilename());
          String imagenKey = String.format("NathBit-POS/%s/productos/%s%s",
              limpiarNombreParaRuta(nombreComercialLimpio),
              producto.getCodigoInterno(),
              extension);

          producto.setImagenUrl(urlImagen);
          producto.setImagenKey(imagenKey);
          producto = productoRepository.save(producto);

          log.info("Imagen actualizada exitosamente para producto {}", productoId);

        } catch (Exception e) {
          log.error("Error actualizando imagen, manteniendo imagen anterior: {}", e.getMessage());
          // No fallar la actualización por la imagen
        }
      }

      log.info("Producto {} actualizado exitosamente", productoId);

      // 11. Convertir y retornar
      return modelMapper.map(producto, ProductoDto.class);

    } catch (BusinessException | ResourceNotFoundException e) {
      log.error("Error de negocio actualizando producto: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Error inesperado actualizando producto", e);
      throw new BusinessException("Error al actualizar producto: " + e.getMessage());
    }
  }

  private boolean productoTieneRelaciones(Producto producto) {
    // 1. Verificar si tiene inventario con cantidad > 0
    if (inventarioRepository.existsByProductoIdAndCantidadActualGreaterThan(
        producto.getId(), BigDecimal.ZERO)) {
      log.info("Producto {} tiene inventario, no se puede cambiar tipo", producto.getId());
      return true;
    }

    // 2. Verificar si está como ingrediente en alguna receta
    if (recetaRepository.existsByIngredientesProductoId(producto.getId())) {
      log.info("Producto {} es ingrediente en recetas, no se puede cambiar tipo", producto.getId());
      return true;
    }

    // 3. Verificar si tiene ventas
    if (facturaDetalleRepository.existsByProductoId(producto.getId())) {
      log.info("Producto {} tiene ventas registradas, no se puede cambiar tipo", producto.getId());
      return true;
    }

    return false;
  }

  // Configurar según tipo en actualización
  private void configurarSegunTipoUpdate(Producto producto, ProductoUpdateDto dto) {
    // Similar a configurarSegunTipo pero para update
    switch (producto.getTipo()) {
      case MIXTO:
        if (dto.getFactorConversionReceta() != null) {
          producto.setFactorConversionReceta(dto.getFactorConversionReceta());
        }
        break;
      case VENTA:
        if (dto.getTipoInventario() != null) {
          // Validar que sea SIMPLE o RECETA
          if (!Arrays.asList(TipoInventario.SIMPLE, TipoInventario.RECETA)
              .contains(dto.getTipoInventario())) {
            throw new BusinessException("Producto de venta solo acepta inventario SIMPLE o RECETA");
          }
          producto.setTipoInventario(dto.getTipoInventario());
        }
        break;
      case COMBO:
        if (dto.getTipoInventario() != null) {
          // Validar que sea PROPIO o REFERENCIA
          if (!Arrays.asList(TipoInventario.PROPIO, TipoInventario.REFERENCIA)
              .contains(dto.getTipoInventario())) {
            throw new BusinessException("Combo solo acepta inventario PROPIO o REFERENCIA");
          }
          producto.setTipoInventario(dto.getTipoInventario());
        }
        break;
      // MATERIA_PRIMA y COMPUESTO no tienen opciones adicionales en update
    }
  }

  // Método auxiliar para generar código único
  private String generarCodigoProducto(Long empresaId, ProductoCreateDto dto) {
    String prefix = dto.getTipo().substring(0, 3);
    String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
    return String.format("%s-%d-%s", prefix, empresaId, timestamp);
  }

  // Método auxiliar para configurar según tipo
  private void configurarSegunTipo(Producto producto, ProductoCreateDto dto) {
    switch (TipoProducto.valueOf(dto.getTipo())) {
      case MIXTO:
        producto.setTipoInventario(TipoInventario.SIMPLE);
        producto.setFactorConversionReceta(dto.getFactorConversionReceta() != null ?
            dto.getFactorConversionReceta() : BigDecimal.ONE);
        break;

      case MATERIA_PRIMA:
        producto.setTipoInventario(TipoInventario.SIMPLE);
        producto.setTipo(TipoProducto.MATERIA_PRIMA);
        break;

      case VENTA:
        producto.setTipoInventario(dto.getTipoInventario() != null ?
            dto.getTipoInventario() : TipoInventario.SIMPLE);
        producto.setTipo(TipoProducto.VENTA);
        break;

      case COMBO:
        producto.setTipoInventario(dto.getTipoInventario() != null ?
            dto.getTipoInventario() : TipoInventario.REFERENCIA);
        producto.setTipo(TipoProducto.COMBO);
        break;

      case COMPUESTO:
        producto.setTipoInventario(TipoInventario.NINGUNO);
        producto.setRequierePersonalizacion(true);
        producto.setTipo(TipoProducto.COMPUESTO);
        producto.setPrecioBase(dto.getPrecioVenta());
        break;
    }
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

      String imagenPath = s3PathService.buildArchivoPath(producto.getEmpresa().getNombreComercial(),
          producto.getNombre());

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

  private String obtenerExtension(String nombreArchivo) {
    if (nombreArchivo == null || !nombreArchivo.contains(".")) {
      return "";
    }
    return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
  }

  private String limpiarNombreParaRuta(String nombre) {
    if (nombre == null) {
      return "sin_nombre";
    }
    return nombre.trim()
        .replaceAll("\\s+", "_")
        .replaceAll("[^a-zA-Z0-9_-]", "")
        .toUpperCase();
  }
}