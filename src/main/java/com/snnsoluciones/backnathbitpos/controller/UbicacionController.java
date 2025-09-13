package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicacion")
@RequiredArgsConstructor
@Tag(name = "Ubicación", description = "Catálogo de ubicaciones de Costa Rica")
public class UbicacionController {

  private final UbicacionService ubicacionService;

  @Operation(summary = "Listar todas las provincias")
  @GetMapping("/provincias")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<List<Provincia>>> listarProvincias() {
    List<Provincia> provincias = ubicacionService.listarProvincias();
    return ResponseEntity.ok(ApiResponse.ok(provincias));
  }

  @Operation(summary = "Listar cantones por provincia")
  @GetMapping("/cantones/{provinciaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<List<Canton>>> listarCantonesPorProvincia(
      @PathVariable Integer provinciaId) {
    List<Canton> cantones = ubicacionService.listarCantonesPorProvincia(provinciaId);
    return ResponseEntity.ok(ApiResponse.ok(cantones));
  }

  @Operation(summary = "Listar distritos por cantón")
  @GetMapping("/distritos/{cantonId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<List<Distrito>>> listarDistritosPorCanton(
      @PathVariable Integer cantonId) {
    List<Distrito> distritos = ubicacionService.listarDistritosPorCanton(cantonId);
    return ResponseEntity.ok(ApiResponse.ok(distritos));
  }

  @Operation(summary = "Listar barrios por distrito")
  @GetMapping("/barrios/{provinciaId}/{cantonId}/{distritoId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<List<Barrio>>> listarBarriosPorDistrito(
      @PathVariable Integer provinciaId,
      @PathVariable Integer cantonId,
      @PathVariable Integer distritoId) {
    List<Barrio> barrios = ubicacionService
        .listarBarriosPorDistrito(provinciaId, cantonId, distritoId);
    return ResponseEntity.ok(ApiResponse.ok(barrios));
  }
}