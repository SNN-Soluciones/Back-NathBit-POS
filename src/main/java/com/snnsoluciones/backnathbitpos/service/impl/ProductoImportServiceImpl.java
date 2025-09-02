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
    private final CodigoCABySRepository codigoCABySRepository;

    @Override
    @Transactional
    public ProductoImportResultDto importarProductos(Long empresaId, List<ProductoImportDto> productos) {
        log.info("Iniciando importación de {} productos para empresa {}", productos.size(), empresaId);

        ProductoImportResultDto resultado = ProductoImportResultDto.builder()
            .totalProcesados(productos.size())
            .exitosos(0)
            .errores(0)
            .productosConError(new ArrayList<>())
            .mensajesGenerales(new ArrayList<>())
            .build();

        // Obtener empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        // Obtener categoría por defecto
        CategoriaProducto categoriaDefault = obtenerCategoriaDefault(empresa);

        // Cache de CABYS para evitar múltiples consultas
        Map<String, EmpresaCAByS> cabysCache = new HashMap<>();

        for (ProductoImportDto productoImport : productos) {
            try {
                // Manejar el CABYS
                EmpresaCAByS cabysAsignado = null;

                if (productoImport.getCabysId() != null && productoImport.getCabysId() > 0) {
                    String cabysKey = productoImport.getCabysId().toString();

                    // Buscar en cache primero
                    if (!cabysCache.containsKey(cabysKey)) {
                        cabysAsignado = obtenerOAsignarCAByS(empresaId, productoImport.getCabysId());
                        if (cabysAsignado != null) {
                            cabysCache.put(cabysKey, cabysAsignado);
                        }
                    } else {
                        cabysAsignado = cabysCache.get(cabysKey);
                    }
                }

                // Si no hay CABYS específico, usar uno por defecto
                if (cabysAsignado == null) {
                    cabysAsignado = obtenerCABySDefault(empresaId);
                }

                // Verificar si el producto ya existe
                boolean existe = productoRepository
                    .existsByCodigoInternoAndEmpresaId(productoImport.getCodigo(), empresaId);

                if (existe) {
                    // Actualizar producto existente
                    actualizarProductoExistente(empresaId, productoImport, cabysAsignado);
                } else {
                    // Crear nuevo producto
                    crearNuevoProducto(empresaId, productoImport, categoriaDefault, cabysAsignado);
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

    private ProductoImportDto mapearFilaAProducto(Row row) {
        ProductoImportDto dto = ProductoImportDto.builder()
            .id(getCellValueAsLong(row.getCell(0)))
            .codigo(getCellValueAsString(row.getCell(1)))
            .nombreProducto(getCellValueAsString(row.getCell(2)))
            .precio(getCellValueAsBigDecimal(row.getCell(3)))
            .productoExento(getCellValueAsBoolean(row.getCell(12)))
            .afectaInventario(getCellValueAsBoolean(row.getCell(13)))
            .precioCompra(getCellValueAsBigDecimal(row.getCell(14)))
            .existenciaMinima(getCellValueAsInteger(row.getCell(16)))
            .estadoProducto(getCellValueAsString(row.getCell(21)))
            .codigoBarras(getCellValueAsString(row.getCell(23)))
            .cabysId(getCellValueAsLong(row.getCell(29)))
            .build();

        log.debug("Producto mapeado: {} - Exento: {} - CABYS ID: {}",
            dto.getCodigo(),
            dto.getProductoExento(),
            dto.getCabysId());

        return dto;
    }

    private void crearNuevoProducto(Long empresaId, ProductoImportDto importDto,
        CategoriaProducto categoria, EmpresaCAByS cabys) {

        boolean esExento = importDto.getProductoExento() != null && importDto.getProductoExento();

        ProductoCreateDto createDto = ProductoCreateDto.builder()
            .codigoInterno(importDto.getCodigo())
            .codigoBarras(importDto.getCodigoBarras())
            .nombre(importDto.getNombreProducto())
            .descripcion("Importado desde Excel")
            .empresaCabysId(cabys.getId())
            .categoriaIds(Arrays.asList(categoria.getId()))
            .unidadMedida(UnidadMedida.UNIDAD)
            .moneda(Moneda.CRC)
            .precioVenta(importDto.getPrecio())
            .aplicaServicio(false)
            .activo("A".equals(importDto.getEstadoProducto()))
            .impuestos(crearImpuestosDefault(esExento))
            .build();

        productoCrudService.crear(empresaId, createDto, null);
    }

    private void actualizarProductoExistente(Long empresaId, ProductoImportDto importDto,
        EmpresaCAByS cabys) {

        Producto productoExistente = productoRepository
            .findByCodigoInternoAndEmpresaId(importDto.getCodigo(), empresaId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado para actualizar"));

        ProductoUpdateDto updateDto = ProductoUpdateDto.builder()
            .codigoInterno(importDto.getCodigo())
            .codigoBarras(importDto.getCodigoBarras())
            .nombre(importDto.getNombreProducto())
            .descripcion(productoExistente.getDescripcion())
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

    private EmpresaCAByS obtenerOAsignarCAByS(Long empresaId, Long cabysId) {
        try {
            // Buscar si ya está asignado a la empresa
            Optional<EmpresaCAByS> existente = empresaCABySRepository
                .findByEmpresaIdAndCodigoCabysIdAndActivoTrue(empresaId, cabysId);

            if (existente.isPresent()) {
                return existente.get();
            }

            // Si no existe, asignarlo
            CodigoCAByS codigoCabys = codigoCABySRepository.findById(cabysId)
                .orElseThrow(() -> new BusinessException("Código CABYS no encontrado: " + cabysId));

            Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

            EmpresaCAByS nuevo = EmpresaCAByS.builder()
                .empresa(empresa)
                .codigoCabys(codigoCabys)
                .activo(true)
                .build();

            EmpresaCAByS guardado = empresaCABySRepository.save(nuevo);
            log.info("CABYS {} asignado automáticamente a empresa {}",
                codigoCabys.getCodigo(), empresaId);

            return guardado;

        } catch (Exception e) {
            log.error("Error al asignar CABYS {}: {}", cabysId, e.getMessage());
            return null;
        }
    }

    private EmpresaCAByS obtenerCABySDefault(Long empresaId) {
        // Buscar el CABYS genérico "0000000000000"
        Optional<EmpresaCAByS> cabysGenerico = empresaCABySRepository
            .findByEmpresaIdAndCodigoCabysCodigoAndActivoTrue(empresaId, "6332000000000");

        if (cabysGenerico.isPresent()) {
            return cabysGenerico.get();
        }

        // Si no existe, crearlo
        Optional<CodigoCAByS> codigoGenerico = codigoCABySRepository
            .findByCodigo("0000000000000");

        if (codigoGenerico.isEmpty()) {
            throw new BusinessException("No se encontró código CABYS genérico. Configure el código 6332000000000");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        EmpresaCAByS nuevo = EmpresaCAByS.builder()
            .empresa(empresa)
            .codigoCabys(codigoGenerico.get())
            .activo(true)
            .build();

        return empresaCABySRepository.save(nuevo);
    }

    private CategoriaProducto obtenerCategoriaDefault(Empresa empresa) {
        // Buscar categoría "GENERAL" o "PRODUCTOS"
        List<String> nombresDefault = Arrays.asList("GENERAL", "PRODUCTOS", "VARIOS");

        for (String nombre : nombresDefault) {
            Optional<CategoriaProducto> categoria = categoriaProductoRepository
                .findByNombreAndEmpresaId(nombre, empresa.getId());
            if (categoria.isPresent() && categoria.get().getActivo()) {
                log.info("Usando categoría existente '{}' para importación", nombre);
                return categoria.get();
            }
        }

        // Usar la primera categoría activa
        List<CategoriaProducto> categoriasActivas = categoriaProductoRepository
            .findByEmpresaIdAndActivoTrueOrderByOrdenAsc(empresa.getId());

        if (!categoriasActivas.isEmpty()) {
            CategoriaProducto primera = categoriasActivas.get(0);
            log.info("Usando primera categoría activa '{}' para importación", primera.getNombre());
            return primera;
        }

        // Crear categoría GENERAL si no hay ninguna
        log.info("No se encontraron categorías, creando categoría GENERAL");
        CategoriaProducto nuevaCategoria = CategoriaProducto.builder()
            .empresa(empresa)
            .nombre("GENERAL")
            .descripcion("Categoría general para productos")
            .color("#6B7280")
            .icono("fa-box")
            .orden(1)
            .activo(true)
            .build();

        return categoriaProductoRepository.save(nuevaCategoria);
    }

    private List<ProductoImpuestoCreateDto> crearImpuestosDefault(boolean esExento) {
        List<ProductoImpuestoCreateDto> impuestos = new ArrayList<>();

        ProductoImpuestoCreateDto impuestoIVA = ProductoImpuestoCreateDto.builder()
            .tipoImpuesto(TipoImpuesto.IVA)
            .codigoTarifaIVA(esExento ? CodigoTarifaIVA.TARIFA_EXENTA : CodigoTarifaIVA.TARIFA_GENERAL_13)
            .build();

        impuestos.add(impuestoIVA);

        log.debug("Producto {} - Tarifa IVA: {}",
            esExento ? "EXENTO" : "GRAVADO",
            impuestoIVA.getCodigoTarifaIVA());

        return impuestos;
    }

    // Métodos auxiliares para leer celdas
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

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue() == 1;
            case STRING -> {
                String value = cell.getStringCellValue().trim().toLowerCase();
                yield "1".equals(value) || "true".equals(value) || "si".equals(value);
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> false;
        };
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