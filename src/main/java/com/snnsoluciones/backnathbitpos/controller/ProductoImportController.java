package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos/import")
@RequiredArgsConstructor
@Tag(name = "Importación de Productos", description = "Importación masiva de productos")
public class ProductoImportController {

    private final ProductoImportService productoImportService;

    @PostMapping("/{empresaId}/validar-mapeo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validar mapeo de Excel",
        description = "Muestra cómo se está mapeando cada fila del Excel para debugging")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validarMapeo(
        @PathVariable Long empresaId,
        @RequestPart("archivo") MultipartFile archivo) {

        try {
            List<ProductoImportDto> productos = productoImportService.procesarArchivoExcel(archivo);

            // Tomar los primeros 5 productos para análisis
            List<Map<String, Object>> muestraProductos = new ArrayList<>();
            for (int i = 0; i < Math.min(5, productos.size()); i++) {
                ProductoImportDto prod = productos.get(i);
                Map<String, Object> info = new HashMap<>();
                info.put("codigo", prod.getCodigo());
                info.put("nombre", prod.getNombreProducto());
                info.put("precio", prod.getPrecio());
                info.put("productoExento", prod.getProductoExento());
                info.put("codigoBarras", prod.getCodigoBarras());
                info.put("estadoProducto", prod.getEstadoProducto());
                info.put("impuestoCalculado", prod.getProductoExento() ? "EXENTO" : "IVA 13%");
                muestraProductos.add(info);
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("totalProductos", productos.size());
            resultado.put("muestra", muestraProductos);
            resultado.put("productosExentos", productos.stream()
                .filter(p -> p.getProductoExento() != null && p.getProductoExento())
                .count());
            resultado.put("productosGravados", productos.stream()
                .filter(p -> p.getProductoExento() == null || !p.getProductoExento())
                .count());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Mapeo validado correctamente")
                .data(resultado)
                .build());

        } catch (Exception e) {
            log.error("Error al validar mapeo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error al procesar el archivo: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{empresaId}/preview")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Previsualizar importación",
        description = "Procesa el archivo Excel y muestra preview sin importar")
    public ResponseEntity<ApiResponse<List<ProductoImportDto>>> previsualizarImportacion(
        @PathVariable Long empresaId,
        @RequestPart("archivo") MultipartFile archivo) {

        try {
            List<ProductoImportDto> productos = productoImportService
                .procesarArchivoExcel(archivo);

            return ResponseEntity.ok(ApiResponse.<List<ProductoImportDto>>builder()
                .success(true)
                .message(String.format("Se encontraron %d productos para importar",
                    productos.size()))
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error al procesar archivo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<ProductoImportDto>>builder()
                    .success(false)
                    .message("Error al procesar el archivo: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{empresaId}/json")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Importar desde JSON",
        description = "Importa productos desde una lista JSON")
    public ResponseEntity<ApiResponse<ProductoImportResultDto>> importarDesdeJson(
        @PathVariable Long empresaId,
        @RequestBody @Valid List<ProductoImportDto> productos) {

        log.info("Importando {} productos desde JSON para empresa: {}",
            productos.size(), empresaId);

        ProductoImportResultDto resultado = productoImportService
            .importarProductos(empresaId, productos);

        return ResponseEntity.ok(ApiResponse.<ProductoImportResultDto>builder()
            .success(true)
            .message(String.format("Importación completada: %d exitosos, %d errores",
                resultado.getExitosos(), resultado.getErrores()))
            .data(resultado)
            .build());
    }

    @PostMapping("/{empresaId}/excel")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Importar productos desde Excel",
        description = "Importa productos directamente desde archivo Excel")
    public ResponseEntity<ApiResponse<ProductoImportResultDto>> importarDesdeExcel(
        @PathVariable Long empresaId,
        @RequestPart("archivo") MultipartFile archivo) {

        try {
            log.info("Iniciando importación desde Excel para empresa: {}", empresaId);

            // Validar archivo
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductoImportResultDto>builder()
                        .success(false)
                        .message("El archivo está vacío")
                        .build());
            }

            // Validar tipo de archivo
            String contentType = archivo.getContentType();
            if (contentType == null ||
                (!contentType.equals("application/vnd.ms-excel") &&
                    !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductoImportResultDto>builder()
                        .success(false)
                        .message("El archivo debe ser un Excel (.xlsx o .xls)")
                        .build());
            }

            // Procesar importación
            ProductoImportResultDto resultado = productoImportService
                .importarDesdeExcel(empresaId, archivo);

            log.info("Importación completada: {} exitosos, {} errores",
                resultado.getExitosos(), resultado.getErrores());

            return ResponseEntity.ok(ApiResponse.<ProductoImportResultDto>builder()
                .success(true)
                .message(String.format("Importación completada: %d productos importados exitosamente, %d errores",
                    resultado.getExitosos(), resultado.getErrores()))
                .data(resultado)
                .build());

        } catch (Exception e) {
            log.error("Error al importar desde Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProductoImportResultDto>builder()
                    .success(false)
                    .message("Error al procesar el archivo: " + e.getMessage())
                    .build());
        }
    }
}