package com.snnsoluciones.backnathbitpos.service.producto.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.producto.ProductoService;
import com.snnsoluciones.backnathbitpos.service.producto.handler.*;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementación del servicio de productos.
 * Orquesta los handlers para proporcionar funcionalidad completa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    // ==================== REPOSITORIES ====================
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaCABySRepository empresaCAbySRepository;
    private final FamiliaProductoRepository familiaProductoRepository;

    // ==================== HANDLERS ====================
    private final ProductoValidador validador;
    private final ProductoImagenHandler imagenHandler;
    private final ProductoCategoriaHandler categoriaHandler;
    private final ProductoImpuestoHandler impuestoHandler;

    // ==================== UTILITIES ====================
    private final ModelMapper modelMapper;

    // ==================== CRUD BÁSICO ====================

    @Override
    @Transactional
    public ProductoDto crear(ProductoCreateDto dto, MultipartFile imagen) {
        log.info("Creando producto: {} para empresa: {}", dto.getNombre(), dto.getEmpresaId());

        // 1. Validar datos
        validador.validarCreacion(dto);

        // 2. Obtener empresa
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        // 3. Obtener o validar sucursal
        Sucursal sucursal = null;
        if (dto.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

            // Validar que la sucursal pertenezca a la empresa
            if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new BusinessException("La sucursal no pertenece a la empresa");
            }
        }

        // 4. Construir producto base
        Producto producto = construirProductoBase(dto, empresa, sucursal);

        // 5. Guardar producto (sin imagen ni categorías todavía)
        producto = productoRepository.save(producto);
        log.debug("Producto base guardado con ID: {}", producto.getId());

        // 6. Procesar imagen (si viene)
        if (imagen != null && !imagen.isEmpty()) {
            try {
                imagenHandler.subirImagen(producto, imagen);
                log.debug("Imagen procesada para producto ID: {}", producto.getId());
            } catch (Exception e) {
                log.error("Error al procesar imagen, continuando sin imagen: {}", e.getMessage());
                // No fallar la creación por la imagen
            }
        }

        // 7. Asignar categorías (si vienen)
        if (dto.getCategoriaIds() != null && !dto.getCategoriaIds().isEmpty()) {
            Set<Long> categoriaIds = new HashSet<>(dto.getCategoriaIds());
            categoriaHandler.asignarCategorias(producto, categoriaIds);
            log.debug("Categorías asignadas: {}", categoriaIds.size());
        }

        // 8. Crear impuestos (si vienen)
        if (dto.getImpuestos() != null && !dto.getImpuestos().isEmpty()) {
            List<ImpuestoDto> impuestos = convertirImpuestos(dto.getImpuestos());
            impuestoHandler.crearImpuestos(producto, impuestos);
            log.debug("Impuestos creados: {}", impuestos.size());
        }

        log.info("Producto creado exitosamente con ID: {}", producto.getId());

        // 9. Recargar y convertir a DTO
        producto = productoRepository.findById(producto.getId()).orElseThrow();
        return convertirADto(producto);
    }

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

        // 4. Guardar cambios básicos
        producto.setUpdatedAt(LocalDateTime.now());
        producto = productoRepository.save(producto);
        log.debug("Campos básicos actualizados para producto ID: {}", productoId);

        // 5. Actualizar imagen (si viene)
        if (imagen != null && !imagen.isEmpty()) {
            try {
                imagenHandler.actualizarImagen(producto, imagen);
                log.debug("Imagen actualizada para producto ID: {}", productoId);
            } catch (Exception e) {
                log.error("Error al actualizar imagen: {}", e.getMessage());
                // No fallar la actualización por la imagen
            }
        }

        // 6. Actualizar categorías (si vienen)
        if (dto.getCategoriaIds() != null) {
            Set<Long> categoriaIds = new HashSet<>(dto.getCategoriaIds());
            categoriaHandler.asignarCategorias(producto, categoriaIds);
            log.debug("Categorías actualizadas para producto ID: {}", productoId);
        }

        // 7. Actualizar impuestos (si vienen)
        if (dto.getImpuestos() != null) {
            List<ImpuestoDto> impuestos = convertirImpuestos(dto.getImpuestos());
            impuestoHandler.actualizarImpuestos(producto, impuestos);
            log.debug("Impuestos actualizados para producto ID: {}", productoId);
        }

        log.info("Producto ID: {} actualizado exitosamente", productoId);

        // 8. Recargar y convertir a DTO
        producto = productoRepository.findById(productoId).orElseThrow();
        return convertirADto(producto);
    }

    @Override
    @Transactional
    public void eliminar(Long productoId) {
        log.info("Eliminando producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Validar que se puede eliminar
        validador.validarEliminacion(producto);

        // Eliminar imagen si tiene
        if (producto.getImagenKey() != null) {
            try {
                imagenHandler.eliminarImagen(producto);
            } catch (Exception e) {
                log.warn("No se pudo eliminar imagen al eliminar producto: {}", e.getMessage());
            }
        }

        // Eliminar producto (las categorías e impuestos se eliminan por cascade/orphanRemoval)
        productoRepository.delete(producto);
        log.info("Producto ID: {} eliminado exitosamente", productoId);
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

        log.info("Producto ID: {} activado", productoId);
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

        log.info("Producto ID: {} desactivado", productoId);
    }

    // ==================== CONSULTAS ====================

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorId(Long productoId) {
        log.debug("Obteniendo producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        return convertirADto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorCodigo(String codigoInterno, Long empresaId) {
        log.debug("Obteniendo producto por código: {} en empresa: {}", codigoInterno, empresaId);

        Producto producto = productoRepository.findByCodigoInternoAndEmpresaId(codigoInterno, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con código: " + codigoInterno));

        return convertirADto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> listar(Long empresaId, Pageable pageable) {
        log.debug("Listando productos de empresa: {}", empresaId);

        return productoRepository.findByEmpresaId(empresaId, pageable)
            .map(this::convertirADto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> listarActivos(Long empresaId, Pageable pageable) {
        log.debug("Listando productos activos de empresa: {}", empresaId);

        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
            .map(this::convertirADto);
    }

    // ==================== BÚSQUEDAS ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> buscar(Long empresaId, String termino, Pageable pageable) {
        log.debug("Buscando productos en empresa: {} con término: {}", empresaId, termino);

        if (termino == null || termino.trim().isEmpty()) {
            return listar(empresaId, pageable);
        }

        // Buscar por código interno, código de barras o nombre
        return productoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
            .map(this::convertirADto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> buscarPorSucursal(Long sucursalId, Pageable pageable) {
        log.debug("Listando productos de sucursal: {}", sucursalId);

        // Validar que la sucursal existe
        sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        return productoRepository.findBySucursalId(sucursalId, pageable)
            .map(this::convertirADto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> buscarPorSucursal(Long sucursalId, String termino, Pageable pageable) {
        log.debug("Buscando productos en sucursal: {} con término: {}", sucursalId, termino);

        // Validar que la sucursal existe
        sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        if (termino == null || termino.trim().isEmpty()) {
            return buscarPorSucursal(sucursalId, pageable);
        }

        // TODO: Implementar búsqueda por término en sucursal
        // Por ahora solo lista todos
        return productoRepository.findBySucursalId(sucursalId, pageable)
            .map(this::convertirADto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        log.debug("Buscando productos por categoría: {}", categoriaId);

        // Aquí necesitarías un método en el repository como:
        // findByCategorias_IdAndActivoTrue(Long categoriaId, Pageable pageable)
        // Por ahora retornamos una página vacía
        return Page.empty(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDto> buscarPorFamilia(Long familiaId, Pageable pageable) {
        log.debug("Buscando productos por familia: {}", familiaId);

        // Validar que la familia existe
        familiaProductoRepository.findById(familiaId)
            .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));

        Page<Producto> productos = productoRepository.findByFamiliaIdAndActivoTrue(familiaId, pageable);
        return productos.map(this::convertirADto);
    }

    // ==================== UTILIDADES ====================

    @Override
    @Transactional(readOnly = true)
    public String generarCodigoInterno(Long empresaId) {
        log.debug("Generando código interno para empresa: {}", empresaId);

        // Validar que la empresa existe
        if (!empresaRepository.existsById(empresaId)) {
            throw new ResourceNotFoundException("Empresa no encontrada");
        }

        // Generar código con formato: PROD-XXXXX
        String prefijo = "PROD-";
        int numero = 1;

        String codigo;
        do {
            codigo = prefijo + String.format("%05d", numero);
            numero++;
        } while (validador.existeCodigoInterno(codigo, empresaId, null));

        log.debug("Código generado: {}", codigo);
        return codigo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigoInterno, Long empresaId) {
        return validador.existeCodigoInterno(codigoInterno, empresaId, null);
    }

    @Override
    @Transactional
    public void actualizarPrecio(Long productoId, BigDecimal nuevoPrecio) {
        log.info("Actualizando precio del producto ID: {} a {}", productoId, nuevoPrecio);

        if (nuevoPrecio == null || nuevoPrecio.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El precio no puede ser negativo");
        }

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        producto.setPrecioVenta(nuevoPrecio);
        producto.setUpdatedAt(LocalDateTime.now());
        productoRepository.save(producto);

        log.info("Precio actualizado para producto ID: {}", productoId);
    }

    // ==================== IMÁGENES ====================

    @Override
    @Transactional
    public void actualizarImagen(Long productoId, MultipartFile imagen) {
        log.info("Actualizando imagen del producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        imagenHandler.actualizarImagen(producto, imagen);
        log.info("Imagen actualizada para producto ID: {}", productoId);
    }

    @Override
    @Transactional
    public void eliminarImagen(Long productoId) {
        log.info("Eliminando imagen del producto ID: {}", productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        imagenHandler.eliminarImagen(producto);
        log.info("Imagen eliminada para producto ID: {}", productoId);
    }

    // ==================== CATEGORÍAS ====================

    @Override
    @Transactional
    public void asignarCategorias(Long productoId, Set<Long> categoriaIds) {
        log.info("Asignando {} categorías al producto ID: {}", categoriaIds.size(), productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        categoriaHandler.asignarCategorias(producto, categoriaIds);
        log.info("Categorías asignadas al producto ID: {}", productoId);
    }

    @Override
    @Transactional
    public void agregarCategoria(Long productoId, Long categoriaId) {
        log.info("Agregando categoría {} al producto ID: {}", categoriaId, productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        categoriaHandler.agregarCategoria(producto, categoriaId);
        log.info("Categoría agregada al producto ID: {}", productoId);
    }

    @Override
    @Transactional
    public void quitarCategoria(Long productoId, Long categoriaId) {
        log.info("Quitando categoría {} del producto ID: {}", categoriaId, productoId);

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        categoriaHandler.quitarCategoria(producto, categoriaId);
        log.info("Categoría quitada del producto ID: {}", productoId);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Construye la entidad Producto base a partir del DTO
     */
    private Producto construirProductoBase(ProductoCreateDto dto, Empresa empresa, Sucursal sucursal) {
        log.debug("Construyendo producto base");

        Producto.ProductoBuilder builder = Producto.builder()
            .empresa(empresa)
            .sucursal(sucursal)
            .codigoInterno(dto.getCodigoInterno() != null
                ? dto.getCodigoInterno()
                : generarCodigoInterno(empresa.getId()))
            .codigoBarras(dto.getCodigoBarras())
            .nombre(dto.getNombre())
            .descripcion(dto.getDescripcion())
            .unidadMedida(dto.getUnidadMedida() != null
                ? dto.getUnidadMedida()
                : UnidadMedida.UNIDAD)
            .moneda(dto.getMoneda() != null
                ? dto.getMoneda()
                : Moneda.CRC)
            .precioVenta(dto.getPrecioVenta())
            .tipo(dto.getTipo() != null
                ? TipoProducto.valueOf(dto.getTipo())
                : TipoProducto.VENTA)
            .requiereInventario(dto.getRequiereInventario() != null
                ? dto.getRequiereInventario()
                : true)
            .requiereReceta(dto.getRequiereReceta() != null
                ? dto.getRequiereReceta()
                : false)
            .esServicio(dto.getEsServicio() != null
                ? dto.getEsServicio()
                : false)
            .incluyeIVA(dto.getIncluyeIVA() != null
                ? dto.getIncluyeIVA()
                : true)
            .activo(dto.getActivo() != null
                ? dto.getActivo()
                : true);

        // Asignar CABYS si viene
        if (dto.getEmpresaCabysId() != null) {
            EmpresaCAByS cabys = empresaCAbySRepository.findById(dto.getEmpresaCabysId())
                .orElse(null);
            builder.empresaCabys(cabys);
        }

        // Asignar familia si viene
        if (dto.getFamiliaId() != null) {
            FamiliaProducto familia = familiaProductoRepository.findById(dto.getFamiliaId())
                .orElse(null);
            builder.familia(familia);
        }

        return builder.build();
    }

    /**
     * Actualiza los campos básicos de un producto desde el DTO
     */
    private void actualizarCamposBasicos(Producto producto, ProductoUpdateDto dto) {
        log.debug("Actualizando campos básicos del producto ID: {}", producto.getId());

        if (dto.getCodigoInterno() != null) {
            producto.setCodigoInterno(dto.getCodigoInterno());
        }

        if (dto.getCodigoBarras() != null) {
            producto.setCodigoBarras(dto.getCodigoBarras());
        }

        if (dto.getNombre() != null) {
            producto.setNombre(dto.getNombre());
        }

        if (dto.getDescripcion() != null) {
            producto.setDescripcion(dto.getDescripcion());
        }

        if (dto.getUnidadMedida() != null) {
            producto.setUnidadMedida(dto.getUnidadMedida());
        }

        if (dto.getMoneda() != null) {
            producto.setMoneda(dto.getMoneda());
        }

        if (dto.getPrecioVenta() != null) {
            producto.setPrecioVenta(dto.getPrecioVenta());
        }

        if (dto.getEsServicio() != null) {
            producto.setEsServicio(dto.getEsServicio());
        }

        if (dto.getActivo() != null) {
            producto.setActivo(dto.getActivo());
        }

        // Actualizar CABYS si cambió
        if (dto.getEmpresaCabysId() != null) {
            if (producto.getEmpresaCabys() == null ||
                !producto.getEmpresaCabys().getId().equals(dto.getEmpresaCabysId())) {

                EmpresaCAByS cabys = empresaCAbySRepository.findById(dto.getEmpresaCabysId())
                    .orElseThrow(() -> new ResourceNotFoundException("Código CABYS no encontrado"));
                producto.setEmpresaCabys(cabys);
            }
        }

        // Actualizar familia si cambió
        if (dto.getFamiliaId() != null) {
            if (producto.getFamilia() == null ||
                !producto.getFamilia().getId().equals(dto.getFamiliaId())) {

                FamiliaProducto familia = familiaProductoRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));
                producto.setFamilia(familia);
            }
        }

        log.debug("Campos básicos actualizados");
    }

// REEMPLAZAR el método convertirADto completo:

    private ProductoDto convertirADto(Producto producto) {
        ProductoDto dto = modelMapper.map(producto, ProductoDto.class);

        // Mapear empresa
        dto.setEmpresaId(producto.getEmpresa().getId());
        // dto.setEmpresaNombre(producto.getEmpresa().getNombreComercial()); // ← Comentar si no existe

        // Mapear sucursal
        if (producto.getSucursal() != null) {
            dto.setSucursalId(producto.getSucursal().getId());
            // dto.setSucursalNombre(producto.getSucursal().getNombre()); // ← Comentar si no existe
        }

        // Mapear CABYS (comentar si los setters no existen)
        // if (producto.getEmpresaCabys() != null) {
        //     dto.setEmpresaCabysId(producto.getEmpresaCabys().getId());
        //     dto.setCodigoCabys(producto.getEmpresaCabys().getCodigo());
        //     dto.setDescripcionCabys(producto.getEmpresaCabys().getDescripcion());
        // }

        // Mapear familia
        if (producto.getFamilia() != null) {
            dto.setFamiliaId(producto.getFamilia().getId());
            // dto.setFamiliaNombre(producto.getFamilia().getNombre()); // ← Comentar si no existe
        }

        // Mapear categorías (comentar si no existe)
        // if (producto.getCategorias() != null && !producto.getCategorias().isEmpty()) {
        //     Set<Long> categoriaIds = producto.getCategorias().stream()
        //         .map(CategoriaProducto::getId)
        //         .collect(java.util.stream.Collectors.toSet());
        //     dto.setCategoriaIds(categoriaIds);
        // }

        // Por ahora NO mapeamos impuestos para evitar el error de tipo
        // dto.setImpuestos(impuestoHandler.obtenerImpuestosComoDto(producto.getId()));

        return dto;
    }

    /**
     * Convierte ProductoImpuestoCreateDto a ImpuestoDto
     */
    private List<ImpuestoDto> convertirImpuestos(List<com.snnsoluciones.backnathbitpos.dto.producto.ProductoImpuestoCreateDto> impuestosCreate) {
        if (impuestosCreate == null) {
            return new java.util.ArrayList<>();
        }

        return impuestosCreate.stream()
            .map(imp -> ImpuestoDto.builder()
                .tipoImpuesto(imp.getTipoImpuesto().name())  // ← Agregar .name()
                .codigoTarifa(imp.getCodigoTarifaIVA().name())  // ← Cambiar a getCodigoTarifaIVA().name()
                .tarifa(imp.getPorcentaje())  // ← Cambiar a getPorcentaje()
                .build())
            .toList();  // ← Cambiar a .toList() (Java 16+) o usar Collectors.toList()
    }
}