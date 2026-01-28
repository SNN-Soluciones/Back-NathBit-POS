// controller/BarraController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.service.mesas.BarraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zonas/{zonaId}/barras")
@RequiredArgsConstructor
@Tag(name = "Barras", description = "Gestión de barras y sillas")
public class BarraController {

    private final BarraService barraService;

    @PostMapping
    @Operation(summary = "Crear una nueva barra en una zona")
    public BarraResponse crear(
            @PathVariable Long zonaId,
            @Valid @RequestBody CrearBarraRequest req) {
        return barraService.crear(zonaId, req);
    }

    @GetMapping
    @Operation(summary = "Listar todas las barras de una zona")
    public List<BarraResponse> listar(@PathVariable Long zonaId) {
        return barraService.listar(zonaId);
    }

    @GetMapping("/{barraId}")
    @Operation(summary = "Obtener detalles de una barra")
    public BarraResponse obtener(
            @PathVariable Long zonaId,
            @PathVariable Long barraId) {
        return barraService.obtener(barraId);
    }

    @PutMapping("/{barraId}")
    @Operation(summary = "Actualizar una barra")
    public BarraResponse actualizar(
            @PathVariable Long zonaId,
            @PathVariable Long barraId,
            @Valid @RequestBody ActualizarBarraRequest req) {
        return barraService.actualizar(barraId, req);
    }

    @DeleteMapping("/{barraId}")
    @Operation(summary = "Eliminar una barra")
    public void eliminar(
            @PathVariable Long zonaId,
            @PathVariable Long barraId) {
        barraService.eliminar(barraId);
    }

    // ========== GESTIÓN DE SILLAS ==========

    @GetMapping("/{barraId}/sillas")
    @Operation(summary = "Listar sillas de una barra")
    public List<SillaBarraResponse> listarSillas(
            @PathVariable Long zonaId,
            @PathVariable Long barraId) {
        return barraService.listarSillas(barraId);
    }

    @PostMapping("/sillas/{sillaId}/estado")
    @Operation(summary = "Cambiar estado de una silla")
    public SillaBarraResponse cambiarEstadoSilla(
            @PathVariable Long zonaId,
            @PathVariable Long sillaId,
            @Valid @RequestBody CambiarEstadoSillaRequest req) {
        return barraService.cambiarEstadoSilla(sillaId, req);
    }

    @PostMapping("/sillas/{sillaId}/ocupar")
    @Operation(summary = "Ocupar una silla con una orden")
    public SillaBarraResponse ocuparSilla(
            @PathVariable Long zonaId,
            @PathVariable Long sillaId,
            @RequestParam Long ordenId,
            @RequestParam(required = false) Long ordenPersonaId) {
        return barraService.ocuparSilla(sillaId, ordenId, ordenPersonaId);
    }

    @PostMapping("/sillas/{sillaId}/liberar")
    @Operation(summary = "Liberar una silla")
    public SillaBarraResponse liberarSilla(
            @PathVariable Long zonaId,
            @PathVariable Long sillaId) {
        return barraService.liberarSilla(sillaId);
    }
}