// ZonaMesaController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.service.mesas.ZonaMesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas/{empresaId}/sucursales/{sucursalId}/zonas")
@RequiredArgsConstructor
public class ZonaMesaController {

  private final ZonaMesaService zonaService;

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
}