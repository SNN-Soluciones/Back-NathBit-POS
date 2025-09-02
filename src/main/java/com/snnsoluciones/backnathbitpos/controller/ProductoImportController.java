package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PostMapping(value = "/{empresaId}/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Importar productos desde Excel",
        description = "Importa productos masivamente desde un archivo Excel")
    public ResponseEntity<ApiResponse<ProductoImportResultDto>> importarDesdeExcel(
        @PathVariable Long empresaId,
        @RequestPart("archivo") MultipartFile archivo) {

        log.info("Importando productos desde Excel para empresa: {}", empresaId);

        try {
            // Validar archivo
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductoImportResultDto>builder()
                        .success(false)
                        .message("El archivo está vacío")
                        .build());
            }

            // Validar extensión
            String filename = archivo.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProductoImportResultDto>builder()
                        .success(false)
                        .message("El archivo debe ser Excel (.xlsx o .xls)")
                        .build());
            }

            // Procesar importación
            ProductoImportResultDto resultado = productoImportService
                .importarDesdeExcel(empresaId, archivo);

            return ResponseEntity.ok(ApiResponse.<ProductoImportResultDto>builder()
                .success(true)
                .message(String.format("Importación completada: %d exitosos, %d errores",
                    resultado.getExitosos(), resultado.getErrores()))
                .data(resultado)
                .build());

        } catch (Exception e) {
            log.error("Error al importar productos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProductoImportResultDto>builder()
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
}