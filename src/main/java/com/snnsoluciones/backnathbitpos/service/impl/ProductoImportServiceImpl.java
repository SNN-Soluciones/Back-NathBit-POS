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
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoImportServiceImpl implements ProductoImportService {

    private static final String CODIGO_CABYS_SERVICIO_RESTAURANTE = "6340001009900";

    private final ProductoRepository productoRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaCABySRepository empresaCABySRepository;
    private final CodigoCABySRepository codigoCABySRepository;
    private final ProductoImpuestoRepository productoImpuestoRepository;

    @Override
    @Transactional
    public ProductoImportResultDto importarProductos(Long empresaId, List<ProductoImportDto> productos) {
        log.info("Iniciando importación de {} productos para empresa {}", productos.size(), empresaId);

        ProductoImportResultDto resultado = ProductoImportResultDto.builder()
            .totalProcesados(productos.size())
            .exitosos(0)
            .errores(0)
            .productosConError(new ArrayList<>())
            .build();

        // Obtener empresa una sola vez
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        // PASO 1: Procesar todos los códigos CABYS y crear lista de EmpresaCAByS
        log.info("PASO 1: Procesando códigos CABYS del Excel");
        List<EmpresaCAByS> listaEmpresaCabys = new ArrayList<>(productos.size());

        for (ProductoImportDto productoImport : productos) {
            try {
                String codigoCabysOriginal = productoImport.getCabysId() != null ?
                    productoImport.getCabysId() : null;

                final String codigoCabys;
                if (codigoCabysOriginal == null || codigoCabysOriginal.trim().isEmpty()) {
                    codigoCabys = CODIGO_CABYS_SERVICIO_RESTAURANTE;
                    log.warn("Producto {} sin CABYS, usando default: {}",
                        productoImport.getCodigo(), codigoCabys);
                } else {
                    codigoCabys = codigoCabysOriginal;
                }

                // Buscar el código CABYS en el catálogo
                CodigoCAByS codigoEntidad = codigoCABySRepository.findByCodigo(codigoCabys)
                    .orElseGet(() -> {
                        log.warn("CABYS {} no encontrado, intentando con servicio restaurante", codigoCabys);
                        return codigoCABySRepository.findByCodigo(CODIGO_CABYS_SERVICIO_RESTAURANTE)
                            .orElseThrow(() -> new BusinessException(
                                "Código CABYS de servicio restaurante no configurado"));
                    });

                // Buscar si ya existe la relación empresa-cabys
                Optional<EmpresaCAByS> empresaCabysExistente = empresaCABySRepository
                    .findByEmpresaIdAndCodigoCabysCodigo(empresa.getId(), codigoEntidad.getCodigo());

                EmpresaCAByS empresaCabys;
                if (empresaCabysExistente.isPresent()) {
                    empresaCabys = empresaCabysExistente.get();
                    log.debug("Usando relación existente para CABYS {}", codigoCabys);
                } else {
                    // Crear nueva relación empresa-cabys
                    empresaCabys = EmpresaCAByS.builder()
                        .empresa(empresa)
                        .codigoCabys(codigoEntidad)
                        .activo(true)
                        .build();

                    empresaCabys = empresaCABySRepository.save(empresaCabys);
                    log.debug("Nueva relación empresa-CABYS creada para código {}", codigoCabys);
                }

                listaEmpresaCabys.add(empresaCabys);

            } catch (Exception e) {
                log.error("Error procesando CABYS para producto {}: {}",
                    productoImport.getCodigo(), e.getMessage());
                listaEmpresaCabys.add(null);
            }
        }

        // PASO 2: Crear productos con sus respectivos EmpresaCAByS
        log.info("PASO 2: Creando productos");

        for (int i = 0; i < productos.size(); i++) {
            ProductoImportDto productoImport = productos.get(i);
            EmpresaCAByS empresaCabysAsignado = listaEmpresaCabys.get(i);

            try {
                if (empresaCabysAsignado == null) {
                    throw new BusinessException("No se pudo procesar el código CABYS");
                }

                // Verificar si el producto ya existe por código
                Optional<Producto> productoExistente = productoRepository
                    .findByCodigoInternoAndEmpresaId(productoImport.getCodigo(), empresaId);

                Producto producto;
                if (productoExistente.isPresent()) {
                    // Actualizar producto existente
                    producto = productoExistente.get();
                    producto.setNombre(productoImport.getNombreProducto());
                    producto.setPrecioVenta(productoImport.getPrecio());
                    producto.setEmpresaCabys(empresaCabysAsignado);
                    producto.setActivo(mapearEstadoActivo(productoImport.getEstadoProducto()));

                    if (productoImport.getCodigoBarras() != null && !productoImport.getCodigoBarras().isEmpty()) {
                        producto.setCodigoBarras(productoImport.getCodigoBarras());
                    }

                    producto = productoRepository.save(producto);
                    log.debug("Producto actualizado: {}", productoImport.getCodigo());

                } else {
                    // AGREGAR ESTA VALIDACIÓN ANTES DE CREAR
                    // Verificar si ya existe un producto con el mismo nombre
                    boolean existePorNombre = productoRepository
                        .existsByNombreAndEmpresaId(productoImport.getNombreProducto(), empresaId);

                    if (existePorNombre) {
                        log.warn("Producto con nombre '{}' ya existe, saltando",
                            productoImport.getNombreProducto());
                        productoImport.setImportado(false);
                        productoImport.setMensajeError("Ya existe un producto con este nombre");
                        resultado.setErrores(resultado.getErrores() + 1);
                        resultado.getProductosConError().add(productoImport);
                        continue; // SALTAR AL SIGUIENTE PRODUCTO
                    }

                    // Crear nuevo producto
                    producto = Producto.builder()
                        .empresa(empresa)
                        .empresaCabys(empresaCabysAsignado)
                        .codigoInterno(productoImport.getCodigo())
                        .codigoBarras(productoImport.getCodigoBarras())
                        .nombre(productoImport.getNombreProducto())
                        .descripcion(null) // Sin descripción por defecto
                        .unidadMedida(UnidadMedida.UNIDAD)
                        .moneda(Moneda.CRC)
                        .precioVenta(productoImport.getPrecio())
                        .aplicaServicio(false)
                        .esServicio(false)
                        .incluyeIVA(true)
                        .activo(mapearEstadoActivo(productoImport.getEstadoProducto()))
                        .build();

                    producto = productoRepository.save(producto);
                    log.debug("Producto creado: {} con ID {}", productoImport.getCodigo(), producto.getId());
                }

                // Crear o actualizar impuesto IVA 13%
                crearImpuestoIVA(producto);

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

    private void crearImpuestoIVA(Producto producto) {
        // Verificar si ya tiene IVA
        Optional<ProductoImpuesto> impuestoExistente = productoImpuestoRepository
            .findByProductoIdAndTipoImpuesto(producto.getId(), TipoImpuesto.IVA);

        if (!impuestoExistente.isPresent()) {
            // Crear impuesto IVA 13%
            ProductoImpuesto impuesto = ProductoImpuesto.builder()
                .producto(producto)
                .tipoImpuesto(TipoImpuesto.IVA)
                .codigoTarifaIVA(CodigoTarifaIVA.TARIFA_GENERAL_13)
                .porcentaje(new BigDecimal("13.00"))
                .activo(true)
                .build();

            productoImpuestoRepository.save(impuesto);
            log.debug("Impuesto IVA 13% creado para producto ID {}", producto.getId());
        }
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
        String codigoCabys = getCellValueAsString(row.getCell(29));
        if (codigoCabys == null || codigoCabys.trim().isEmpty()) {
            codigoCabys = CODIGO_CABYS_SERVICIO_RESTAURANTE;
        }

        // Obtener código y truncar si es muy largo
        String codigo = getCellValueAsString(row.getCell(1));
        if (codigo != null && codigo.length() > 20) {
            log.warn("Truncando código '{}' de {} a 20 caracteres",
                codigo, codigo.length());
            codigo = codigo.substring(0, 20);
        }

        ProductoImportDto dto = ProductoImportDto.builder()
            .codigo(codigo)
            .nombreProducto(getCellValueAsString(row.getCell(2)))
            .precio(getCellValueAsBigDecimal(row.getCell(3)))
            .estadoProducto(getCellValueAsString(row.getCell(21)))
            .codigoBarras(getCellValueAsString(row.getCell(23)))
            .cabysId(codigoCabys) // ✅ Cambiar a String en el DTO
            .productoExento(false)
            .build();

        log.debug("Producto mapeado: {} - Precio: {} - CABYS: {}",
            dto.getCodigo(), dto.getPrecio(), codigoCabys);

        return dto;
    }

    // Métodos auxiliares para leer celdas
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Para códigos CABYS largos
                if (cell.getNumericCellValue() > 999999) {
                    return String.format("%.0f", cell.getNumericCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue())
                    .setScale(2, RoundingMode.HALF_UP);
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private boolean mapearEstadoActivo(String estado) {
        return estado != null && "A".equalsIgnoreCase(estado.trim());
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}