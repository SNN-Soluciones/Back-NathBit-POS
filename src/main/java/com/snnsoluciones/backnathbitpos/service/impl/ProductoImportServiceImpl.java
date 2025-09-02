package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoImportServiceImpl implements ProductoImportService {

    private final ProductoCrudService productoCrudService;
    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaCABySRepository empresaCABySRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;

    @Override
    @Transactional
    public ProductoImportResultDto importarProductos(Long empresaId, List<ProductoImportDto> productos) {
        log.info("Iniciando importación de {} productos para empresa {}", productos.size(), empresaId);

        // Verificar que la empresa existe
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        // Obtener categoría por defecto o la primera activa
        CategoriaProducto categoriaDefault = obtenerCategoriaDefault(empresa);

        // Obtener código CABYS por defecto
        EmpresaCAByS cabysDefault = obtenerCabysDefault(empresaId);

        ProductoImportResultDto resultado = ProductoImportResultDto.builder()
            .totalProcesados(productos.size())
            .exitosos(0)
            .errores(0)
            .productosConError(new ArrayList<>())
            .mensajesGenerales(new ArrayList<>())
            .build();

        for (ProductoImportDto productoImport : productos) {
            try {
                // Validar si ya existe por código
                if (productoRepository.existsByCodigoInternoAndEmpresaId(
                    productoImport.getCodigo(), empresaId)) {
                    log.warn("Producto con código {} ya existe, actualizando...",
                        productoImport.getCodigo());

                    // Actualizar producto existente
                    actualizarProductoExistente(empresaId, productoImport, cabysDefault);
                } else {
                    // Crear nuevo producto
                    crearNuevoProducto(empresaId, productoImport, categoriaDefault, cabysDefault);
                }

                productoImport.setImportado(true);
                resultado.setExitosos(resultado.getExitosos() + 1);

            } catch (Exception e) {
                log.error("Error al importar producto {}: {}",
                    productoImport.getCodigo(), e.getMessage());
                productoImport.setImportado(false);
                productoImport.setMensajeError(e.getMessage());
                resultado.setErrores(resultado.getErrores() + 1);
                resultado.getProductosConError().add(productoImport);
            }
        }

        log.info("Importación completada: {} exitosos, {} errores",
            resultado.getExitosos(), resultado.getErrores());

        return resultado;
    }

    @Override
    public List<ProductoImportDto> procesarArchivoExcel(MultipartFile archivo) throws Exception {
        List<ProductoImportDto> productos = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Saltar la fila de encabezados
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Saltar filas vacías
                if (isRowEmpty(row)) continue;

                try {
                    ProductoImportDto producto = mapearFilaAProducto(row);
                    productos.add(producto);
                } catch (Exception e) {
                    log.error("Error procesando fila {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }
        }

        log.info("Se procesaron {} productos del archivo Excel", productos.size());
        return productos;
    }

    @Override
    @Transactional
    public ProductoImportResultDto importarDesdeExcel(Long empresaId, MultipartFile archivo)
        throws Exception {
        List<ProductoImportDto> productos = procesarArchivoExcel(archivo);
        return importarProductos(empresaId, productos);
    }

    private void crearNuevoProducto(Long empresaId, ProductoImportDto importDto,
        CategoriaProducto categoria, EmpresaCAByS cabys) {

        // Determinar si el producto es exento basado en el campo del Excel
        boolean esExento = importDto.getProductoExento() != null && importDto.getProductoExento();

        // Crear DTO para el servicio de creación
        ProductoCreateDto createDto = ProductoCreateDto.builder()
            .codigoInterno(importDto.getCodigo())
            .codigoBarras(importDto.getCodigoBarras())
            .nombre(importDto.getNombreProducto())
            .descripcion("Importado desde Excel")
            .empresaCabysId(cabys.getId())
            .categoriaIds(Arrays.asList(categoria.getId()))
            .unidadMedida(UnidadMedida.UNIDAD) // Por defecto
            .moneda(Moneda.CRC) // Por defecto
            .precioVenta(importDto.getPrecio())
            .aplicaServicio(false) // Por defecto
            .activo("A".equals(importDto.getEstadoProducto()))
            .impuestos(crearImpuestosDefault(esExento))
            .build();

        productoCrudService.crear(empresaId, createDto, null);
    }

    private void actualizarProductoExistente(Long empresaId, ProductoImportDto importDto,
        EmpresaCAByS cabys) {

        // Buscar el producto existente
        Producto productoExistente = productoRepository
            .findByCodigoInternoAndEmpresaId(importDto.getCodigo(), empresaId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado para actualizar"));

        // Crear DTO de actualización
        ProductoUpdateDto updateDto = ProductoUpdateDto.builder()
            .codigoInterno(importDto.getCodigo())
            .codigoBarras(importDto.getCodigoBarras())
            .nombre(importDto.getNombreProducto())
            .descripcion(productoExistente.getDescripcion()) // Mantener descripción
            .empresaCabysId(cabys.getId())
            .categoriaId(productoExistente.getCategorias().stream()
                .findFirst()
                .map(CategoriaProducto::getId)
                .orElse(null))
            .unidadMedida(productoExistente.getUnidadMedida())
            .moneda(productoExistente.getMoneda())
            .precioVenta(importDto.getPrecio())
            .aplicaServicio(productoExistente.getAplicaServicio())
            .activo("A".equals(importDto.getEstadoProducto()))
            .build();

        productoCrudService.actualizar(empresaId, productoExistente.getId(), updateDto, null);
    }

    private List<ProductoImpuestoCreateDto> crearImpuestosDefault(boolean esExento) {
        List<ProductoImpuestoCreateDto> impuestos = new ArrayList<>();

        // Crear impuesto IVA basado en si es exento o no
        ProductoImpuestoCreateDto impuestoIVA = ProductoImpuestoCreateDto.builder()
            .tipoImpuesto(TipoImpuesto.IVA)
            .codigoTarifaIVA(esExento ? CodigoTarifaIVA.TARIFA_EXENTA : CodigoTarifaIVA.TARIFA_GENERAL_13)
            .build();

        impuestos.add(impuestoIVA);

        // Log para debugging
        log.debug("Producto {} - Exento: {}, Tarifa IVA: {}",
            esExento ? "EXENTO" : "GRAVADO",
            esExento,
            impuestoIVA.getCodigoTarifaIVA());

        return impuestos;
    }

    private CategoriaProducto obtenerCategoriaDefault(Empresa empresa) {
        // Opción 1: Buscar categoría "GENERAL" o "PRODUCTOS"
        List<String> nombresDefault = Arrays.asList("GENERAL", "PRODUCTOS", "VARIOS");

        for (String nombre : nombresDefault) {
            Optional<CategoriaProducto> categoria = categoriaProductoRepository
                .findByNombreAndEmpresaId(nombre, empresa.getId());
            if (categoria.isPresent() && categoria.get().getActivo()) {
                log.info("Usando categoría existente '{}' para importación", nombre);
                return categoria.get();
            }
        }

        // Opción 2: Usar la primera categoría activa
        List<CategoriaProducto> categoriasActivas = categoriaProductoRepository
            .findByEmpresaIdAndActivoTrueOrderByOrdenAsc(empresa.getId());

        if (!categoriasActivas.isEmpty()) {
            CategoriaProducto primera = categoriasActivas.get(0);
            log.info("Usando primera categoría activa '{}' para importación", primera.getNombre());
            return primera;
        }

        // Opción 3: Crear categoría GENERAL si no hay ninguna
        log.info("No se encontraron categorías, creando categoría GENERAL");
        CategoriaProducto nuevaCategoria = CategoriaProducto.builder()
            .empresa(empresa)
            .nombre("GENERAL")
            .descripcion("Categoría general para productos")
            .color("#6B7280") // Gris
            .icono("fa-box")
            .orden(1)
            .activo(true)
            .build();

        return categoriaProductoRepository.save(nuevaCategoria);
    }

    private EmpresaCAByS obtenerCabysDefault(Long empresaId) {
        // Buscar código CABYS genérico (0000000000000)
        return empresaCABySRepository
            .findByCodigoCabysCodigo("0000000000000")
            .stream()
            .filter(ec -> ec.getEmpresa().getId().equals(empresaId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "No se encontró código CABYS por defecto. Configure el código 0000000000000"));
    }

    private ProductoImportDto mapearFilaAProducto(Row row) {
        ProductoImportDto dto = ProductoImportDto.builder()
            .id(getCellValueAsLong(row.getCell(0)))
            .codigo(getCellValueAsString(row.getCell(1)))
            .nombreProducto(getCellValueAsString(row.getCell(2)))
            .precio(getCellValueAsBigDecimal(row.getCell(3)))
            .codigoBarras(getCellValueAsString(row.getCell(20)))
            .precioCompra(getCellValueAsBigDecimal(row.getCell(11)))
            .existenciaMinima(getCellValueAsInteger(row.getCell(13)))
            .productoExento(getCellValueAsBoolean(row.getCell(9)))
            .afectaInventario(getCellValueAsBoolean(row.getCell(10)))
            .estadoProducto(getCellValueAsString(row.getCell(18)))
            .build();

        // Log para debugging
        log.debug("Producto mapeado: {} - Exento: {}",
            dto.getCodigo(),
            dto.getProductoExento());

        return dto;
    }

    // Métodos auxiliares para leer celdas de Excel
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    return new BigDecimal(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }

    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        Long value = getCellValueAsLong(cell);
        return value != null ? value.intValue() : null;
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return false;

        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                return cell.getNumericCellValue() == 1;
            case STRING:
                String value = cell.getStringCellValue().trim();
                return "1".equals(value) || "S".equalsIgnoreCase(value) ||
                    "SI".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
            default:
                return false;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}

// 5. Controller para importación
// ProductoImportController.java
