// ZonaMesaController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.service.mesas.ZonaLayoutService;
import com.snnsoluciones.backnathbitpos.service.mesas.ZonaMesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas/{empresaId}/sucursales/{sucursalId}/zonas")
@RequiredArgsConstructor
@Slf4j
public class ZonaMesaController {

  private final ZonaMesaService zonaService;
  private final ZonaLayoutService zonaLayoutService;

  @PostMapping
  public ZonaResponse crear(@PathVariable Long empresaId,
                            @PathVariable Long sucursalId,
                            @Valid @RequestBody CrearZonaRequest req) {
    return zonaService.crear(empresaId, sucursalId, req);
  }

  @GetMapping
  public List<ZonaResponse> listar(@PathVariable Long empresaId,
                                   @PathVariable Long sucursalId) {
    // empresaId se valida en el service si quieres reforzar
    return zonaService.listar(sucursalId);
  }

  @PutMapping("/{zonaId}")
  public ZonaResponse actualizar(@PathVariable Long empresaId,
                                 @PathVariable Long sucursalId,
                                 @PathVariable Long zonaId,
                                 @Valid @RequestBody ActualizarZonaRequest req) {
    return zonaService.actualizar(zonaId, req);
  }

  @PostMapping("/{zonaId}/layout")
  @Operation(summary = "Guardar layout de mesas de una zona")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Layout guardado correctamente"),
      @ApiResponse(responseCode = "404", description = "Zona no encontrada"),
      @ApiResponse(responseCode = "400", description = "Datos inválidos")
  })
  public ResponseEntity<Void> guardarLayout(
      @PathVariable Long zonaId,
      @Valid @RequestBody GuardarLayoutRequest request) {

    log.info("Guardando layout para zona {} con {} mesas", zonaId, request.getMesas().size());
    zonaLayoutService.guardarLayout(zonaId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{zonaId}/layout")
  @Operation(summary = "Obtener layout de mesas de una zona")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Layout obtenido correctamente"),
      @ApiResponse(responseCode = "404", description = "Zona no encontrada")
  })
  public ResponseEntity<List<MesaLayoutDTO>> obtenerLayout(@PathVariable Long zonaId) {
    List<MesaLayoutDTO> layout = zonaLayoutService.obtenerLayout(zonaId);
    return ResponseEntity.ok(layout);
  }

  @DeleteMapping("/{zonaId}/layout")
  @Operation(summary = "Eliminar layout de una zona (resetear a default)")
  public ResponseEntity<Void> eliminarLayout(@PathVariable Long zonaId) {
    zonaLayoutService.eliminarLayout(zonaId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{zonaId}/layout/exists")
  @Operation(summary = "Verificar si existe layout personalizado para una zona")
  public ResponseEntity<Boolean> existeLayout(@PathVariable Long zonaId) {
    boolean existe = zonaLayoutService.existeLayout(zonaId);
    return ResponseEntity.ok(existe);
  }
}