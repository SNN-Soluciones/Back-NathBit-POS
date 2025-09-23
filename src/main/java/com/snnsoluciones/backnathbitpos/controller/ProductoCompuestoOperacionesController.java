package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos/compuesto")
@RequiredArgsConstructor
@Tag(name = "Operaciones Compuestos", description = "Operaciones adicionales para productos compuestos")
public class ProductoCompuestoOperacionesController {

    private final ProductoCompuestoService compuestoService;

    @PostMapping("/{productoId}/calcular-precio")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Calcular precio", 
               description = "Calcula el precio total según las opciones seleccionadas")
    public ResponseEntity<ApiResponse<CalculoPrecioResponse>> calcularPrecio(
            @PathVariable Long productoId,
            @RequestParam Long sucursalId, // IMPORTANTE: Para validar disponibilidad
            @Valid @RequestBody CalculoPrecioRequest request) {
        
        try {
            CalculoPrecioResponse calculo = compuestoService.calcularPrecio(
                productoId, sucursalId, request.getOpcionesSeleccionadas()
            );
            return ResponseEntity.ok(ApiResponse.ok("Precio calculado", calculo));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{productoId}/validar-seleccion")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    @Operation(summary = "Validar selección", 
               description = "Valida que la selección cumpla con las reglas del compuesto")
    public ResponseEntity<ApiResponse<ValidacionSeleccionResponse>> validarSeleccion(
            @PathVariable Long productoId,
            @RequestParam Long sucursalId, // Para verificar stock
            @Valid @RequestBody ValidarSeleccionRequest request) {
        
        try {
            ValidacionSeleccionResponse validacion = compuestoService.validarSeleccion(
                productoId, sucursalId, request.getOpcionesSeleccionadas()
            );
            return ResponseEntity.ok(ApiResponse.ok("Validación completa", validacion));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Listar compuestos", 
               description = "Lista todos los productos compuestos de la empresa")
    public ResponseEntity<ApiResponse<List<ProductoCompuestoDto>>> listarPorEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Long sucursalId) {
        
        List<ProductoCompuestoDto> compuestos = compuestoService.listarPorEmpresa(empresaId);
        
        // Si se especifica sucursal, filtrar opciones por disponibilidad
        if (sucursalId != null) {
            compuestos = compuestoService.filtrarPorDisponibilidadSucursal(compuestos, sucursalId);
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Lista de compuestos", compuestos));
    }
}