package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.CrearVentaPausadaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.VentaPausadaDetalleDTO;
import com.snnsoluciones.backnathbitpos.dto.factura.VentaPausadaListDTO;
import com.snnsoluciones.backnathbitpos.service.impl.VentaPausadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/ventas-pausadas")
@RequiredArgsConstructor
@Tag(name = "Ventas Pausadas", description = "Gestión de ventas pausadas/borradores")
@PreAuthorize("hasAnyRole('CAJERO', 'MESERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
public class VentaPausadaController {
    
    private final VentaPausadaService service;

    @PostMapping
    @Operation(summary = "Pausar una venta", description = "Guarda el estado actual de una venta para retomarla después")
    public ResponseEntity<ApiResponse<VentaPausadaListDTO>> pausarVenta(
        @Valid @RequestBody CrearVentaPausadaRequest request,
        @RequestParam Long sucursalId,
        @RequestParam(required = false) Long terminalId) {

        VentaPausadaListDTO venta = service.crear(request, sucursalId, terminalId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.<VentaPausadaListDTO>builder()
                .success(true)
                .message("Venta pausada exitosamente")
                .data(venta)
                .build()
        );
    }

    @GetMapping
    @Operation(summary = "Listar ventas pausadas", description = "Lista las ventas pausadas activas del usuario actual")
    public ResponseEntity<ApiResponse<List<VentaPausadaListDTO>>> listarVentasPausadas(
        @RequestParam Long sucursalId) {

        List<VentaPausadaListDTO> ventas = service.listarActivas(sucursalId);

        return ResponseEntity.ok(
            ApiResponse.<List<VentaPausadaListDTO>>builder()
                .success(true)
                .message("Ventas pausadas obtenidas")
                .data(ventas)
                .build()
        );
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas las ventas de la sucursal", description = "Solo para supervisores")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<VentaPausadaListDTO>>> listarTodasSucursal(
        @RequestParam Long sucursalId) {

        List<VentaPausadaListDTO> ventas = service.listarTodasSucursal(sucursalId);

        return ResponseEntity.ok(
            ApiResponse.<List<VentaPausadaListDTO>>builder()
                .success(true)
                .message("Todas las ventas pausadas de la sucursal")
                .data(ventas)
                .build()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de venta pausada", description = "Obtiene los datos completos para retomar la venta")
    public ResponseEntity<ApiResponse<VentaPausadaDetalleDTO>> obtenerDetalle(
        @PathVariable Long id,
        @RequestParam Long sucursalId) {

        VentaPausadaDetalleDTO venta = service.obtenerDetalle(id, sucursalId);

        return ResponseEntity.ok(
            ApiResponse.<VentaPausadaDetalleDTO>builder()
                .success(true)
                .message("Detalle de venta pausada")
                .data(venta)
                .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar venta pausada", description = "Elimina permanentemente una venta pausada")
    public ResponseEntity<ApiResponse<Void>> eliminarVenta(
        @PathVariable Long id,
        @RequestParam Long sucursalId) {

        service.eliminar(id, sucursalId);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Venta pausada eliminada")
                .build()
        );
    }

    @GetMapping("/contador")
    @Operation(summary = "Contar ventas pausadas activas", description = "Obtiene el número de ventas pausadas para el badge")
    public ResponseEntity<ApiResponse<Long>> contarVentasPausadas(
        @RequestParam Long sucursalId) {

        long count = service.contarActivas(sucursalId);

        return ResponseEntity.ok(
            ApiResponse.<Long>builder()
                .success(true)
                .message("Contador de ventas pausadas")
                .data(count)
                .build()
        );
    }
}