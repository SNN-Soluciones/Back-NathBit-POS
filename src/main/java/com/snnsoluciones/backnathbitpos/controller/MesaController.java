// MesaController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.service.mesas.MesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zonas/{zonaId}/mesas")
@RequiredArgsConstructor
public class MesaController {

  private final MesaService mesaService;

  @PostMapping
  public MesaResponse crear(@PathVariable Long zonaId, @Valid @RequestBody CrearMesaRequest req) {
    return mesaService.crear(zonaId, req);
  }

  @GetMapping
  public List<MesaResponse> listar(@PathVariable Long zonaId) {
    return mesaService.listar(zonaId);
  }

  @PutMapping("/{mesaId}")
  public MesaResponse actualizar(@PathVariable Long zonaId,
                                 @PathVariable Long mesaId,
                                 @Valid @RequestBody ActualizarMesaRequest req) {
    return mesaService.actualizar(mesaId, req);
  }

  @PostMapping("/{mesaId}/estado")
  public MesaResponse cambiarEstado(@PathVariable Long zonaId,
                                    @PathVariable Long mesaId,
                                    @Valid @RequestBody CambiarEstadoMesaRequest req) {
    return mesaService.cambiarEstado(mesaId, req);
  }

  @PostMapping("/{mesaId}/mover/{nuevaZonaId}")
  public MesaResponse mover(@PathVariable Long zonaId,
                            @PathVariable Long mesaId,
                            @PathVariable Long nuevaZonaId) {
    return mesaService.moverDeZona(mesaId, nuevaZonaId);
  }

  // Unión/Desunión de mesas
  @PostMapping("/unir")
  public List<MesaResponse> unir(@PathVariable Long zonaId,
                                 @RequestBody List<Long> mesaIds,
                                 @RequestParam Long unionGroupId) {
    return mesaService.unirMesas(mesaIds, unionGroupId);
  }

  @PostMapping("/{mesaId}/desunir")
  public MesaResponse desunir(@PathVariable Long zonaId, @PathVariable Long mesaId) {
    return mesaService.desunirMesa(mesaId);
  }
}