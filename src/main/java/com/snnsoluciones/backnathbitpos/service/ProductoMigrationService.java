package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.*;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoMigrationService {

    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaCABySRepository empresaCAbySRepository;
    private final ProductoImpuestoRepository impuestoRepository;

    @Transactional
    public Map<String, Object> migrarProductosDesdeExcel(
            MultipartFile archivo, 
            Long empresaId, 
            Long sucursalId,
            Long empresaCabysId) {
        
        log.info("=== INICIANDO MIGRACIÓN DE PRODUCTOS DESDE EXCEL ===");
        
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<Map<String, String>> productosCreados = new ArrayList<>();
        List<Map<String, String>> productosDuplicados = new ArrayList<>();
        List<Map<String, String>> errores = new ArrayList<>();
        
        try {
            // Validar archivo
            if (archivo.isEmpty()) {
                throw new BusinessException("El archivo está vacío");
            }
            
            String nombreArchivo = archivo.getOriginalFilename();
            if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".xlsx")) {
                throw new BusinessException("El archivo debe ser un Excel (.xlsx)");
            }
            
            // Validar entidades
            Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa ID " + empresaId + " no encontrada"));
            
            Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new BusinessException("Sucursal ID " + sucursalId + " no encontrada"));
            
            if (!sucursal.getEmpresa().getId().equals(empresaId)) {
                throw new BusinessException("La sucursal no pertenece a la empresa");
            }
            
            EmpresaCAByS empresaCabys = empresaCAbySRepository.findById(empresaCabysId)
                .orElseThrow(() -> new BusinessException("EmpresaCAByS ID " + empresaCabysId + " no encontrada"));
            
            if (!empresaCabys.getEmpresa().getId().equals(empresaId)) {
                throw new BusinessException("EmpresaCAByS no pertenece a la empresa");
            }
            
            log.info("Procesando archivo: {} para Empresa: {} - Sucursal: {}", 
                nombreArchivo, empresa.getNombreComercial(), sucursal.getNombre());
            
            // Cargar categorías existentes
            Map<String, CategoriaProducto> categoriasMap = cargarCategoriasExistentes(empresaId, sucursalId);
            
            // Procesar Excel
            List<ProductoExcel> productosExcel = leerProductosDelExcel(archivo.getInputStream());
            log.info("Productos encontrados en Excel: {}", productosExcel.size());
            
            // Procesar cada producto
            for (ProductoExcel productoExcel : productosExcel) {
                try {
                    // Verificar si ya existe por código interno
                    boolean existe = productoRepository.existsByCodigoInternoAndEmpresaIdAndSucursalId(
                        productoExcel.getCodigo(), empresaId, sucursalId
                    );
                    
                    if (existe) {
                        log.warn("Producto ya existe - Código: {}, Nombre: {}", 
                            productoExcel.getCodigo(), productoExcel.getDescripcion());
                        
                        productosDuplicados.add(Map.of(
                            "codigo", productoExcel.getCodigo(),
                            "nombre", productoExcel.getDescripcion(),
                            "razon", "Ya existe un producto con este código"
                        ));
                        continue;
                    }
                    
                    // Crear producto
                    Producto producto = crearProducto(productoExcel, empresa, sucursal, empresaCabys);
                    
                    // Asignar categoría si existe
                    if (productoExcel.getFamilia() != null && categoriasMap.containsKey(productoExcel.getFamilia())) {
                        CategoriaProducto categoria = categoriasMap.get(productoExcel.getFamilia());
                        producto.getCategorias().add(categoria);
                    }

                    ProductoImpuesto productoImpuesto = ProductoImpuesto.builder()
                        .codigoTarifaIVA(CodigoTarifaIVA.TARIFA_0_EXENTO)
                        .activo(true)
                        .tipoImpuesto(TipoImpuesto.IVA)
                        .porcentaje(BigDecimal.ZERO)
                        .build();

                    // Guardar producto
                    producto = productoRepository.save(producto);
                    
                    // Crear impuesto IVA 0%
                    crearImpuestoExento(producto);
                    
                    log.info("✅ Producto creado: {} - {} (ID: {})", 
                        producto.getCodigoInterno(), producto.getNombre(), producto.getId());
                    
                    productosCreados.add(Map.of(
                        "id", producto.getId().toString(),
                        "codigo", producto.getCodigoInterno(),
                        "nombre", producto.getNombre(),
                        "precio", producto.getPrecioVenta().toString(),
                        "categoria", productoExcel.getFamilia() != null ? productoExcel.getFamilia() : "Sin categoría"
                    ));
                    
                } catch (Exception e) {
                    String error = String.format("Error al procesar producto %s (%s): %s",
                        productoExcel.getCodigo(), productoExcel.getDescripcion(), e.getMessage());
                    log.error(error, e);
                    
                    errores.add(Map.of(
                        "codigo", productoExcel.getCodigo(),
                        "nombre", productoExcel.getDescripcion(),
                        "error", e.getMessage()
                    ));
                }
            }
            
            // Preparar resumen
            resultado.put("estado", "COMPLETADO");
            resultado.put("archivo", nombreArchivo);
            resultado.put("empresaId", empresaId);
            resultado.put("empresaNombre", empresa.getNombreComercial());
            resultado.put("sucursalId", sucursalId);
            resultado.put("sucursalNombre", sucursal.getNombre());
            resultado.put("empresaCabysId", empresaCabysId);
            resultado.put("totalEncontrados", productosExcel.size());
            resultado.put("productosCreados", productosCreados.size());
            resultado.put("productosDuplicados", productosDuplicados.size());
            resultado.put("errores", errores.size());
            resultado.put("detalleCreados", productosCreados);
            resultado.put("detalleDuplicados", productosDuplicados);
            resultado.put("detalleErrores", errores);
            
            log.info("=== MIGRACIÓN COMPLETADA ===");
            log.info("Productos creados: {}", productosCreados.size());
            log.info("Productos duplicados: {}", productosDuplicados.size());
            if (!errores.isEmpty()) {
                log.warn("Errores encontrados: {}", errores.size());
            }
            
        } catch (Exception e) {
            log.error("Error general en migración: ", e);
            resultado.put("estado", "ERROR");
            resultado.put("mensaje", e.getMessage());
            throw new BusinessException("Error en migración: " + e.getMessage());
        }
        
        return resultado;
    }
    
    private Map<String, CategoriaProducto> cargarCategoriasExistentes(Long empresaId, Long sucursalId) {
        Map<String, CategoriaProducto> categoriasMap = new HashMap<>();
        
        List<CategoriaProducto> categorias = categoriaRepository
            .findByEmpresaIdAndSucursalIdAndActivoTrueOrderByOrdenAsc(empresaId, sucursalId);
        
        for (CategoriaProducto categoria : categorias) {
            categoriasMap.put(categoria.getNombre(), categoria);
        }
        
        log.debug("Categorías cargadas: {}", categoriasMap.keySet());
        return categoriasMap;
    }
    
    private List<ProductoExcel> leerProductosDelExcel(InputStream inputStream) throws Exception {
        List<ProductoExcel> productos = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet == null) {
                throw new BusinessException("No se pudo leer la primera hoja del Excel");
            }
            
            // Mapear índices de columnas
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("El archivo no tiene encabezados");
            }
            
            Map<String, Integer> columnasIndices = mapearColumnas(headerRow);
            
            // Leer productos
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                ProductoExcel producto = extraerProductoDeRow(row, columnasIndices);
                
                // Filtrar filas no válidas
                if (producto.getCodigo() != null && 
                    !producto.getCodigo().isEmpty() && 
                    !producto.getDescripcion().equalsIgnoreCase("TOTALES GENERALES:")) {
                    
                    productos.add(producto);
                }
            }
            
        } catch (Exception e) {
            log.error("Error al procesar archivo Excel: ", e);
            throw new BusinessException("Error al leer el archivo Excel: " + e.getMessage());
        }
        
        return productos;
    }
    
    private Map<String, Integer> mapearColumnas(Row headerRow) {
        Map<String, Integer> indices = new HashMap<>();
        
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                String columnName = cell.getStringCellValue().trim();
                int columnIndex = cell.getColumnIndex();
                
                // Mapear nombres de columnas
                switch (columnName) {
                    case "Código":
                        indices.put("codigo", columnIndex);
                        break;
                    case "Descripción":
                        indices.put("descripcion", columnIndex);
                        break;
                    case "Código de barras":
                        indices.put("codigoBarras", columnIndex);
                        break;
                    case "Código CABYS":
                        indices.put("cabys", columnIndex);
                        break;
                    case "Precio de venta":
                        indices.put("precioVenta", columnIndex);
                        break;
                    case "Familia":
                        indices.put("familia", columnIndex);
                        break;
                    case "Unidad":
                        indices.put("unidad", columnIndex);
                        break;
                }
            }
        }
        
        // Validar columnas requeridas
        if (!indices.containsKey("codigo") || !indices.containsKey("descripcion") || 
            !indices.containsKey("precioVenta") || !indices.containsKey("familia")) {
            throw new BusinessException("El archivo no contiene todas las columnas requeridas");
        }
        
        return indices;
    }
    
    private ProductoExcel extraerProductoDeRow(Row row, Map<String, Integer> indices) {
        ProductoExcel producto = new ProductoExcel();
        
        // Código
        Cell codigoCell = row.getCell(indices.get("codigo"));
        if (codigoCell != null) {
            producto.setCodigo(getCellValueAsString(codigoCell));
        }
        
        // Descripción
        Cell descripcionCell = row.getCell(indices.get("descripcion"));
        if (descripcionCell != null) {
            producto.setDescripcion(getCellValueAsString(descripcionCell));
        }
        
        // Código de barras
        if (indices.containsKey("codigoBarras")) {
            Cell codigoBarrasCell = row.getCell(indices.get("codigoBarras"));
            if (codigoBarrasCell != null) {
                producto.setCodigoBarras(getCellValueAsString(codigoBarrasCell));
            }
        }
        
        // CABYS
        if (indices.containsKey("cabys")) {
            Cell cabysCell = row.getCell(indices.get("cabys"));
            if (cabysCell != null) {
                producto.setCabys(getCellValueAsString(cabysCell));
            }
        }
        
        // Precio de venta
        Cell precioCell = row.getCell(indices.get("precioVenta"));
        if (precioCell != null) {
            if (precioCell.getCellType() == CellType.NUMERIC) {
                producto.setPrecioVenta(BigDecimal.valueOf(precioCell.getNumericCellValue()));
            } else {
                String precioStr = getCellValueAsString(precioCell);
                try {
                    producto.setPrecioVenta(new BigDecimal(precioStr));
                } catch (NumberFormatException e) {
                    producto.setPrecioVenta(BigDecimal.ZERO);
                }
            }
        }
        
        // Familia (categoría)
        Cell familiaCell = row.getCell(indices.get("familia"));
        if (familiaCell != null) {
            producto.setFamilia(getCellValueAsString(familiaCell));
        }
        
        return producto;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    
    private Producto crearProducto(ProductoExcel productoExcel, Empresa empresa, 
                                  Sucursal sucursal, EmpresaCAByS empresaCabys) {
        
        // Determinar zona de preparación según categoría
        ZonaPreparacion zona = determinarZonaPreparacion(productoExcel.getFamilia());
        
        return Producto.builder()
            .empresa(empresa)
            .sucursal(sucursal)
            .codigoInterno(productoExcel.getCodigo())
            .nombre(productoExcel.getDescripcion())
            .descripcion(productoExcel.getDescripcion())
            .codigoBarras(productoExcel.getCodigoBarras())
            .precioVenta(productoExcel.getPrecioVenta())
            .precioCompra(BigDecimal.ZERO)
            .moneda(Moneda.CRC)
            .tipo(TipoProducto.VENTA)
            .tipoInventario(TipoInventario.SIMPLE)
            .zonaPreparacion(zona)
            .unidadMedida(UnidadMedida.SERVICIOS_PROFESIONALES)
            .requiereInventario(false)
            .empresaCabys(empresaCabys)
            .activo(true)
            .requiereReceta(false)
            .esServicio(false)
            .categorias(new HashSet<>())
            .build();
    }
    
    private ZonaPreparacion determinarZonaPreparacion(String familia) {
        if (familia == null) return ZonaPreparacion.NINGUNA;
        
        String familiaUpper = familia.toUpperCase();
        
        // Bebidas generalmente no requieren preparación
        if (familiaUpper.contains("BEBIDA") || familiaUpper.contains("CERVEZA") || 
            familiaUpper.contains("VINO") || familiaUpper.contains("LICOR")) {
            return ZonaPreparacion.NINGUNA;
        }
        
        // Todo lo demás va a cocina en una marisquería
        return ZonaPreparacion.COCINA;
    }
    
    private void crearImpuestoExento(Producto producto) {
        ProductoImpuesto impuesto = ProductoImpuesto.builder()
            .producto(producto)
            .tipoImpuesto(TipoImpuesto.IVA)
            .codigoTarifaIVA(CodigoTarifaIVA.TARIFA_EXENTA)
            .porcentaje(BigDecimal.ZERO)
            .activo(true)
            .build();
        
        impuestoRepository.save(impuesto);
    }
    
    // Clase interna para mapear datos del Excel
    @lombok.Data
    private static class ProductoExcel {
        private String codigo;
        private String descripcion;
        private String codigoBarras;
        private String cabys;
        private BigDecimal precioVenta;
        private String familia;
        private String unidad;
    }
}