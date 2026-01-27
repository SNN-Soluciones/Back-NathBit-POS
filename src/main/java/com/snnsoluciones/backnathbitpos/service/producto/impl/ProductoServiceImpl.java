package com.snnsoluciones.backnathbitpos.service.producto.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.producto.ProductoService;
import com.snnsoluciones.backnathbitpos.service.producto.handler.*;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    // ==================== REPOSITORIES ====================
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final CategoriaProductoRepository categoriaRepository;
    private final FamiliaProductoRepository familiaRepository;

    // ==================== HANDLERS ====================
    private final ProductoValidador validador;
    private final ProductoImagenHandler imagenHandler;
    private final ProductoCategoriaHandler categoriaHandler;
    private final ProductoImpuestoHandler impuestoHandler;
    private final ProductoTributacionHandler tributacionHandler;

    // ==================== CREAR ====================

    @Override
    @Transactional
    public ProductoDto crear(ProductoCreateDto dto, MultipartFile imagen) {
        log.info("Creando producto: {} para empresa: {}, sucursal: {}",
            dto.getNombre(), dto.getEmpresaId(), dto.getSucursalId());

        // 1. Validar datos
        validador.validarCreacion(dto);

        // 2. Obtener empresa
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // 3. Validar y obtener sucursal (si viene)
        Sucursal sucursal = null;
        if (dto.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

            // Validar que la sucursal pertenezca a la empresa
            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BusinessException("La sucursal no pertenece a la empresa especificada");
            }

            log.debug("Producto LOCAL para sucursal: {}", sucursal.getNombre());
        } else {
            log.debug("Producto GLOBAL para toda la empresa");
        }

        // 4. Generar código interno si no viene
        String codigoInterno = dto.getCodigoInterno();
        if (codigoInterno == null || codigoInterno.trim().isEmpty()) {
            codigoInterno = generarCodigoInterno(empresa.getId());
            log.debug("Código interno generado: {}", codigoInterno);
        }

        // 5. Construir producto base
        Producto producto = construirProductoBase(dto, empresa, sucursal, codigoInterno);

        // 6. Guardar producto (sin imagen, categorías ni tributación todavía)
        producto = productoRepository.save(producto);
        log.debug("Producto base guardado con ID: {}", producto.getId());

        // 7. 🆕 CONFIGURAR TRIBUTACIÓN (CABYS + Impuestos)
        tributacionHandler.configurarTributacion(producto, dto);
        log.debug("Tributación configurada");

        // 8. Procesar imagen (si viene)
        if (imagen != null && !imagen.isEmpty()) {
            try {
                imagenHandler.subirImagen(producto, imagen);
                log.debug("Imagen procesada para producto ID: {}", producto.getId());
            } catch (Exception e) {
                log.error("Error al procesar imagen, continuando sin imagen: {}", e.getMessage());
                // No fallar la creación por la imagen
            }
        }

        // 9. Asignar categorías (si vienen)
        if (dto.getCategoriaIds() != null && !dto.getCategoriaIds().isEmpty()) {
            categoriaHandler.asignarCategorias(producto, dto.getCategoriaIds());
            log.debug("Categorías asignadas: {}", dto.getCategoriaIds().size());
        }

        log.info("Producto creado exitosamente con ID: {}", producto.getId());

        // 10. Recargar y convertir a DTO
        producto = productoRepository.findById(producto.getId()).orElseThrow();
        return convertirADto(producto);
    }

    // ==================== ACTUALIZAR ====================

    @Override
    @Transactional
    public ProductoDto actualizar(Long productoId, ProductoUpdateDto dto, MultipartFile imagen) {
        log.info("Actualizando producto ID: {}", productoId);

        // 1. Obtener producto existente
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // 2. Validar actualización
        validador.validarActualizacion(dto, producto);

        // 3. Actualizar campos básicos
        actualizarCamposBasicos(producto, dto);

        // 4. Actualizar imagen (si viene)
        if (imagen != null && !imagen.isEmpty()) {
            try {
                imagenHandler.actualizarImagen(producto, imagen);
                log.debug("Imagen actualizada para producto ID: {}", producto.getId());
            } catch (Exception e) {
                log.error("Error al actualizar imagen: {}", e.getMessage());
                throw new BusinessException("Error al procesar la imagen: " + e.getMessage());
            }
        }

        // 5. Actualizar categorías (si vienen)
        if (dto.getCategoriaIds() != null) {
            categoriaHandler.actualizarCategorias(producto, dto.getCategoriaIds());
            log.debug("Categorías actualizadas");
        }

        // 6. Actualizar impuestos (si vienen)
        if (dto.getImpuestos() != null) {
            impuestoHandler.actualizarImpuestos(producto, dto.getImpuestos());
            log.debug("Impuestos actualizados");
        }

        // 7. Guardar cambios
        producto.setUpdatedAt(LocalDateTime.now());
        producto = productoRepository.save(producto);

        log.info("Producto actualizado exitosamente ID: {}", producto.getId());

        // 8. Recargar y convertir a DTO
        producto = productoRepository.findById(producto.getId()).orElseThrow();
        return convertirADto(producto);
    }

    // ==================== ELIMINAR/ACTIVAR ====================

    @Override
    @Transactional
    public void eliminar(Long productoId) {
        log.info("Eliminando producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Eliminado lógico
        producto.setActivo(false);
        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Producto eliminado (desactivado) ID: {}", productoId);
    }

    @Override
    @Transactional
    public void activar(Long productoId) {
        log.info("Activando producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        producto.setActivo(true);
        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Producto activado ID: {}", productoId);
    }

    @Override
    @Transactional
    public void desactivar(Long productoId) {
        log.info("Desactivando producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        producto.setActivo(false);
        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Producto desactivado ID: {}", productoId);
    }

// ==================== CONSULTAS ====================

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorId(Long productoId) {
        log.debug("Obteniendo producto por ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        return convertirADto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorCodigo(String codigoInterno, Long empresaId) {
        log.debug("Obteniendo producto por código: {} en empresa: {}", codigoInterno, empresaId);

        Producto producto = productoRepository
            .findByCodigoInternoAndEmpresaId(codigoInterno, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Producto no encontrado con código: " + codigoInterno));

        return convertirADto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> listar(Long empresaId, Long sucursalId, String tipo, Pageable pageable) {
        log.debug("Listando productos - empresa: {}, sucursal: {}, tipo: {}", empresaId, sucursalId, tipo);

        Page<Producto> productos;

        // Convertir String tipo a TipoProducto enum (si viene)
        TipoProducto tipoProducto = tipo != null && !tipo.trim().isEmpty()
            ? TipoProducto.valueOf(tipo.trim().toUpperCase())
            : null;

        if (sucursalId == null) {
            // Solo productos GLOBALES (sucursalId = NULL)
            log.debug("Consultando productos GLOBALES");

            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullAndTipo(
                    empresaId, tipoProducto, pageable);
            } else {
                // Sin filtro de tipo (comportamiento original)
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNull(empresaId, pageable);
            }
        } else {
            // Productos GLOBALES + LOCALES de la sucursal
            log.debug("Consultando productos GLOBALES + LOCALES de sucursal: {}", sucursalId);

            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullOrSucursalIdAndTipo(
                    empresaId, sucursalId, tipoProducto, pageable);
            } else {
                // Sin filtro de tipo (comportamiento original)
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullOrSucursalId(
                    empresaId, sucursalId, pageable);
            }
        }

        return productos.map(this::convertirAListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> listarActivos(Long empresaId, Long sucursalId, String tipo, Pageable pageable) {
        log.debug("Listando productos ACTIVOS - empresa: {}, sucursal: {}, tipo: {}", empresaId, sucursalId, tipo);

        Page<Producto> productos;

        // Convertir String tipo a TipoProducto enum (si viene)
        TipoProducto tipoProducto = tipo != null && !tipo.trim().isEmpty()
            ? TipoProducto.valueOf(tipo.trim().toUpperCase())
            : null;

        if (sucursalId == null) {
            // Solo productos GLOBALES activos
            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullAndActivoTrueAndTipo(
                    empresaId, tipoProducto, pageable);
            } else {
                // Sin filtro de tipo (comportamiento original)
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullAndActivoTrue(
                    empresaId, pageable);
            }
        } else {
            // Productos GLOBALES + LOCALES activos
            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullOrSucursalIdAndActivoTrueAndTipo(
                    empresaId, sucursalId, tipoProducto, pageable);
            } else {
                // Sin filtro de tipo (comportamiento original)
                productos = productoRepository.findByEmpresaIdAndSucursalIdIsNullOrSucursalIdAndActivoTrue(
                    empresaId, sucursalId, pageable);
            }
        }

        return productos.map(this::convertirAListDto);
    }

    // ==================== BÚSQUEDAS ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> buscar(Long empresaId, Long sucursalId, String termino,
        String tipo, boolean activo, Pageable pageable) {
        log.debug("Buscando productos con término: '{}' - empresa: {}, sucursal: {}, tipo: {}, activo: {}",
            termino, empresaId, sucursalId, tipo, activo);

        if (termino == null || termino.trim().isEmpty()) {
            // Si no hay término, usar listar
            return activo
                ? listarActivos(empresaId, sucursalId, tipo, pageable)
                : listar(empresaId, sucursalId, tipo, pageable);
        }

        String terminoLimpio = "%" + termino.trim().toLowerCase() + "%";
        Page<Producto> productos;

        // Convertir String tipo a TipoProducto enum (si viene)
        TipoProducto tipoProducto = tipo != null && !tipo.trim().isEmpty()
            ? TipoProducto.valueOf(tipo.trim().toUpperCase())
            : null;

        if (sucursalId == null) {
            // Buscar solo en productos GLOBALES
            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                if (activo) {
                    productos = productoRepository.buscarGlobalesPorTerminoYTipoActivos(
                        empresaId, terminoLimpio, tipoProducto, pageable);
                } else {
                    productos = productoRepository.buscarGlobalesPorTerminoYTipo(
                        empresaId, terminoLimpio, tipoProducto, pageable);
                }
            } else {
                // Sin filtro de tipo (comportamiento original)
                if (activo) {
                    productos = productoRepository.buscarGlobalesPorTerminoActivos(
                        empresaId, terminoLimpio, pageable);
                } else {
                    productos = productoRepository.buscarGlobalesPorTermino(
                        empresaId, terminoLimpio, pageable);
                }
            }
        } else {
            // Buscar en productos GLOBALES + LOCALES
            if (tipoProducto != null) {
                // ✨ CON FILTRO DE TIPO
                if (activo) {
                    productos = productoRepository.buscarGlobalesYLocalesPorTerminoYTipoActivos(
                        empresaId, sucursalId, terminoLimpio, tipoProducto, pageable);
                } else {
                    productos = productoRepository.buscarGlobalesYLocalesPorTerminoYTipo(
                        empresaId, sucursalId, terminoLimpio, tipoProducto, pageable);
                }
            } else {
                // Sin filtro de tipo (comportamiento original)
                if (activo) {
                    productos = productoRepository.buscarGlobalesYLocalesPorTerminoActivos(
                        empresaId, sucursalId, terminoLimpio, pageable);
                } else {
                    productos = productoRepository.buscarGlobalesYLocalesPorTermino(
                        empresaId, sucursalId, terminoLimpio, pageable);
                }
            }
        }

        return productos.map(this::convertirAListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> buscarPorCategoria(Long categoriaId, Long empresaId,
        Long sucursalId, Pageable pageable) {
        log.debug("Buscando productos por categoría: {} - empresa: {}, sucursal: {}",
            categoriaId, empresaId, sucursalId);

        // Validar que la categoría existe
        categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        Page<Producto> productos;

        if (sucursalId == null) {
            // Solo productos GLOBALES de la categoría
            productos = productoRepository.findByCategoriasIdAndEmpresaIdAndSucursalIdIsNull(
                categoriaId, empresaId, pageable);
        } else {
            // Productos GLOBALES + LOCALES de la categoría
            productos = productoRepository.findByCategoriasIdAndEmpresaIdAndSucursalIdIsNullOrSucursalId(
                categoriaId, empresaId, sucursalId, pageable);
        }

        return productos.map(this::convertirAListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoListDto> buscarPorFamilia(Long familiaId, Long empresaId,
        Long sucursalId, Pageable pageable) {
        log.debug("Buscando productos por familia: {} - empresa: {}, sucursal: {}",
            familiaId, empresaId, sucursalId);

        // Validar que la familia existe
        familiaRepository.findById(familiaId)
            .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));

        Page<Producto> productos;

        if (sucursalId == null) {
            // Solo productos GLOBALES de la familia
            productos = productoRepository.findByFamiliaIdAndEmpresaIdAndSucursalIdIsNull(
                familiaId, empresaId, pageable);
        } else {
            // Productos GLOBALES + LOCALES de la familia
            productos = productoRepository.findByFamiliaIdAndEmpresaIdAndSucursalIdIsNullOrSucursalId(
                familiaId, empresaId, sucursalId, pageable);
        }

        return productos.map(this::convertirAListDto);
    }

    // ==================== IMÁGENES ====================

    @Override
    @Transactional
    public void actualizarImagen(Long productoId, MultipartFile imagen) {
        log.info("Actualizando solo imagen del producto ID: {}", productoId);

        if (imagen == null || imagen.isEmpty()) {
            throw new BadRequestException("La imagen es obligatoria");
        }

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        imagenHandler.actualizarImagen(producto, imagen);

        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Imagen actualizada para producto ID: {}", productoId);
    }

    @Override
    @Transactional
    public void eliminarImagen(Long productoId) {
        log.info("Eliminando imagen del producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        imagenHandler.eliminarImagen(producto);

        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Imagen eliminada del producto ID: {}", productoId);
    }

    // ==================== UTILIDADES ====================

    @Override
    public String generarCodigoInterno(Long empresaId) {
        log.debug("Generando código interno para empresa: {}", empresaId);

        // Obtener el último código de la empresa
        String ultimoCodigo = productoRepository.findUltimoCodigoInternoByEmpresa(empresaId);

        if (ultimoCodigo == null) {
            // Primera vez, empezar en 1
            return "PROD-00001";
        }

        try {
            // Extraer el número del código (PROD-00001 → 00001)
            String numeroParte = ultimoCodigo.substring(5); // Después de "PROD-"
            int numero = Integer.parseInt(numeroParte);
            numero++;

            // Formatear con ceros a la izquierda
            String codigoGenerado = String.format("PROD-%05d", numero);
            log.debug("Código generado: {}", codigoGenerado);

            return codigoGenerado;

        } catch (Exception e) {
            log.error("Error generando código, usando formato por defecto", e);
            // Fallback: usar timestamp
            return "PROD-" + System.currentTimeMillis();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigoInterno(String codigoInterno, Long empresaId) {
        return productoRepository.existsByCodigoInternoAndEmpresaId(codigoInterno, empresaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigoBarras(String codigoBarras) {
        return productoRepository.existsByCodigoBarras(codigoBarras);
    }

    // ==================== MÉTODOS PRIVADOS HELPER ====================

    /**
     * Construye la entidad Producto base a partir del DTO
     */
    private Producto construirProductoBase(ProductoCreateDto dto, Empresa empresa,
        Sucursal sucursal, String codigoInterno) {
        Producto producto = new Producto();

        // Identificación
        producto.setEmpresa(empresa);
        producto.setSucursal(sucursal); // NULL si es global
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCodigoInterno(codigoInterno);
        producto.setCodigoBarras(dto.getCodigoBarras());

        // Tipo y clasificación
        TipoProducto tipo = TipoProducto.valueOf(dto.getTipo());
        producto.setTipo(tipo);
        producto.setTipoInventario(
            dto.getTipoInventario() != null ? dto.getTipoInventario() :
                determinarTipoInventarioPorDefecto(tipo));

        // Precios
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setUnidadMedida(dto.getUnidadMedida());
        producto.setMoneda(dto.getMoneda());
        producto.setIncluyeIVA(dto.getIncluyeIVA() != null ? dto.getIncluyeIVA() : true);
        producto.setEsServicio(dto.getEsServicio() != null ? dto.getEsServicio() : false);

        // Inventario
        if (dto.getFactorConversionReceta() != null) {
            producto.setFactorConversionReceta(dto.getFactorConversionReceta());
        }

        // Producto compuesto
        if (tipo == TipoProducto.COMPUESTO) {
            producto.setRequierePersonalizacion(true);
            producto.setPrecioBase(dto.getPrecioBase() != null ? dto.getPrecioBase() : dto.getPrecioVenta());
            producto.setTipoInventario(TipoInventario.NINGUNO);
        } else {
            producto.setRequierePersonalizacion(
                dto.getRequierePersonalizacion() != null ? dto.getRequierePersonalizacion() : false);
        }

        // Estado
        producto.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        producto.setCreatedAt(LocalDateTime.now());
        producto.setUpdatedAt(LocalDateTime.now());

        return producto;
    }

    /**
     * Actualiza los campos básicos del producto desde el DTO
     */
    private void actualizarCamposBasicos(Producto producto, ProductoUpdateDto dto) {
        if (dto.getNombre() != null) {
            producto.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            producto.setDescripcion(dto.getDescripcion());
        }
        if (dto.getCodigoBarras() != null) {
            producto.setCodigoBarras(dto.getCodigoBarras());
        }
        if (dto.getTipoInventario() != null) {
            producto.setTipoInventario(dto.getTipoInventario());
        }
        if (dto.getPrecioVenta() != null) {
            producto.setPrecioVenta(dto.getPrecioVenta());
        }
        if (dto.getUnidadMedida() != null) {
            producto.setUnidadMedida(dto.getUnidadMedida());
        }
        if (dto.getMoneda() != null) {
            producto.setMoneda(dto.getMoneda());
        }
        if (dto.getIncluyeIVA() != null) {
            producto.setIncluyeIVA(dto.getIncluyeIVA());
        }
        if (dto.getEsServicio() != null) {
            producto.setEsServicio(dto.getEsServicio());
        }
        if (dto.getFactorConversionReceta() != null) {
            producto.setFactorConversionReceta(dto.getFactorConversionReceta());
        }
        if (dto.getRequierePersonalizacion() != null) {
            producto.setRequierePersonalizacion(dto.getRequierePersonalizacion());
        }
        if (dto.getPrecioBase() != null) {
            producto.setPrecioBase(dto.getPrecioBase());
        }
        if (dto.getActivo() != null) {
            producto.setActivo(dto.getActivo());
        }
    }

    /**
     * Determina el tipo de inventario por defecto según el tipo de producto
     */
    private TipoInventario determinarTipoInventarioPorDefecto(TipoProducto tipo) {
        return switch (tipo) {
            case VENTA -> TipoInventario.SIMPLE;
            case MATERIA_PRIMA -> TipoInventario.SIMPLE;
            case COMBO -> TipoInventario.REFERENCIA;
            case COMPUESTO -> TipoInventario.NINGUNO;
            default -> TipoInventario.SIMPLE;
        };
    }

    /**
     * Convierte entidad Producto a ProductoDto (completo)
     */
    private ProductoDto convertirADto(Producto producto) {
        ProductoDto dto = ProductoDto.builder()
            .id(producto.getId())
            .empresaId(producto.getEmpresa().getId())
            .empresaNombre(producto.getEmpresa().getNombreComercial())
            .sucursalId(producto.getSucursal() != null ? producto.getSucursal().getId() : null)
            .sucursalNombre(producto.getSucursal() != null ? producto.getSucursal().getNombre() : null)
            .codigoInterno(producto.getCodigoInterno())
            .codigoBarras(producto.getCodigoBarras())
            .nombre(producto.getNombre())
            .descripcion(producto.getDescripcion())
            .tipo(producto.getTipo())
            .tipoInventario(producto.getTipoInventario())
            .unidadMedida(producto.getUnidadMedida())
            .moneda(producto.getMoneda())
            .precioVenta(producto.getPrecioVenta())
            .precioBase(producto.getPrecioBase())
            .incluyeIVA(producto.getIncluyeIVA())
            .esServicio(producto.getEsServicio())
            .factorConversionReceta(producto.getFactorConversionReceta())
            .requierePersonalizacion(producto.getRequierePersonalizacion())
            .imagenUrl(producto.getImagenUrl())
            .thumbnailUrl(producto.getThumbnailUrl())
            .activo(producto.getActivo())
            .createdAt(producto.getCreatedAt())
            .updatedAt(producto.getUpdatedAt())
            .build();

        // Categorías
        if (producto.getCategorias() != null && !producto.getCategorias().isEmpty()) {
            Set<CategoriaProductoDto> categorias = producto.getCategorias().stream()
                .map(this::convertirCategoriaADto)
                .collect(Collectors.toSet());
            dto.setCategorias(categorias);
        }

        // Familia
        if (producto.getFamilia() != null) {
            dto.setFamiliaId(producto.getFamilia().getId());
            dto.setFamiliaNombre(producto.getFamilia().getNombre());
            dto.setFamiliaCodigo(producto.getFamilia().getCodigo());
            dto.setFamiliaColor(producto.getFamilia().getColor());
        }

        // Impuestos y cálculos
        if (producto.getImpuestos() != null && !producto.getImpuestos().isEmpty()) {
            List<ProductoImpuestoDto> impuestos = producto.getImpuestos().stream()
                .map(this::convertirImpuestoADto)
                .collect(Collectors.toList());
            dto.setImpuestos(impuestos);

            // Calcular totales
            BigDecimal totalImpuestos = calcularTotalImpuestos(producto);
            dto.setTotalImpuestos(totalImpuestos);
            dto.setPrecioFinal(producto.getPrecioVenta().add(totalImpuestos));
        } else {
            dto.setTotalImpuestos(BigDecimal.ZERO);
            dto.setPrecioFinal(producto.getPrecioVenta());
        }

        return dto;
    }

    /**
     * Convierte entidad Producto a ProductoListDto (optimizado)
     */
    private ProductoListDto convertirAListDto(Producto producto) {
        return ProductoListDto.builder()
            .id(producto.getId())
            .empresaId(producto.getEmpresa().getId())
            .sucursalId(producto.getSucursal() != null ? producto.getSucursal().getId() : null)
            .sucursalNombre(producto.getSucursal() != null ? producto.getSucursal().getNombre() : null)
            .codigoInterno(producto.getCodigoInterno())
            .codigoBarras(producto.getCodigoBarras())
            .nombre(producto.getNombre())
            .tipo(producto.getTipo().name())
            .precioVenta(producto.getPrecioVenta())
            .precioFinal(calcularPrecioFinal(producto))
            .familiaId(producto.getFamilia() != null ? producto.getFamilia().getId() : null)
            .familiaNombre(producto.getFamilia() != null ? producto.getFamilia().getNombre() : null)
            .familiaColor(producto.getFamilia() != null ? producto.getFamilia().getColor() : null)
            .imagenUrl(producto.getImagenUrl())
            .thumbnailUrl(producto.getThumbnailUrl())
            .activo(producto.getActivo())
            .esGlobal(producto.getSucursal() == null)
            .unidadMedida(producto.getUnidadMedida().name())
            .zonaPreparacion(producto.getZonaPreparacion())  // 🆕 AGREGAR
            .esServicio(producto.getEsServicio())             // 🆕 AGREGAR
            .empresaCabys(producto.getEmpresaCabys() != null
                ? convertirEmpresaCabysADto(producto.getEmpresaCabys())
                : null)
            .impuestos(producto.getImpuestos() != null
                ? producto.getImpuestos().stream()
                .map(this::convertirImpuestoADto)
                .toList()
                : List.of())
            .categorias(producto.getCategorias() != null
                ? producto.getCategorias().stream()
                .map(this::convertirCategoriaADto)
                .toList()
                : List.of())
            .build();
    }

    /**
     * Convierte CategoriaProducto a DTO
     */
    private CategoriaProductoDto convertirCategoriaADto(CategoriaProducto categoria) {
        return CategoriaProductoDto.builder()
            .id(categoria.getId())
            .nombre(categoria.getNombre())
            .color(categoria.getColor())
            // NO incluir código si no existe en la entidad
            .build();
    }

    /**
     * Convierte ProductoImpuesto a DTO
     */
    private ProductoImpuestoDto convertirImpuestoADto(ProductoImpuesto impuesto) {
        return ProductoImpuestoDto.builder()
            .id(impuesto.getId())
            .tipoImpuesto(impuesto.getTipoImpuesto())  // ← Es TipoImpuesto, no .getTipo()
            .codigoTarifaIVA(impuesto.getCodigoTarifaIVA())  // ← Es codigoTarifaIVA
            .porcentaje(impuesto.getPorcentaje())  // ← Es porcentaje, no tarifa
            .activo(impuesto.getActivo())
            .build();
    }

    /**
     * Calcula el total de impuestos del producto
     */
    private BigDecimal calcularTotalImpuestos(Producto producto) {
        if (producto.getImpuestos() == null || producto.getImpuestos().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal precioBase = producto.getPrecioVenta();
        BigDecimal totalImpuestos = BigDecimal.ZERO;

        for (ProductoImpuesto impuesto : producto.getImpuestos()) {
            // Usar porcentaje (no tarifa ni montoImpuesto)
            if (impuesto.getPorcentaje() != null) {
                BigDecimal montoImpuesto = precioBase
                    .multiply(impuesto.getPorcentaje())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                totalImpuestos = totalImpuestos.add(montoImpuesto);
            }
        }

        return totalImpuestos;
    }

    /**
     * Calcula el precio final del producto (precio base + impuestos)
     */
    private BigDecimal calcularPrecioFinal(Producto producto) {
        BigDecimal precioBase = producto.getPrecioVenta();

        if (precioBase == null) {
            return BigDecimal.ZERO;
        }

        // Si no tiene impuestos, el precio final es igual al precio base
        if (producto.getImpuestos() == null || producto.getImpuestos().isEmpty()) {
            return precioBase;
        }

        // Si el precio ya incluye IVA, retornar el precio tal cual
        if (Boolean.TRUE.equals(producto.getIncluyeIVA())) {
            return precioBase;
        }

        // Calcular impuestos y sumarlos al precio base
        BigDecimal totalImpuestos = BigDecimal.ZERO;

        for (ProductoImpuesto impuesto : producto.getImpuestos()) {
            if (Boolean.TRUE.equals(impuesto.getActivo()) && impuesto.getPorcentaje() != null) {
                BigDecimal montoImpuesto = precioBase
                    .multiply(impuesto.getPorcentaje())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                totalImpuestos = totalImpuestos.add(montoImpuesto);
            }
        }

        return precioBase.add(totalImpuestos);
    }

    /**
     * Convierte EmpresaCabys a DTO
     */
    private EmpresaCABySSelectDto convertirEmpresaCabysADto(EmpresaCAByS empresaCabys) {
        if (empresaCabys == null) {
            return null;
        }

        return EmpresaCABySSelectDto.builder()
            .id(empresaCabys.getId())
            .codigo(empresaCabys.getCodigoCabys().getCodigo())
            .codigoCabys(empresaCabys.getCodigoCabys() != null
                ? convertirCodigoCabysADto(empresaCabys.getCodigoCabys())
                : null)
            .activo(empresaCabys.getActivo())
            .build();
    }

    /**
     * Convierte CodigoCAByS a DTO
     */
    private CodigoCABySDto convertirCodigoCabysADto(CodigoCAByS codigoCabys) {
        if (codigoCabys == null) {
            return null;
        }

        return CodigoCABySDto.builder()
            .codigo(codigoCabys.getCodigo())
            .descripcion(codigoCabys.getDescripcion())
            .impuestoSugerido(codigoCabys.getImpuestoSugerido())
            .build();
    }
}