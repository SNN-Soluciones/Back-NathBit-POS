// ZonaMesaService.java
package com.snnsoluciones.backnathbitpos.service.mesas;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.ZonaMesa;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.ZonaMesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class ZonaMesaService {
  private final ZonaMesaRepository zonaRepo;
  private final SucursalRepository sucursalRepo;

  @Transactional
  public ZonaResponse crear(Long empresaId, Long sucursalId, CrearZonaRequest req) {
    Sucursal sucursal = sucursalRepo.findById(sucursalId)
        .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));
    // (opcional) valida que sucursal pertenezca a empresaId
    ZonaMesa z = ZonaMesa.builder()
        .sucursal(sucursal).nombre(req.nombre().trim())
        .descripcion(req.descripcion()).orden(req.orden() == null ? 0 : req.orden())
        .activo(true).build();
    zonaRepo.save(z);
    return new ZonaResponse(z.getId(), z.getNombre(), z.getDescripcion(), z.getOrden(), z.getActivo());
  }

  @Transactional(readOnly = true)
  public List<ZonaResponse> listar(Long sucursalId) {
    return zonaRepo.findBySucursalIdAndActivoTrueOrderByOrdenAsc(sucursalId).stream()
        .map(z -> new ZonaResponse(z.getId(), z.getNombre(), z.getDescripcion(), z.getOrden(), z.getActivo()))
        .toList();
  }

  @Transactional
  public ZonaResponse actualizar(Long zonaId, ActualizarZonaRequest req) {
    ZonaMesa z = zonaRepo.findById(zonaId)
        .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
    z.setNombre(req.nombre().trim());
    z.setDescripcion(req.descripcion());
    if (req.orden() != null) z.setOrden(req.orden());
    if (req.activo() != null) z.setActivo(req.activo());
    return new ZonaResponse(z.getId(), z.getNombre(), z.getDescripcion(), z.getOrden(), z.getActivo());
  }
}